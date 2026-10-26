package dev.zedkodex.modehud.server;

import dev.zedkodex.modehud.Config;
import dev.zedkodex.modehud.ModeHud;
import dev.zedkodex.modehud.data.Vitals;
import dev.zedkodex.modehud.net.BandagePayload;
import dev.zedkodex.modehud.net.PortalPayload;
import dev.zedkodex.modehud.net.VitalsPayload;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ModeHud.MODID)
public class ServerEvents {
    public static final ResourceKey<DamageType> BLEED = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(ModeHud.MODID, "bleed"));
    public static final ResourceKey<DamageType> THIRST = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(ModeHud.MODID, "thirst"));

    private static DamageSource source(Level level, ResourceKey<DamageType> key) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key));
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Vitals v = p.getData(ModeHud.VITALS);
        if (p.isAlive() && !p.isCreative() && !p.isSpectator()) {
            thirst(p, v);
            bleeding(p, v);
            regen(p, v);
            bandageHeal(p, v);
        }
        sync(p, v, false, false);
    }

    private static void thirst(ServerPlayer p, Vitals v) {
        if (!Config.COMMON.thirstEnabled.get()) return;

        double rate = 1.0 / (Config.COMMON.waterSecondsPerPoint.get() * 20.0);
        if (p.isSprinting()) rate *= Config.COMMON.sprintMultiplier.get();
        if (p.isSwimming()) rate *= 1.3;
        boolean hot = p.level().dimension() == Level.NETHER
                || p.level().getBiome(p.blockPosition()).value().getBaseTemperature() >= 1.5f;
        if (hot) rate *= Config.COMMON.hotMultiplier.get();

        v.exhaust += rate;
        if (v.exhaust >= 1.0) {
            v.exhaust -= 1.0;
            v.water = Math.max(0f, v.water - 1f);
        }

        if (v.water <= 0f) {
            if (++v.thirstTimer >= Config.COMMON.thirstDamageTicks.get()) {
                v.thirstTimer = 0;
                Difficulty d = p.level().getDifficulty();
                float floor = d == Difficulty.EASY ? 10f : d == Difficulty.NORMAL ? 1f : 0f;
                if (p.getHealth() > floor) {
                    p.hurt(source(p.level(), THIRST), 1f);
                    p.invulnerableTime = 0;
                }
            }
        } else {
            v.thirstTimer = 0;
        }

        if (Config.COMMON.thirstSlowdown.get() && v.water <= 4f && p.tickCount % 20 == 0) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false, false));
        }
    }

    private static void bleeding(ServerPlayer p, Vitals v) {
        if (!Config.COMMON.bleedEnabled.get()) {
            v.bleed = 0;
            return;
        }
        if (v.bleed <= 0) {
            v.bleedTimer = 0;
            v.clotTimer = 0;
            return;
        }
        int interval = v.bleed == 1 ? Config.COMMON.bleedTicks1.get()
                : v.bleed == 2 ? Config.COMMON.bleedTicks2.get() : Config.COMMON.bleedTicks3.get();
        if (++v.bleedTimer >= interval) {
            v.bleedTimer = 0;
            p.hurt(source(p.level(), BLEED), 1f);
            p.invulnerableTime = 0;
        }
        int clot = Config.COMMON.clotSeconds.get();
        if (v.bleed == 1 && clot > 0 && ++v.clotTimer >= clot * 20) {
            v.bleed = 0;
            v.clotTimer = 0;
        }
    }

    private static void regen(ServerPlayer p, Vitals v) {
        if (v.debt <= 0f) {
            v.regenTimer = 0;
            return;
        }
        if (p.getHealth() >= p.getMaxHealth()) {
            v.debt = 0f;
            return;
        }
        boolean fixed = v.water > 0f
                && p.getFoodData().getFoodLevel() > 0
                && p.getAirSupply() > 0
                && v.bleed == 0;
        if (!fixed) {
            v.regenTimer = 0;
            return;
        }
        if (++v.regenTimer >= Config.COMMON.debtRegenTicks.get()) {
            v.regenTimer = 0;
            p.heal(1f);
            v.debt = Math.max(0f, v.debt - 1f);
            v.fx(Vitals.FX_HEAL);
        }
    }

    private static void bandageHeal(ServerPlayer p, Vitals v) {
        if (v.healTicks <= 0) return;
        v.healTicks--;
        int interval = Math.max(1, Config.COMMON.bandageHealTicks.get() / Config.COMMON.bandageHeal.get());
        if (v.healLeft > 0 && v.healTicks % interval == 0) {
            p.heal(1f);
            v.healLeft--;
            v.fx(Vitals.FX_HEAL);
        }
        if (v.healTicks <= 0) {
            v.healLeft = 0;
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new BandagePayload(p.getId(), 0));
        }
    }

    @SubscribeEvent
    static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
            int left = target.getData(ModeHud.VITALS).healTicks;
            if (left > 0) PacketDistributor.sendToPlayer(viewer, new BandagePayload(target.getId(), left));
        }
    }

    private static boolean physical(DamageSource s) {
        return s.getEntity() != null
                || s.is(DamageTypes.FALL)
                || s.is(DamageTypes.FLY_INTO_WALL)
                || s.is(DamageTypes.CACTUS)
                || s.is(DamageTypes.STALAGMITE)
                || s.is(DamageTypes.FALLING_STALACTITE)
                || s.is(DamageTypes.FALLING_ANVIL)
                || s.is(DamageTypes.SWEET_BERRY_BUSH);
    }

    @SubscribeEvent
    static void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Vitals v = p.getData(ModeHud.VITALS);
        DamageSource s = event.getSource();
        float amount = event.getNewDamage();

        if (s.is(BLEED)) {
            v.fx(Vitals.FX_BLEED);
            return;
        }
        if (s.is(THIRST)) {
            v.debt = Math.min(20f, v.debt + amount);
            v.fx(Vitals.FX_THIRST);
            return;
        }
        if (s.is(DamageTypes.STARVE)) {
            v.debt = Math.min(20f, v.debt + amount);
            v.fx(Vitals.FX_HUNGER);
            return;
        }
        if (s.is(DamageTypes.DROWN)) {
            v.debt = Math.min(20f, v.debt + amount);
            v.fx(Vitals.FX_DROWN);
            return;
        }

        v.fx(Vitals.FX_HIT);
        if (!Config.COMMON.bleedEnabled.get() || p.isCreative() || !physical(s)) return;

        int stage = amount >= Config.COMMON.bleedDamage3.get() ? 3
                : amount >= Config.COMMON.bleedDamage2.get() ? 2
                : amount >= Config.COMMON.bleedMinDamage.get() ? 1 : 0;
        if (stage <= v.bleed) return;
        double chance = stage == 1 ? Config.COMMON.bleedChance1.get()
                : stage == 2 ? Config.COMMON.bleedChance2.get() : Config.COMMON.bleedChance3.get();
        if (p.getRandom().nextDouble() < chance) {
            v.bleed = stage;
            v.bleedTimer = 0;
            v.clotTimer = 0;
        }
    }

    @SubscribeEvent
    static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !Config.COMMON.thirstEnabled.get()) return;
        ItemStack stack = event.getItem();
        float add = 0f;
        if (stack.is(Items.POTION)) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null && contents.is(Potions.WATER)) add = 8f;
        } else if (stack.is(Items.MILK_BUCKET)) {
            add = 6f;
        } else if (stack.is(Items.HONEY_BOTTLE)) {
            add = 3f;
        } else if (stack.is(Items.MELON_SLICE)) {
            add = 2f;
        }
        if (add > 0f) {
            Vitals v = p.getData(ModeHud.VITALS);
            v.water = Math.min(Vitals.MAX_WATER, v.water + add);
        }
    }

    private record PendingEnd(ServerLevel level, BlockPos pos, int due, boolean before) {}

    private static final java.util.List<PendingEnd> pendingEnd = new java.util.ArrayList<>();

    @SubscribeEvent
    static void onNetherPortal(BlockEvent.PortalSpawnEvent event) {
        if (event.getLevel() instanceof ServerLevel sl) {
            BlockPos p = event.getPos();
            PacketDistributor.sendToPlayersNear(sl, null, p.getX(), p.getY(), p.getZ(), 64, new PortalPayload(0));
        }
    }

    @SubscribeEvent
    static void onEyePlaced(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel() instanceof ServerLevel sl
                && event.getItemStack().is(Items.ENDER_EYE)
                && sl.getBlockState(event.getPos()).is(Blocks.END_PORTAL_FRAME)) {
            pendingEnd.add(new PendingEnd(sl, event.getPos(), sl.getServer().getTickCount() + 20, countEndPortal(sl, event.getPos()) >= 9));
        }
    }

    private static int countEndPortal(ServerLevel level, BlockPos c) {
        int n = 0;
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    if (level.getBlockState(c.offset(dx, dy, dz)).is(Blocks.END_PORTAL)) n++;
                }
            }
        }
        return n;
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (pendingEnd.isEmpty()) return;
        int now = event.getServer().getTickCount();
        pendingEnd.removeIf(pe -> {
            if (now < pe.due()) return false;
            BlockPos c = pe.pos();
            if (!pe.before() && countEndPortal(pe.level(), c) >= 9) {
                PacketDistributor.sendToPlayersNear(pe.level(), null, c.getX(), c.getY(), c.getZ(), 64, new PortalPayload(1));
            }
            return true;
        });
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Vitals v = p.getData(ModeHud.VITALS);
            boolean first = !v.journeyShown;
            v.journeyShown = true;
            sync(p, v, true, first);
        }
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Vitals v = p.getData(ModeHud.VITALS);
            v.journeyShown = true;
            sync(p, v, true, false);
        }
    }

    @SubscribeEvent
    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) sync(p, p.getData(ModeHud.VITALS), true, false);
    }

    private static void sync(ServerPlayer p, Vitals v, boolean force, boolean journey) {
        int water = (int) Math.ceil(v.water);
        if (force || water != v.sentWater || v.bleed != v.sentBleed || v.fxN != v.sentFxN) {
            v.sentWater = water;
            v.sentBleed = v.bleed;
            v.sentFxN = v.fxN;
            PacketDistributor.sendToPlayer(p, new VitalsPayload(water, v.bleed, v.fxKind, v.fxN, journey));
        }
    }
}
