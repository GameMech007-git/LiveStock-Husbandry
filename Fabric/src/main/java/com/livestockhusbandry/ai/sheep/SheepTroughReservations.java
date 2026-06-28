package com.livestockhusbandry.ai.sheep;

import com.livestockhusbandry.trough.TroughAnimalType;
import com.livestockhusbandry.trough.fold.TroughFold;
import com.livestockhusbandry.trough.fold.TroughFoldKey;
import com.livestockhusbandry.trough.fold.TroughFoldUtil;
import com.livestockhusbandry.ai.trough.TroughAssignmentUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
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

    public static boolean register(
            Level level,
            UUID sheepId,
            BlockPos sheepPos,
            TroughFold fold
    ) {
        SheepKey sheepKey = new SheepKey(level.dimension(), sheepId);

        Assignment oldAssignment = SHEEP_TO_ASSIGNMENT.get(sheepKey);

        if (oldAssignment != null && oldAssignment.foldKey().equals(fold.key())) {
            return true;
        }

        if (oldAssignment != null) {
            TROUGH_BLOCK_TO_SHEEP.remove(
                    new TroughBlockKey(
                            level.dimension(),
                            oldAssignment.foldKey(),
                            oldAssignment.assignedTroughPos()
                    )
            );

            SHEEP_TO_ASSIGNMENT.remove(sheepKey);
        }

        BlockPos freeTroughPos = findFreeTroughBlock(level, sheepId, sheepPos, fold);

        if (freeTroughPos == null) {
            return false;
        }

        BlockPos eatPos = TroughAssignmentUtil.findBestEatBlock(sheepPos, freeTroughPos, fold);

        Assignment assignment = new Assignment(
                fold.key(),
                freeTroughPos.immutable(),
                eatPos.immutable()
        );

        SHEEP_TO_ASSIGNMENT.put(sheepKey, assignment);

        TROUGH_BLOCK_TO_SHEEP.put(
                new TroughBlockKey(
                        level.dimension(),
                        fold.key(),
                        freeTroughPos.immutable()
                ),
                sheepId
        );

        if (level instanceof ServerLevel serverLevel) {
            TroughFoldUtil.lockFoldAnimalType(
                    serverLevel,
                    fold,
                    TroughAnimalType.SHEEP
            );
        }

        return true;
    }

    @Nullable
    private static BlockPos findFreeTroughBlock(
            Level level,
            UUID sheepId,
            BlockPos sheepPos,
            TroughFold fold
    ) {
        BlockPos bestPos = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos troughPos : fold.troughPositions()) {
            TroughBlockKey troughBlockKey = new TroughBlockKey(
                    level.dimension(),
                    fold.key(),
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
    public static TroughFoldKey getRegisteredFoldKey(Level level, UUID sheepId) {
        Assignment assignment = SHEEP_TO_ASSIGNMENT.get(
                new SheepKey(level.dimension(), sheepId)
        );

        if (assignment == null) {
            return null;
        }

        return assignment.foldKey();
    }

    @Nullable
    public static BlockPos getRegisteredTrough(Level level, UUID sheepId) {
        Assignment assignment = SHEEP_TO_ASSIGNMENT.get(
                new SheepKey(level.dimension(), sheepId)
        );

        if (assignment == null) {
            return null;
        }

        return assignment.assignedTroughPos();
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

    public static int getRegisteredCount(Level level, TroughFoldKey foldKey) {
        int count = 0;

        for (Assignment assignment : SHEEP_TO_ASSIGNMENT.values()) {
            if (assignment.foldKey().equals(foldKey)) {
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
                new TroughBlockKey(
                        level.dimension(),
                        assignment.foldKey(),
                        assignment.assignedTroughPos()
                )
        );
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
                        old.foldKey(),
                        old.assignedTroughPos(),
                        newEatPos.immutable()
                )
        );
    }

    private record SheepKey(ResourceKey<Level> dimension, UUID sheepId) {
    }

    private record TroughBlockKey(
            ResourceKey<Level> dimension,
            TroughFoldKey foldKey,
            BlockPos pos
    ) {
    }

    private record Assignment(
            TroughFoldKey foldKey,
            BlockPos assignedTroughPos,
            BlockPos assignedEatPos
    ) {
    }

    public static void unregisterAssignedToTroughBlock(Level level, BlockPos troughPos) {
        SheepKey sheepToRemove = null;
        Assignment assignmentToRemove = null;

        for (Map.Entry<SheepKey, Assignment> entry : SHEEP_TO_ASSIGNMENT.entrySet()) {
            Assignment assignment = entry.getValue();

            if (assignment.assignedTroughPos().equals(troughPos)) {
                sheepToRemove = entry.getKey();
                assignmentToRemove = assignment;
                break;
            }
        }

        if (sheepToRemove == null) {
            return;
        }

        SHEEP_TO_ASSIGNMENT.remove(sheepToRemove);

        TROUGH_BLOCK_TO_SHEEP.remove(
                new TroughBlockKey(
                        level.dimension(),
                        assignmentToRemove.foldKey(),
                        assignmentToRemove.assignedTroughPos()
                )
        );
    }
}