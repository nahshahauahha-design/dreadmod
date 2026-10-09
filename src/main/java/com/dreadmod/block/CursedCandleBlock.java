package com.dreadmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/** Candela con fiamma spettrale: illumina (livello 12) e per il sistema della paura conta come luce sicura. */
public class CursedCandleBlock extends Block {
    private static final VoxelShape SHAPE = Block.createCuboidShape(6, 0, 6, 10, 11, 10);

    public CursedCandleBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return Block.sideCoversSmallSquare(world, pos.down(), Direction.UP);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.95;
        double z = pos.getZ() + 0.5;
        world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.005, 0);
        if (random.nextInt(6) == 0) {
            world.addParticle(ParticleTypes.SMOKE, x, y + 0.1, z, 0, 0.02, 0);
        }
        if (random.nextInt(40) == 0) {
            world.playSound(x, y, z, SoundEvents.BLOCK_CANDLE_AMBIENT, SoundCategory.BLOCKS,
                    1.0f, 0.7f, false);
        }
    }
}
