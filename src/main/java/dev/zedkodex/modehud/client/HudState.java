package dev.zedkodex.modehud.client;

import dev.zedkodex.modehud.Config;
import dev.zedkodex.modehud.data.Vitals;
import dev.zedkodex.modehud.net.ClientState;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceKey;

public final class HudState {
    private HudState() {}

    public static class Step {
        public int shown = -1;
        private long last;

        void update(int target, long now) {
            if (shown < 0) {
                shown = target;
                return;
            }
            if (shown == target) return;
            if (now - last >= 110) {
                shown += Integer.signum(target - shown);
                last = now;
            }
        }
    }

    public static boolean active;
    public static boolean dead;
    private static boolean wasDead;
    public static long hudStartMs;
    public static long toastGateMs;

    public static final Step hp = new Step();
    public static final Step food = new Step();
    public static final Step water = new Step();
    public static final Step air = new Step();

    public static int hpTarget = 20;
    public static boolean airVisible;
    public static int bleed;
    public static int bleedStage = 1;
    public static long bleedChangeMs;

    public static long hpFlashMs;
    public static int hpFlashRgb = 0xFFFFFF;
    public static long foodFlashMs;
    public static long waterFlashMs;
    public static long airFlashMs;

    public static long barShowMs;

    private static int lastFxN;
    private static int lastSlot = -1;
    private static ItemStack lastHeld = ItemStack.EMPTY;

    private static int currentPlace = -1;
    private static int candidate = -1;
    private static int candidateCount;
    private static boolean journeyDone;
    private static boolean dragonShown;
    private static ResourceKey<Level> lastDim;

    public static void reset() {
        active = false;
        hp.shown = food.shown = water.shown = air.shown = -1;
        bleed = 0;
        lastFxN = 0;
        lastSlot = -1;
        lastHeld = ItemStack.EMPTY;
        currentPlace = -1;
        candidate = -1;
        candidateCount = 0;
        journeyDone = false;
        dragonShown = false;
        lastDim = null;
        ClientState.reset();
        ToastPanel.reset();
        EyesPanel.reset();
        BloodPanel.reset();
    }

    private static void respawn(long now) {
        hp.shown = food.shown = water.shown = air.shown = -1;
        boolean eyes = Config.CLIENT.eyesIntro.get();
        if (eyes) EyesPanel.start(now);
        hudStartMs = now + (eyes ? 4600 : 0);
        toastGateMs = now + (eyes ? 7200 : 800);
        barShowMs = hudStartMs;
        currentPlace = -1;
        candidate = -1;
        candidateCount = 0;
        dragonShown = false;
        lastDim = null;
        bleed = 0;
        ToastPanel.forget();
        BloodPanel.reset();
    }

    private static void start(long now) {
        active = true;
        boolean eyes = Config.CLIENT.eyesIntro.get();
        if (eyes) EyesPanel.start(now);
        hudStartMs = now + (eyes ? 4600 : 0);
        toastGateMs = now + (eyes ? 7200 : 800);
        barShowMs = hudStartMs;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || mc.level == null) return;
        long now = Util.getMillis();
        if (!active) start(now);

        boolean nowDead = p.isDeadOrDying();
        if (nowDead) {
            dead = true;
            wasDead = true;
        } else {
            dead = false;
            if (wasDead) {
                wasDead = false;
                respawn(now);
            }
        }

        hpTarget = dead ? 0 : Math.max(0, Math.min(20, (int) Math.ceil(p.getHealth() / p.getMaxHealth() * 20f - 0.001f)));
        int maxAir = p.getMaxAirSupply();
        int air20 = dead ? 0 : Math.max(0, Math.min(20, Math.round(p.getAirSupply() / (float) maxAir * 20f)));
        airVisible = !dead && (p.isUnderWater() || p.getAirSupply() < maxAir);

        hp.update(hpTarget, now);
        food.update(dead ? 0 : p.getFoodData().getFoodLevel(), now);
        water.update(dead ? 0 : Math.round(ClientState.water), now);
        air.update(air20, now);

        if (ClientState.bleed != bleed) {
            bleed = ClientState.bleed;
            if (bleed > 0) bleedStage = bleed;
            bleedChangeMs = now;
        }

