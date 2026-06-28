package com.livestockhusbandry.entity.ai.cow;

import com.livestockhusbandry.block.entity.TroughBlockEntity;
import com.livestockhusbandry.block.trough.TroughAnimalType;
import com.livestockhusbandry.block.trough.TroughUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CowTroughReservations {

    private static final Map<CowKey, Assignment> COW_TO_ASSIGNMENT = new HashMap<>();
    private static final Map<TroughBlockKey, UUID> TROUGH_BLOCK_TO_COW = new HashMap<>();

    private CowTroughReservations() {
    }

    public static boolean register(Level level, UUID cowId, BlockPos cowPos, TroughUtil.TroughGroup group) {
        CowKey cowKey = new CowKey(level.dimension(), cowId);

        Assignment oldAssignment = COW_TO_ASSIGNMENT.get(cowKey);

        if (oldAssignment != null && oldAssignment.controllerPos().equals(group.controllerPos())) {
            return true;
        }

        if (oldAssignment != null) {
            TROUGH_BLOCK_TO_COW.remove(
                    new TroughBlockKey(level.dimension(), oldAssignment.assignedTroughPos())
            );
            COW_TO_ASSIGNMENT.remove(cowKey);
        }

        BlockPos freeTroughPos = findFreeTroughBlock(level, cowId, cowPos, group);

        if (freeTroughPos == null) {
            return false;
        }

        BlockPos eatPos = findBestEatBlock(level, cowPos, freeTroughPos, group);

        Assignment assignment = new Assignment(
                group.controllerPos().immutable(),
                freeTroughPos.immutable(),
                eatPos.immutable()
        );

        COW_TO_ASSIGNMENT.put(cowKey, assignment);
        TroughBlockKey troughBlockKey = new TroughBlockKey(
                level.dimension(),
                freeTroughPos.immutable()
        );

        TROUGH_BLOCK_TO_COW.putIfAbsent(troughBlockKey, cowId);

        if (level instanceof ServerLevel serverLevel) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(group.controllerPos());

            if (blockEntity instanceof TroughBlockEntity trough) {
                trough.lockAnimalType(TroughAnimalType.COW);
            }
        }

        return true;
    }

    private static BlockPos findBestEatBlock(
            Level level,
            BlockPos cowPos,
            BlockPos troughPos,
            TroughUtil.TroughGroup group
    ) {
        BlockPos bestPos = null;
        double bestDistance = Double.MAX_VALUE;

        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos candidate = troughPos.relative(direction);

            boolean isAnotherTrough = false;

            for (BlockPos groupTroughPos : group.troughPositions()) {
                if (groupTroughPos.equals(candidate)) {
                    isAnotherTrough = true;
                    break;
                }
            }

            if (isAnotherTrough) {
                continue;
            }

            double distance = candidate.distSqr(cowPos);

            if (distance < bestDistance) {
                bestDistance = distance;
                bestPos = candidate.immutable();
            }
        }

        if (bestPos == null) {
            return troughPos;
        }

        return bestPos;
    }

    @Nullable
    private static BlockPos findFreeTroughBlock(
            Level level,
            UUID cowId,
            BlockPos cowPos,
            TroughUtil.TroughGroup group
    ) {
        BlockPos bestFreePos = null;
        double bestFreeDistance = Double.MAX_VALUE;

        BlockPos bestAnyPos = null;
        double bestAnyDistance = Double.MAX_VALUE;

        for (BlockPos troughPos : group.troughPositions()) {
            double distance = troughPos.distSqr(cowPos);

            if (distance < bestAnyDistance) {
                bestAnyDistance = distance;
                bestAnyPos = troughPos.immutable();
            }

            TroughBlockKey troughBlockKey = new TroughBlockKey(
                    level.dimension(),
                    troughPos.immutable()
            );

            UUID currentCow = TROUGH_BLOCK_TO_COW.get(troughBlockKey);

            if (currentCow != null && !currentCow.equals(cowId)) {
                continue;
            }

            if (distance < bestFreeDistance) {
                bestFreeDistance = distance;
                bestFreePos = troughPos.immutable();
            }
        }

        if (bestFreePos != null) {
            return bestFreePos;
        }
        return bestAnyPos;
    }

    @Nullable
    public static BlockPos getRegisteredTrough(Level level, UUID cowId) {
        Assignment assignment = COW_TO_ASSIGNMENT.get(
                new CowKey(level.dimension(), cowId)
        );

        if (assignment == null) {
            return null;
        }

        return assignment.controllerPos();
    }

    @Nullable
    public static BlockPos getAssignedTroughBlock(Level level, UUID cowId) {
        Assignment assignment = COW_TO_ASSIGNMENT.get(
                new CowKey(level.dimension(), cowId)
        );

        if (assignment == null) {
            return null;
        }

        return assignment.assignedTroughPos();
    }

    public static int getRegisteredCount(Level level, BlockPos controllerPos) {
        int count = 0;

        for (Assignment assignment : COW_TO_ASSIGNMENT.values()) {
            if (assignment.controllerPos().equals(controllerPos)) {
                count++;
            }
        }

        return count;
    }

    public static void unregister(Level level, UUID cowId) {
        CowKey cowKey = new CowKey(level.dimension(), cowId);
        Assignment assignment = COW_TO_ASSIGNMENT.remove(cowKey);

        if (assignment == null) {
            return;
        }

        TROUGH_BLOCK_TO_COW.remove(
                new TroughBlockKey(level.dimension(), assignment.assignedTroughPos())
        );
    }

    @Nullable
    public static BlockPos getAssignedEatBlock(Level level, UUID cowId) {
        Assignment assignment = COW_TO_ASSIGNMENT.get(
                new CowKey(level.dimension(), cowId)
        );

        if (assignment == null) {
            return null;
        }

        return assignment.assignedEatPos();
    }

    public static void updateAssignedEatBlock(Level level, UUID cowId, BlockPos newEatPos) {
        CowKey cowKey = new CowKey(level.dimension(), cowId);
        Assignment old = COW_TO_ASSIGNMENT.get(cowKey);

        if (old == null) {
            return;
        }

        COW_TO_ASSIGNMENT.put(
                cowKey,
                new Assignment(
                        old.controllerPos(),
                        old.assignedTroughPos(),
                        newEatPos.immutable()
                )
        );
    }

    private record CowKey(ResourceKey<Level> dimension, UUID cowId) {
    }

    private record TroughBlockKey(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private record Assignment(BlockPos controllerPos, BlockPos assignedTroughPos, BlockPos assignedEatPos) {
    }
}