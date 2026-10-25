package dev.zedkodex.modehud.world;

import java.util.Map;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public class SmoothTerrainFeature extends Feature<NoneFeatureConfiguration> {
    public SmoothTerrainFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        ChunkPos cp = new ChunkPos(ctx.origin());
        ChunkAccess chunk = level.getChunk(cp.x, cp.z);
        boolean placed = false;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = cp.getMinBlockX() + dx;
                int z = cp.getMinBlockZ() + dz;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                BlockPos low = new BlockPos(x, y, z);
                BlockState lowState = level.getBlockState(low);
                if (!isGround(lowState)) continue;
                BlockPos above = low.above();
                if (!level.getBlockState(above).isAir() || !level.getFluidState(above).isEmpty()) continue;

                for (Direction d : Direction.Plane.HORIZONTAL) {
                    int nx = x + d.getStepX();
                    int nz = z + d.getStepZ();
                    int ny = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, nx, nz) - 1;
                    if (ny != y + 1) continue;
                    BlockPos high = new BlockPos(nx, ny, nz);
                    BlockState highState = level.getBlockState(high);
                    if (!level.getBlockState(high.above()).isAir()) continue;
                    BlockState slab = slabState(highState);
                    if (slab == null || inStructure(chunk, level, above) || inStructure(chunk, level, high)) continue;
                    level.setBlock(above, slab, 2);
                    placed = true;
                    break;
                }
            }
        }
        return placed;
    }

    private static boolean isGround(BlockState s) {
        return !s.isAir() && !s.is(Blocks.SNOW) && s.isSolid() && s.getFluidState().isEmpty()
                && (ModBlocks.slabFor(s.getBlock()) != null || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.STONE) || s.is(Blocks.DEEPSLATE)
                || s.is(Blocks.TERRACOTTA) || s.is(Blocks.ANDESITE) || s.is(Blocks.GRANITE) || s.is(Blocks.DIORITE) || s.is(Blocks.ROOTED_DIRT));
    }

    private static BlockState slabState(BlockState parent) {
        Block b = parent.getBlock();
        if (b == Blocks.SNOW_BLOCK) return Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 4);
        SlabBlock slab = ModBlocks.slabFor(b);
        if (slab == null) return null;
        return slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    }

    private static boolean inStructure(ChunkAccess chunk, WorldGenLevel level, BlockPos pos) {
        try {
            ChunkAccess c = (chunk.getPos().x == (pos.getX() >> 4) && chunk.getPos().z == (pos.getZ() >> 4))
                    ? chunk : level.getChunk(pos);
            for (StructureStart s : c.getAllStarts().values()) {
                if (s.isValid() && s.getBoundingBox().inflatedBy(3).isInside(pos)) return true;
            }
            for (Map.Entry<net.minecraft.world.level.levelgen.structure.Structure, it.unimi.dsi.fastutil.longs.LongSet> e : c.getAllReferences().entrySet()) {
                for (long l : e.getValue()) {
                    ChunkPos sp = new ChunkPos(l);
                    if (!level.hasChunk(sp.x, sp.z)) continue;
                    StructureStart st = level.getChunk(sp.x, sp.z).getStartForStructure(e.getKey());
                    if (st != null && st.isValid() && st.getBoundingBox().inflatedBy(3).isInside(pos)) return true;
                }
            }
        } catch (Exception ignored) {
            return true;
        }
        return false;
    }
}