        if (ClientState.fxN != lastFxN) {
            lastFxN = ClientState.fxN;
            switch (ClientState.fxKind) {
                case Vitals.FX_HIT -> {
                    hpFlashMs = now + 600;
                    hpFlashRgb = 0xFFFFFF;
                }
                case Vitals.FX_BLEED -> {
                    hpFlashMs = now + 350;
                    hpFlashRgb = 0xFF5A5A;
                }
                case Vitals.FX_HUNGER -> foodFlashMs = now + 700;
                case Vitals.FX_THIRST -> waterFlashMs = now + 700;
                case Vitals.FX_DROWN -> airFlashMs = now + 700;
                case Vitals.FX_HEAL -> {
                    hpFlashMs = now + 800;
                    hpFlashRgb = 0x7CFFA0;
                }
                default -> {
                }
            }
        }

        int slot = p.getInventory().selected;
        ItemStack held = p.getMainHandItem();
        if (slot != lastSlot || !ItemStack.matches(held, lastHeld)) {
            lastSlot = slot;
            lastHeld = held.copy();
            barShowMs = now;
        }

        Integer portal;
        while ((portal = ClientState.portals.poll()) != null) {
            if (Config.CLIENT.notifications.get() && !dead) {
                ToastPanel.enqueue(Places.get(portal == 0 ? Places.NETHER_PORTAL : Places.END_PORTAL));
            }
        }

        if (!dead) trackPlaces(mc, p);
    }

    private static void trackPlaces(Minecraft mc, Player p) {
        if (!Config.CLIENT.notifications.get()) return;
        Level level = mc.level;

        if (ClientState.journey && !journeyDone) {
            journeyDone = true;
            ToastPanel.enqueue(Places.get(Places.JOURNEY));
        }

        ResourceKey<Level> dim = level.dimension();
        if (lastDim == null) {
            lastDim = dim;
        } else if (dim != lastDim) {
            lastDim = dim;
            currentPlace = -1;
            candidate = -1;
            dragonShown = false;
            if (dim == Level.NETHER) ToastPanel.enqueue(Places.get(Places.NETHER));
            else if (dim == Level.END) ToastPanel.enqueue(Places.get(Places.END));
            else ToastPanel.enqueue(Places.get(Places.BACK));
        }

        if (p.tickCount % 10 != 0) return;
        BlockPos pos = p.blockPosition();

        if (dim == Level.END && !dragonShown && p.tickCount % 20 == 0) {
            AABB box = p.getBoundingBox().inflate(220);
            if (!level.getEntitiesOfClass(EnderDragon.class, box).isEmpty()) {
                dragonShown = true;
                ToastPanel.enqueue(Places.get(Places.DRAGON));
            }
        }

        int id = detect(level, dim, pos);
        if (id == currentPlace) {
            candidate = id;
            candidateCount = 0;
            return;
        }
        if (id == candidate) candidateCount++;
        else {
            candidate = id;
            candidateCount = 1;
        }
        if (candidateCount >= 3) {
            currentPlace = id;
            ToastPanel.enqueue(Places.get(id));
        }
    }

    private static int detect(Level level, ResourceKey<Level> dim, BlockPos pos) {
        Holder<Biome> b = level.getBiome(pos);
        if (dim == Level.NETHER) {
            if (b.is(Biomes.CRIMSON_FOREST)) return Places.CRIMSON;
            if (b.is(Biomes.WARPED_FOREST)) return Places.WARPED;
            if (b.is(Biomes.SOUL_SAND_VALLEY)) return Places.SOUL;
            if (b.is(Biomes.BASALT_DELTAS)) return Places.BASALT;
            return Places.WASTES;
        }
        if (dim == Level.END) {
            if (b.is(Biomes.END_HIGHLANDS) || b.is(Biomes.END_MIDLANDS) || b.is(Biomes.END_BARRENS)) return Places.END_HIGH;
            if (b.is(Biomes.SMALL_END_ISLANDS)) return Places.END_SMALL;
            return Places.END_ISLAND;
        }
        if (b.is(Biomes.DEEP_DARK)) return Places.DEEP_DARK;
        if (pos.getY() < 50 && level.getBrightness(LightLayer.SKY, pos) == 0) return Places.CAVE;
        if (b.is(BiomeTags.IS_OCEAN)) return Places.OCEAN;
        if (b.is(BiomeTags.IS_MOUNTAIN)) return Places.MOUNTAINS;
        if (b.value().coldEnoughToSnow(pos)) return Places.SNOWY;
        if (b.is(BiomeTags.IS_JUNGLE)) return Places.JUNGLE;
        if (b.is(Biomes.SWAMP) || b.is(Biomes.MANGROVE_SWAMP)) return Places.SWAMP;
        if (b.is(Biomes.DESERT) || b.is(BiomeTags.IS_BADLANDS)) return Places.DESERT;
        if (b.is(BiomeTags.IS_FOREST) || b.is(BiomeTags.IS_TAIGA)) return Places.FOREST;
        return Places.PLAINS;
    }
}
