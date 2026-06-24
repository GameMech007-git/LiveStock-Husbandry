package com.livestockhusbandry.entity.ai.sheep;

import com.livestockhusbandry.block.trough.TroughUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SheepTroughReservations {

    private static final Map<SheepKey, Assignment> SHEEP_TO_ASSIGNMENT = new HashMap<>();
    private static final Map<TroughBlockKey, UUID> TROUGH_BLOCK_TO_SHEEP = new HashMap<>();

    private SheepTroughReservations() {
    }

    public static boolean register(Level level, UUID sheepId, BlockPos sheepPos, TroughUtil.TroughGroup group) {
        SheepKey sheepKey = new SheepKey(level.dimension(), sheepId);

        Assignment oldAssignment = SHEEP_TO_ASSIGNMENT.get(sheepKey);

        if (oldAssignment != null && oldAssignment.controllerPos().equals(group.controllerPos())) {
            return true;
        }

        if (oldAssignment != null) {
            TROUGH_BLOCK_TO_SHEEP.remove(
                    new TroughBlockKey(level.dimension(), oldAssignment.assignedTroughPos())
            );
            SHEEP_TO_ASSIGNMENT.remove(sheepKey);
        }

        BlockPos freeTroughPos = findFreeTroughBlock(level, sheepId, sheepPos, group);

        if (freeTroughPos == null) {
            return false;
        }

        BlockPos eatPos = findBestEatBlock(level, sheepPos, freeTroughPos, group);

        Assignment assignment = new Assignment(
                group.controllerPos().immutable(),
                freeTroughPos.immutable(),
                eatPos.immutable()
        );

        SHEEP_TO_ASSIGNMENT.put(sheepKey, assignment);
        TROUGH_BLOCK_TO_SHEEP.put(
                new TroughBlockKey(level.dimension(), freeTroughPos.immutable()),
                sheepId
        );

        return true;
    }

    private static BlockPos findBestEatBlock(
            Level level,
            BlockPos sheepPos,
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

            double distance = candidate.distSqr(sheepPos);

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
            UUID sheepId,
            BlockPos sheepPos,
            TroughUtil.TroughGroup group
    ) {
        BlockPos bestPos = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos troughPos : group.troughPositions()) {
            TroughBlockKey troughBlockKey = new TroughBlockKey(
                    level.dimension(),
                    troughPos.immutable()
            );

            UUID currentSheep = TROUGH_BLOCK_TO_SHEEP.get(troughBlockKey);

            if (currentSheep != null && !currentSheep.equals(sheepId)) {
                continue;
            }

            double distance = troughPos.distSqr(sheepPos);

            if (distance < bestDistance) {
                bestDistance = distance;
                bestPos = troughPos.immutable();
            }
        }

        return bestPos;
    }

    @Nullable
    public static BlockPos getRegisteredTrough(Level level, UUID sheepId) {
        Assignment assignment = SHEEP_TO_ASSIGNMENT.get(
                new SheepKey(level.dimension(), sheepId)
        );

        if (assignment == null) {
            return null;
        }

        return assignment.controllerPos();
    }

    @Nullable
    public static BlockPos getAssignedTroughBlock(Level level, UUID sheepId) {
        Assignment assignment = SHEEP_TO_ASSIGNMENT.get(
                new SheepKey(level.dimension(), sheepId)
        );

        if (assignment == null) {
            return null;
        }

        return assignment.assignedTroughPos();
    }

    public static int getRegisteredCount(Level level, BlockPos controllerPos) {
        int count = 0;

        for (Assignment assignment : SHEEP_TO_ASSIGNMENT.values()) {
            if (assignment.controllerPos().equals(controllerPos)) {
                count++;
            }
        }

        return count;
    }

    public static void unregister(Level level, UUID sheepId) {
        SheepKey sheepKey = new SheepKey(level.dimension(), sheepId);
        Assignment assignment = SHEEP_TO_ASSIGNMENT.remove(sheepKey);

        if (assignment == null) {
            return;
        }

        TROUGH_BLOCK_TO_SHEEP.remove(
                new TroughBlockKey(level.dimension(), assignment.assignedTroughPos())
        );
    }

    @Nullable
    public static BlockPos getAssignedEatBlock(Level level, UUID sheepId) {
        Assignment assignment = SHEEP_TO_ASSIGNMENT.get(
                new SheepKey(level.dimension(), sheepId)
        );

        if (assignment == null) {
            return null;
        }

        return assignment.assignedEatPos();
    }

    public static void updateAssignedEatBlock(Level level, UUID sheepId, BlockPos newEatPos) {
        SheepKey sheepKey = new SheepKey(level.dimension(), sheepId);
        Assignment old = SHEEP_TO_ASSIGNMENT.get(sheepKey);

        if (old == null) {
            return;
        }

        SHEEP_TO_ASSIGNMENT.put(
                sheepKey,
                new Assignment(
                        old.controllerPos(),
                        old.assignedTroughPos(),
                        newEatPos.immutable()
                )
        );
    }

    private record SheepKey(ResourceKey<Level> dimension, UUID sheepId) {
    }

    private record TroughBlockKey(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private record Assignment(BlockPos controllerPos, BlockPos assignedTroughPos, BlockPos assignedEatPos) {
    }
}