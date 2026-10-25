package dev.zedkodex.modehud.world;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import dev.zedkodex.modehud.ModeHud;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ModeHud.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ModeHud.MODID);

    public static final Map<String, DeferredBlock<SlabBlock>> SLABS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<BlockItem>> SLAB_ITEMS = new LinkedHashMap<>();
    private static final Map<String, Supplier<Block>> PARENTS = new LinkedHashMap<>();

    private static void slab(String name, Supplier<Block> parent) {
        DeferredBlock<SlabBlock> b = BLOCKS.register(name, () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(parent.get())));
        SLABS.put(name, b);
        SLAB_ITEMS.put(name, ITEMS.registerSimpleBlockItem(name, b));
        PARENTS.put(name, parent);
    }

    static {
        slab("grass_slab", () -> Blocks.GRASS_BLOCK);
        slab("dirt_slab", () -> Blocks.DIRT);
        slab("coarse_dirt_slab", () -> Blocks.COARSE_DIRT);
        slab("podzol_slab", () -> Blocks.PODZOL);
        slab("mycelium_slab", () -> Blocks.MYCELIUM);
        slab("sand_slab", () -> Blocks.SAND);
        slab("red_sand_slab", () -> Blocks.RED_SAND);
        slab("gravel_slab", () -> Blocks.GRAVEL);
        slab("mud_slab", () -> Blocks.MUD);
        slab("rough_stone_slab", () -> Blocks.STONE);
        slab("clay_slab", () -> Blocks.CLAY);
    }

    private static Map<Block, SlabBlock> lookup;

    public static SlabBlock slabFor(Block parent) {
        if (lookup == null) {
            lookup = new java.util.HashMap<>();
            for (Map.Entry<String, Supplier<Block>> e : PARENTS.entrySet()) lookup.put(e.getValue().get(), SLABS.get(e.getKey()).get());
        }
        return lookup.get(parent);
    }

    public static Iterable<? extends Item> allItems() {
        return SLAB_ITEMS.values().stream().map(DeferredItem::get).toList();
    }
}
