package com.livestockhusbandry.ai.largelivestock;

import com.livestockhusbandry.block.ModBlocks;
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

public final class LargeLivestockTroughReservations {

    private static final Map<AnimalKey, Assignment> ANIMAL_TO_ASSIGNMENT = new HashMap<>();

    private LargeLivestockTroughReservations() {
    }

    public static boolean register(
            Level level,
            TroughAnimalType animalType,
            UUID animalId,
            BlockPos animalPos,
            TroughFold fold
    ) {
        AnimalKey animalKey = new AnimalKey(
                level.dimension(),
                animalType,
                animalId
        );

        Assignment oldAssignment = ANIMAL_TO_ASSIGNMENT.get(animalKey);

        if (oldAssignment != null && oldAssignment.foldKey().equals(fold.key())) {
            return true;
        }

        if (oldAssignment != null) {
            ANIMAL_TO_ASSIGNMENT.remove(animalKey);
        }

        BlockPos assignedTroughPos = TroughAssignmentUtil.findNearestTroughBlock(
                animalPos,
                fold
        );

        if (assignedTroughPos == null) {
            return false;
        }

        BlockPos eatPos = TroughAssignmentUtil.findBestEatBlock(
                animalPos,
                assignedTroughPos,
                fold
        );

        ANIMAL_TO_ASSIGNMENT.put(
                animalKey,
                new Assignment(
                        fold.key(),
                        assignedTroughPos.immutable(),
                        eatPos.immutable()
                )
        );

        if (level instanceof ServerLevel serverLevel) {
            TroughFoldUtil.lockFoldAnimalType(
                    serverLevel,
                    fold,
                    animalType
            );
        }

        return true;
    }

    @Nullable
    public static TroughFoldKey getRegisteredFoldKey(
            Level level,
            TroughAnimalType animalType,
            UUID animalId
    ) {
        Assignment assignment = ANIMAL_TO_ASSIGNMENT.get(
                new AnimalKey(
                        level.dimension(),
                        animalType,
                        animalId
                )
        );

        if (assignment == null) {
            return null;
        }

        return assignment.foldKey();
    }

    @Nullable
    public static BlockPos getAssignedTroughBlock(
            Level level,
            TroughAnimalType animalType,
            UUID animalId
    ) {
        Assignment assignment = ANIMAL_TO_ASSIGNMENT.get(
                new AnimalKey(
                        level.dimension(),
                        animalType,
                        animalId
                )
        );

        if (assignment == null) {
            return null;
        }

        return assignment.assignedTroughPos();
    }

    @Nullable
    public static BlockPos getAssignedEatBlock(
            Level level,
            TroughAnimalType animalType,
            UUID animalId
    ) {
        Assignment assignment = ANIMAL_TO_ASSIGNMENT.get(
                new AnimalKey(
                        level.dimension(),
                        animalType,
                        animalId
                )
        );

        if (assignment == null) {
            return null;
        }

        return assignment.assignedEatPos();
    }

    public static int getRegisteredCount(
            Level level,
            TroughAnimalType animalType,
            TroughFoldKey foldKey
    ) {
        int count = 0;

        for (Map.Entry<AnimalKey, Assignment> entry : ANIMAL_TO_ASSIGNMENT.entrySet()) {
            AnimalKey animalKey = entry.getKey();
            Assignment assignment = entry.getValue();

            if (animalKey.dimension().equals(level.dimension())
                    && animalKey.animalType() == animalType
                    && assignment.foldKey().equals(foldKey)) {
                count++;
            }
        }

        return count;
    }

    public static void unregister(
            Level level,
            TroughAnimalType animalType,
            UUID animalId
    ) {
        ANIMAL_TO_ASSIGNMENT.remove(
                new AnimalKey(
                        level.dimension(),
                        animalType,
                        animalId
                )
        );
    }

    public static void reassignIfAssignedTroughMissing(
            Level level,
            TroughAnimalType animalType,
            UUID animalId,
            TroughFold fold,
            BlockPos animalPos
    ) {
        AnimalKey animalKey = new AnimalKey(
                level.dimension(),
                animalType,
                animalId
        );

        Assignment old = ANIMAL_TO_ASSIGNMENT.get(animalKey);

        if (old == null) {
            return;
        }

        BlockPos assignedTroughPos = old.assignedTroughPos();

        if (level.getBlockState(assignedTroughPos).is(ModBlocks.TROUGH)) {
            return;
        }

        BlockPos newTroughPos = TroughAssignmentUtil.findNearestTroughBlock(
                animalPos,
                fold
        );

        if (newTroughPos == null) {
            unregister(level, animalType, animalId);
            return;
        }

        BlockPos newEatPos = TroughAssignmentUtil.findBestEatBlock(
                animalPos,
                newTroughPos,
                fold
        );

        ANIMAL_TO_ASSIGNMENT.put(
                animalKey,
                new Assignment(
                        old.foldKey(),
                        newTroughPos.immutable(),
                        newEatPos.immutable()
                )
        );
    }

    private record AnimalKey(
            ResourceKey<Level> dimension,
            TroughAnimalType animalType,
            UUID animalId
    ) {
    }

    private record Assignment(
            TroughFoldKey foldKey,
            BlockPos assignedTroughPos,
            BlockPos assignedEatPos
    ) {
    }
}