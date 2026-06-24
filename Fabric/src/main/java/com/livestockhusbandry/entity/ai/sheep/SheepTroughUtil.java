package com.livestockhusbandry.entity.ai.sheep;

import com.livestockhusbandry.block.ModBlocks;
import com.livestockhusbandry.block.entity.TroughBlockEntity;
import com.livestockhusbandry.block.trough.TroughUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public final class SheepTroughUtil {

    private static final double EAT_TOLERANCE_SQR = 0.72D;

    private SheepTroughUtil() {
    }

    public static TroughUtil.TroughGroup findNearbyAvailableTroughGroup(
            Sheep sheep,
            int searchRange
    ) {
        if (!(sheep.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        BlockPos sheepPos = sheep.blockPosition();

        TroughUtil.TroughGroup bestGroup = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                sheepPos.offset(-searchRange, -2, -searchRange),
                sheepPos.offset(searchRange, 2, searchRange)
        )) {
            if (!serverLevel.getBlockState(pos).is(ModBlocks.TROUGH)) {
                continue;
            }

            TroughUtil.TroughGroup group = TroughUtil.resolveGroup(serverLevel, pos);

            if (!group.controllerPos().equals(pos)) {
                continue;
            }

            int registeredCount = SheepTroughReservations.getRegisteredCount(
                    serverLevel,
                    group.controllerPos()
            );

            if (registeredCount >= group.capacity()) {
                continue;
            }

            double distance = group.centerPos().distSqr(sheepPos);

            if (distance < bestDistance) {
                bestDistance = distance;
                bestGroup = group;
            }
        }

        return bestGroup;
    }

    public static TroughUtil.TroughGroup getRegisteredGroup(Sheep sheep) {
        if (!(sheep.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        BlockPos controllerPos = SheepTroughReservations.getRegisteredTrough(
                serverLevel,
                sheep.getUUID()
        );

        if (controllerPos == null) {
            return null;
        }

        if (!serverLevel.getBlockState(controllerPos).is(ModBlocks.TROUGH)) {
            SheepTroughReservations.unregister(serverLevel, sheep.getUUID());
            return null;
        }

        BlockPos assignedTroughPos = SheepTroughReservations.getAssignedTroughBlock(
                serverLevel,
                sheep.getUUID()
        );

        if (assignedTroughPos == null || !serverLevel.getBlockState(assignedTroughPos).is(ModBlocks.TROUGH)) {
            SheepTroughReservations.unregister(serverLevel, sheep.getUUID());
            return null;
        }

        return TroughUtil.resolveGroup(serverLevel, controllerPos);
    }

    @Nullable
    public static TroughBlockEntity getControllerTrough(ServerLevel level, TroughUtil.TroughGroup group) {
        BlockEntity blockEntity = level.getBlockEntity(group.controllerPos());

        if (blockEntity instanceof TroughBlockEntity trough) {
            return trough;
        }

        return null;
    }

    public static boolean isInsideTroughArea(Sheep sheep, TroughUtil.TroughGroup group) {
        if (!(sheep.level() instanceof ServerLevel serverLevel)) {
            return true;
        }

        TroughBlockEntity trough = getControllerTrough(serverLevel, group);

        if (trough == null) {
            return true;
        }

        return trough.isInsideSavedArea(sheep.blockPosition(), group);
    }

    public static void moveNearTrough(Sheep sheep, TroughUtil.TroughGroup group, double speedModifier) {
        BlockPos eatPos = getEatBlockPos(sheep, group);

        sheep.getNavigation().moveTo(
                eatPos.getX() + 0.5D,
                sheep.getY(),
                eatPos.getZ() + 0.5D,
                speedModifier
        );
    }

    public static void lookAtTrough(Sheep sheep, TroughUtil.TroughGroup group) {
        BlockPos assignedTroughPos = getAssignedOrCenterTroughPos(sheep, group);

        sheep.getLookControl().setLookAt(
                assignedTroughPos.getX() + 0.5D,
                assignedTroughPos.getY() + 0.4D,
                assignedTroughPos.getZ() + 0.5D
        );
    }

    public static boolean isNearTroughCenter(Sheep sheep, TroughUtil.TroughGroup group) {
        if (isStandingOnTrough(sheep)) {
            return false;
        }

        if (isAtEatBlockOrOneBehind(sheep, group)) {
            return true;
        }

        if (isBesideAssignedTrough(sheep, group)) {
            return true;
        }

        BlockPos eatPos = getEatBlockPos(sheep, group);

        double dx = sheep.getX() - (eatPos.getX() + 0.5D);
        double dz = sheep.getZ() - (eatPos.getZ() + 0.5D);

        return dx * dx + dz * dz < EAT_TOLERANCE_SQR;
    }

    public static boolean isStandingOnTrough(Sheep sheep) {
        return sheep.level().getBlockState(sheep.blockPosition()).is(ModBlocks.TROUGH)
                || sheep.level().getBlockState(sheep.blockPosition().below()).is(ModBlocks.TROUGH);
    }

    public static BlockPos getEatBlockPos(Sheep sheep, TroughUtil.TroughGroup group) {
        BlockPos assignedEatPos = SheepTroughReservations.getAssignedEatBlock(
                sheep.level(),
                sheep.getUUID()
        );

        if (assignedEatPos != null) {
            return assignedEatPos;
        }

        BlockPos troughPos = getAssignedOrCenterTroughPos(sheep, group);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = troughPos.relative(direction);

            if (!sheep.level().getBlockState(candidate).is(ModBlocks.TROUGH)) {
                return candidate;
            }
        }

        return troughPos;
    }

    public static void playEatAnimation(Sheep sheep) {
        sheep.level().broadcastEntityEvent(sheep, (byte) 10);
    }

    private static BlockPos getAssignedOrCenterTroughPos(Sheep sheep, TroughUtil.TroughGroup group) {
        BlockPos assignedTroughPos = SheepTroughReservations.getAssignedTroughBlock(
                sheep.level(),
                sheep.getUUID()
        );

        if (assignedTroughPos != null) {
            return assignedTroughPos;
        }

        return group.centerPos();
    }

    public static void reassignEatBlockNearCurrentPosition(Sheep sheep, TroughUtil.TroughGroup group) {
        BlockPos troughPos = getAssignedOrCenterTroughPos(sheep, group);

        BlockPos bestPos = null;
        double bestDistance = Double.MAX_VALUE;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = troughPos.relative(direction);

            if (sheep.level().getBlockState(candidate).is(ModBlocks.TROUGH)) {
                continue;
            }

            double distance = candidate.distSqr(sheep.blockPosition());

            if (distance < bestDistance) {
                bestDistance = distance;
                bestPos = candidate.immutable();
            }
        }

        if (bestPos != null) {
            SheepTroughReservations.updateAssignedEatBlock(
                    sheep.level(),
                    sheep.getUUID(),
                    bestPos
            );
        }
    }

    private static boolean isBesideAssignedTrough(Sheep sheep, TroughUtil.TroughGroup group) {
        BlockPos sheepPos = sheep.blockPosition();
        BlockPos assignedTroughPos = getAssignedOrCenterTroughPos(sheep, group);

        return isBesideBlock(sheepPos, assignedTroughPos);
    }

    private static boolean isBesideBlock(BlockPos sheepPos, BlockPos troughPos) {
        int dx = Math.abs(sheepPos.getX() - troughPos.getX());
        int dz = Math.abs(sheepPos.getZ() - troughPos.getZ());

        if (dx == 0 && dz == 0) {
            return false;
        }

        return dx <= 1 && dz <= 1;
    }
    private static boolean isAtEatBlockOrOneBehind(Sheep sheep, TroughUtil.TroughGroup group) {
        BlockPos sheepPos = sheep.blockPosition();
        BlockPos troughPos = getAssignedOrCenterTroughPos(sheep, group);
        BlockPos eatPos = getEatBlockPos(sheep, group);

        if (sheepPos.equals(eatPos)) {
            return true;
        }

        int dx = Integer.compare(eatPos.getX() - troughPos.getX(), 0);
        int dz = Integer.compare(eatPos.getZ() - troughPos.getZ(), 0);

        BlockPos oneBehindEatPos = eatPos.offset(dx, 0, dz);

        return sheepPos.equals(oneBehindEatPos);
    }

    public static boolean consumeWheatForWoolGrowth(Sheep sheep, TroughUtil.TroughGroup group) {
        if (!(sheep.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        TroughBlockEntity trough = getControllerTrough(serverLevel, group);

        if (trough == null) {
            return false;
        }

        return trough.consumeWheat(1);
    }

    public static boolean hasWheatForWoolGrowth(Sheep sheep, TroughUtil.TroughGroup group) {
        if (!(sheep.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        TroughBlockEntity trough = getControllerTrough(serverLevel, group);

        if (trough == null) {
            return false;
        }

        return trough.getWheatCount() >= 1;
    }

}