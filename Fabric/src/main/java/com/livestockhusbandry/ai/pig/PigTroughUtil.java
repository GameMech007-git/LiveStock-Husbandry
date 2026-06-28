package com.livestockhusbandry.ai.pig;

import com.livestockhusbandry.trough.TroughAnimalType;
import com.livestockhusbandry.trough.fold.TroughFold;
import com.livestockhusbandry.trough.fold.TroughFoldKey;
import com.livestockhusbandry.trough.fold.TroughFoldUtil;
import com.livestockhusbandry.ai.largelivestock.LargeLivestockFoldRules;
import com.livestockhusbandry.ai.trough.TroughAnimalMovementUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.pig.Pig;
import org.jspecify.annotations.Nullable;

public final class PigTroughUtil {

    private PigTroughUtil() {
    }

    @Nullable
    public static TroughFold findNearbyAvailableFold(
            Pig pig,
            int searchRange
    ) {
        if (!(pig.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        TroughFold fold = TroughFoldUtil.findSavedFoldContaining(
                serverLevel,
                pig.blockPosition(),
                searchRange
        );

        if (fold == null) {
            return null;
        }

        if (!TroughFoldUtil.canRegister(fold, TroughAnimalType.PIG)) {
            return null;
        }

        int registeredCount = PigTroughReservations.getRegisteredCount(
                serverLevel,
                fold.key()
        );

        int pigLimit = LargeLivestockFoldRules.breedingLimit(
                fold.troughCount()
        );

        if (registeredCount >= pigLimit) {
            return null;
        }

        return fold;
    }

    @Nullable
    public static TroughFold getRegisteredFold(Pig pig) {
        if (!(pig.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        TroughFoldKey foldKey = PigTroughReservations.getRegisteredFoldKey(
                serverLevel,
                pig.getUUID()
        );

        if (foldKey == null) {
            return null;
        }

        TroughFold fold = TroughFoldUtil.getSavedFoldByKey(serverLevel, foldKey);

        if (fold == null) {
            PigTroughReservations.unregister(serverLevel, pig.getUUID());
            return null;
        }

        PigTroughReservations.reassignIfAssignedTroughMissing(
                serverLevel,
                pig.getUUID(),
                fold,
                pig.blockPosition()
        );

        return fold;
    }

    public static boolean isInsideTroughArea(Pig pig, TroughFold fold) {
        return TroughAnimalMovementUtil.isInsideTroughArea(pig, fold);
    }

    public static void moveNearTrough(Pig pig, TroughFold fold, double speedModifier) {
        TroughAnimalMovementUtil.moveNearTrough(
                pig,
                getEatBlockPos(pig, fold),
                speedModifier
        );
    }

    public static void lookAtTrough(Pig pig, TroughFold fold) {
        TroughAnimalMovementUtil.lookAtTrough(
                pig,
                getAssignedOrCenterTroughPos(pig, fold)
        );
    }

    public static boolean isNearTroughCenter(Pig pig, TroughFold fold) {
        return TroughAnimalMovementUtil.isNearTroughCenter(
                pig,
                getAssignedOrCenterTroughPos(pig, fold),
                getEatBlockPos(pig, fold)
        );
    }

    public static BlockPos getEatBlockPos(Pig pig, TroughFold fold) {
        BlockPos assignedEatPos = PigTroughReservations.getAssignedEatBlock(
                pig.level(),
                pig.getUUID()
        );

        if (assignedEatPos != null) {
            return assignedEatPos;
        }

        return TroughAnimalMovementUtil.findFallbackEatBlock(
                pig,
                getAssignedOrCenterTroughPos(pig, fold)
        );
    }

    private static BlockPos getAssignedOrCenterTroughPos(Pig pig, TroughFold fold) {
        BlockPos assignedTroughPos = PigTroughReservations.getAssignedTroughBlock(
                pig.level(),
                pig.getUUID()
        );

        if (assignedTroughPos != null) {
            return assignedTroughPos;
        }

        return fold.centerPos();
    }
}