package com.livestockhusbandry.entity.ai.cow;

import com.livestockhusbandry.block.ModBlocks;
import com.livestockhusbandry.block.entity.TroughBlockEntity;
import com.livestockhusbandry.block.trough.TroughAnimalType;
import com.livestockhusbandry.block.trough.TroughUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public final class CowTroughUtil {

    private static final double EAT_TOLERANCE_SQR = 0.72D;

    private CowTroughUtil() {
    }

    public static TroughUtil.TroughGroup findNearbyAvailableTroughGroup(
            Cow cow,
            int searchRange
    ) {
        if (!(cow.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        BlockPos cowPos = cow.blockPosition();

        TroughUtil.TroughGroup bestGroup = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                cowPos.offset(-searchRange, -2, -searchRange),
                cowPos.offset(searchRange, 2, searchRange)
        )) {
            if (!serverLevel.getBlockState(pos).is(ModBlocks.TROUGH)) {
                continue;
            }

            TroughUtil.TroughGroup group = TroughUtil.resolveGroup(serverLevel, pos);

            if (!group.controllerPos().equals(pos)) {
                continue;
            }

            TroughBlockEntity trough = getControllerTrough(serverLevel, group);

            if (trough == null) {
                continue;
            }

            if (!trough.canRegisterAnimal(TroughAnimalType.COW)) {
                continue;
            }

            int registeredCount = CowTroughReservations.getRegisteredCount(
                    serverLevel,
                    group.controllerPos()
            );

            int cowRegisterLimit = group.capacity() + 2;

            if (registeredCount >= cowRegisterLimit) {
                continue;
            }

            double distance = group.centerPos().distSqr(cowPos);

            if (distance < bestDistance) {
                bestDistance = distance;
                bestGroup = group;
            }
        }

        return bestGroup;
    }

    public static TroughUtil.TroughGroup getRegisteredGroup(Cow cow) {
        if (!(cow.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        BlockPos controllerPos = CowTroughReservations.getRegisteredTrough(
                serverLevel,
                cow.getUUID()
        );

        if (controllerPos == null) {
            return null;
        }

        if (!serverLevel.getBlockState(controllerPos).is(ModBlocks.TROUGH)) {
            CowTroughReservations.unregister(serverLevel, cow.getUUID());
            return null;
        }

        BlockPos assignedTroughPos = CowTroughReservations.getAssignedTroughBlock(
                serverLevel,
                cow.getUUID()
        );

        if (assignedTroughPos == null || !serverLevel.getBlockState(assignedTroughPos).is(ModBlocks.TROUGH)) {
            CowTroughReservations.unregister(serverLevel, cow.getUUID());
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

    public static boolean isInsideTroughArea(Cow cow, TroughUtil.TroughGroup group) {
        if (!(cow.level() instanceof ServerLevel serverLevel)) {
            return true;
        }

        TroughBlockEntity trough = getControllerTrough(serverLevel, group);

        if (trough == null) {
            return true;
        }

        return trough.isInsideSavedArea(cow.blockPosition(), group);
    }

    public static void moveNearTrough(Cow cow, TroughUtil.TroughGroup group, double speedModifier) {
        BlockPos eatPos = getEatBlockPos(cow, group);

        cow.getNavigation().moveTo(
                eatPos.getX() + 0.5D,
                cow.getY(),
                eatPos.getZ() + 0.5D,
                speedModifier
        );
    }

    public static void lookAtTrough(Cow cow, TroughUtil.TroughGroup group) {
        BlockPos assignedTroughPos = getAssignedOrCenterTroughPos(cow, group);

        cow.getLookControl().setLookAt(
                assignedTroughPos.getX() + 0.5D,
                assignedTroughPos.getY() + 0.4D,
                assignedTroughPos.getZ() + 0.5D
        );
    }

    public static boolean isNearTroughCenter(Cow cow, TroughUtil.TroughGroup group) {
        if (isStandingOnTrough(cow)) {
            return false;
        }

        if (isAtEatBlockOrOneBehind(cow, group)) {
            return true;
        }

        if (isBesideAssignedTrough(cow, group)) {
            return true;
        }

        BlockPos eatPos = getEatBlockPos(cow, group);

        double dx = cow.getX() - (eatPos.getX() + 0.5D);
        double dz = cow.getZ() - (eatPos.getZ() + 0.5D);

        return dx * dx + dz * dz < EAT_TOLERANCE_SQR;
    }

    public static boolean isStandingOnTrough(Cow cow) {
        return cow.level().getBlockState(cow.blockPosition()).is(ModBlocks.TROUGH)
                || cow.level().getBlockState(cow.blockPosition().below()).is(ModBlocks.TROUGH);
    }

    public static BlockPos getEatBlockPos(Cow cow, TroughUtil.TroughGroup group) {
        BlockPos assignedEatPos = CowTroughReservations.getAssignedEatBlock(
                cow.level(),
                cow.getUUID()
        );

        if (assignedEatPos != null) {
            return assignedEatPos;
        }

        BlockPos troughPos = getAssignedOrCenterTroughPos(cow, group);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = troughPos.relative(direction);

            if (!cow.level().getBlockState(candidate).is(ModBlocks.TROUGH)) {
                return candidate;
            }
        }

        return troughPos;
    }

    private static BlockPos getAssignedOrCenterTroughPos(Cow cow, TroughUtil.TroughGroup group) {
        BlockPos assignedTroughPos = CowTroughReservations.getAssignedTroughBlock(
                cow.level(),
                cow.getUUID()
        );

        if (assignedTroughPos != null) {
            return assignedTroughPos;
        }

        return group.centerPos();
    }

    public static void reassignEatBlockNearCurrentPosition(Cow cow, TroughUtil.TroughGroup group) {
        BlockPos troughPos = getAssignedOrCenterTroughPos(cow, group);

        BlockPos bestPos = null;
        double bestDistance = Double.MAX_VALUE;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = troughPos.relative(direction);

            if (cow.level().getBlockState(candidate).is(ModBlocks.TROUGH)) {
                continue;
            }

            double distance = candidate.distSqr(cow.blockPosition());

            if (distance < bestDistance) {
                bestDistance = distance;
                bestPos = candidate.immutable();
            }
        }

        if (bestPos != null) {
            CowTroughReservations.updateAssignedEatBlock(
                    cow.level(),
                    cow.getUUID(),
                    bestPos
            );
        }
    }

    private static boolean isBesideAssignedTrough(Cow cow, TroughUtil.TroughGroup group) {
        BlockPos cowPos = cow.blockPosition();
        BlockPos assignedTroughPos = getAssignedOrCenterTroughPos(cow, group);

        return isBesideBlock(cowPos, assignedTroughPos);
    }

    private static boolean isBesideBlock(BlockPos cowPos, BlockPos troughPos) {
        int dx = Math.abs(cowPos.getX() - troughPos.getX());
        int dz = Math.abs(cowPos.getZ() - troughPos.getZ());

        if (dx == 0 && dz == 0) {
            return false;
        }

        return dx <= 1 && dz <= 1;
    }
    private static boolean isAtEatBlockOrOneBehind(Cow cow, TroughUtil.TroughGroup group) {
        BlockPos cowPos = cow.blockPosition();
        BlockPos troughPos = getAssignedOrCenterTroughPos(cow, group);
        BlockPos eatPos = getEatBlockPos(cow, group);

        if (cowPos.equals(eatPos)) {
            return true;
        }

        int dx = Integer.compare(eatPos.getX() - troughPos.getX(), 0);
        int dz = Integer.compare(eatPos.getZ() - troughPos.getZ(), 0);

        BlockPos oneBehindEatPos = eatPos.offset(dx, 0, dz);

        return cowPos.equals(oneBehindEatPos);
    }

}