package com.livestockhusbandry.ai.cow;

import com.livestockhusbandry.trough.TroughAnimalType;
import com.livestockhusbandry.trough.fold.TroughFold;
import com.livestockhusbandry.trough.fold.TroughFoldKey;
import com.livestockhusbandry.ai.largelivestock.LargeLivestockTroughReservations;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public final class CowTroughReservations {

    private static final TroughAnimalType ANIMAL_TYPE = TroughAnimalType.COW;

    private CowTroughReservations() {
    }

    public static boolean register(
            Level level,
            UUID cowId,
            BlockPos cowPos,
            TroughFold fold
    ) {
        return LargeLivestockTroughReservations.register(
                level,
                ANIMAL_TYPE,
                cowId,
                cowPos,
                fold
        );
    }

    @Nullable
    public static TroughFoldKey getRegisteredFoldKey(Level level, UUID cowId) {
        return LargeLivestockTroughReservations.getRegisteredFoldKey(
                level,
                ANIMAL_TYPE,
                cowId
        );
    }

    @Nullable
    public static BlockPos getAssignedTroughBlock(Level level, UUID cowId) {
        return LargeLivestockTroughReservations.getAssignedTroughBlock(
                level,
                ANIMAL_TYPE,
                cowId
        );
    }

    @Nullable
    public static BlockPos getAssignedEatBlock(Level level, UUID cowId) {
        return LargeLivestockTroughReservations.getAssignedEatBlock(
                level,
                ANIMAL_TYPE,
                cowId
        );
    }

    public static int getRegisteredCount(Level level, TroughFoldKey foldKey) {
        return LargeLivestockTroughReservations.getRegisteredCount(
                level,
                ANIMAL_TYPE,
                foldKey
        );
    }

    public static void unregister(Level level, UUID cowId) {
        LargeLivestockTroughReservations.unregister(
                level,
                ANIMAL_TYPE,
                cowId
        );
    }

    public static void reassignIfAssignedTroughMissing(
            Level level,
            UUID cowId,
            TroughFold fold,
            BlockPos cowPos
    ) {
        LargeLivestockTroughReservations.reassignIfAssignedTroughMissing(
                level,
                ANIMAL_TYPE,
                cowId,
                fold,
                cowPos
        );
    }
}