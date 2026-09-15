package com.adam8797.electroutilities.content.substation;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Shaft-style placement for substation poles: aiming at one end of an existing column and using a
 * substation item places the next segment along the column's axis (Create's arrow-guided assist).
 */
public class SubstationPlacementHelper implements IPlacementHelper {

    @Override
    public Predicate<ItemStack> getItemPredicate() {
        return stack -> stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof SubstationPoleBlock;
    }

    @Override
    public Predicate<BlockState> getStatePredicate() {
        return state -> state.getBlock() instanceof SubstationPoleBlock;
    }

    @Override
    public PlacementOffset getOffset(Player player, Level level, BlockState state, BlockPos pos, BlockHitResult ray) {
        Direction.Axis axis = state.getValue(SubstationPoleBlock.AXIS);
        List<Direction> directions = IPlacementHelper.orderedByDistanceOnlyAxis(pos, ray.getLocation(), axis);
        for (Direction dir : directions) {
            BlockPos cursor = pos;
            int count = 0;
            while (count < SubstationPoleBlock.MAX_HEIGHT) {
                BlockState next = level.getBlockState(cursor.relative(dir));
                if (next.getBlock() instanceof SubstationPoleBlock && next.getValue(SubstationPoleBlock.AXIS) == axis) {
                    cursor = cursor.relative(dir);
                    count++;
                } else {
                    break;
                }
            }
            BlockPos target = cursor.relative(dir);
            if (level.getBlockState(target).canBeReplaced())
                return PlacementOffset.success(target, s -> s.setValue(SubstationPoleBlock.AXIS, axis));
        }
        return PlacementOffset.fail();
    }
}
