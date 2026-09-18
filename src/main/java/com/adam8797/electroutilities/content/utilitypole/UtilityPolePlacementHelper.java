package com.adam8797.electroutilities.content.utilitypole;

import java.util.List;
import java.util.function.Predicate;

import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementOffset;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Shaft-style placement for utility poles: aiming at the top or bottom of an existing pole and using a
 * utility pole item places the next segment above/below it. Utility poles are vertical only, so the
 * column always extends along the Y axis.
 */
public class UtilityPolePlacementHelper implements IPlacementHelper {

    private static final int RANGE = 32;

    @Override
    public Predicate<ItemStack> getItemPredicate() {
        return stack -> stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof UtilityPoleBlock;
    }

    @Override
    public Predicate<BlockState> getStatePredicate() {
        return state -> state.getBlock() instanceof UtilityPoleBlock;
    }

    @Override
    public PlacementOffset getOffset(Player player, Level level, BlockState state, BlockPos pos, BlockHitResult ray) {
        Direction.Axis axis = Direction.Axis.Y; // vertical only
        List<Direction> directions = IPlacementHelper.orderedByDistanceOnlyAxis(pos, ray.getLocation(), axis);
        for (Direction dir : directions) {
            BlockPos cursor = pos;
            int count = 0;
            while (count < RANGE) {
                BlockState next = level.getBlockState(cursor.relative(dir));
                if (next.getBlock() instanceof UtilityPoleBlock && next.getValue(RotatedPillarBlock.AXIS) == axis) {
                    cursor = cursor.relative(dir);
                    count++;
                } else {
                    break;
                }
            }
            BlockPos target = cursor.relative(dir);
            if (level.getBlockState(target).canBeReplaced())
                return PlacementOffset.success(target, s -> s.setValue(RotatedPillarBlock.AXIS, axis));
        }
        return PlacementOffset.fail();
    }
}
