package com.dreadmod.block;

import com.dreadmod.DreadUtil;
import com.dreadmod.FearManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** Lapide: leggere l'iscrizione ti terrorizza... e potrebbe attirare qualcosa. */
public class TombstoneBlock extends HorizontalFacingBlock {
    private static final VoxelShape SHAPE_NS = Block.createCuboidShape(2, 0, 4, 14, 14, 12);
    private static final VoxelShape SHAPE_EW = Block.createCuboidShape(4, 0, 2, 12, 14, 14);
    public static final int INSCRIPTIONS = 5;

    public TombstoneBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction.Axis axis = state.get(FACING).getAxis();
        return axis == Direction.Axis.X ? SHAPE_EW : SHAPE_NS;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
                              BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (!world.isClient && player instanceof ServerPlayerEntity sp) {
            int idx = world.random.nextInt(INSCRIPTIONS);
            sp.sendMessage(Text.translatable("message.dreadmod.tombstone." + idx)
                    .formatted(Formatting.GRAY, Formatting.ITALIC), true);
            DreadUtil.playAtPlayer(sp, SoundEvents.PARTICLE_SOUL_ESCAPE, 1.0f, 0.6f);
            FearManager.add(sp, 8);
            if (FearManager.get(sp) >= 50 && world.random.nextInt(4) == 0 && world instanceof ServerWorld sw) {
                FearManager.spawnStalker(sw, sp);
            }
        }
        return ActionResult.success(world.isClient);
    }
}
