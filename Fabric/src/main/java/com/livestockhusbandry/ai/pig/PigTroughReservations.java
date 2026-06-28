package com.livestockhusbandry.ai.pig;

import com.livestockhusbandry.trough.TroughAnimalType;
import com.livestockhusbandry.trough.fold.TroughFold;
import com.livestockhusbandry.trough.fold.TroughFoldKey;
import com.livestockhusbandry.ai.largelivestock.LargeLivestockTroughReservations;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public final class PigTroughReservations {

    private static final TroughAnimalType ANIMAL_TYPE = TroughAnimalType.PIG;

    private PigTroughReservations() {
    }

    public static boolean register(
            Level level,
            UUID pigId,
            BlockPos pigPos,
            TroughFold fold
    ) {
        return LargeLivestockTroughReservations.register(
                level,
                ANIMAL_TYPE,
                pigId,
                pigPos,
                fold
        );
    }

    @Nullable
    public static TroughFoldKey getRegisteredFoldKey(Level level, UUID pigId) {
        return LargeLivestockTroughReservations.getRegisteredFoldKey(
                level,
                ANIMAL_TYPE,
                pigId
        );
    }

    @Nullable
    public static BlockPos getAssignedTroughBlock(Level level, UUID pigId) {
        return LargeLivestockTroughReservations.getAssignedTroughBlock(
                level,
                ANIMAL_TYPE,
                pigId
        );
    }

    @Nullable
    public static BlockPos getAssignedEatBlock(Level level, UUID pigId) {
        return LargeLivestockTroughReservations.getAssignedEatBlock(
                level,
                ANIMAL_TYPE,
                pigId
        );
    }

    public static int getRegisteredCount(Level level, TroughFoldKey foldKey) {
        return LargeLivestockTroughReservations.getRegisteredCount(
                level,
                ANIMAL_TYPE,
                foldKey
        );
    }

    public static void unregister(Level level, UUID pigId) {
        LargeLivestockTroughReservations.unregister(
                level,
                ANIMAL_TYPE,
                pigId
        );
    }

    public static void reassignIfAssignedTroughMissing(
            Level level,
            UUID pigId,
            TroughFold fold,
            BlockPos pigPos
    ) {
        LargeLivestockTroughReservations.reassignIfAssignedTroughMissing(
                level,
                ANIMAL_TYPE,
                pigId,
                fold,
                pigPos
        );
    }
}