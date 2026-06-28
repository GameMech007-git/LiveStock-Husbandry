package com.livestockhusbandry.ai.cow;

import com.livestockhusbandry.trough.TroughAnimalType;
import com.livestockhusbandry.trough.fold.TroughFold;
import com.livestockhusbandry.trough.fold.TroughFoldKey;
import com.livestockhusbandry.trough.fold.TroughFoldUtil;
import com.livestockhusbandry.ai.largelivestock.LargeLivestockFoldRules;
import com.livestockhusbandry.ai.trough.TroughAnimalMovementUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.cow.Cow;
import org.jspecify.annotations.Nullable;

public final class CowTroughUtil {

    private CowTroughUtil() {
    }

    @Nullable
    public static TroughFold findNearbyAvailableFold(
            Cow cow,
            int searchRange
    ) {
        if (!(cow.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        TroughFold fold = TroughFoldUtil.findSavedFoldContaining(
                serverLevel,
                cow.blockPosition(),
                searchRange
        );

        if (fold == null) {
            return null;
        }

        if (!TroughFoldUtil.canRegister(fold, TroughAnimalType.COW)) {
            return null;
        }

        int registeredCount = CowTroughReservations.getRegisteredCount(
                serverLevel,
                fold.key()
        );

        int cowLimit = LargeLivestockFoldRules.breedingLimit(
                fold.troughCount()
        );

        if (registeredCount >= cowLimit) {
            return null;
        }

        return fold;
    }

    @Nullable
    public static TroughFold getRegisteredFold(Cow cow) {
        if (!(cow.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        TroughFoldKey foldKey = CowTroughReservations.getRegisteredFoldKey(
                serverLevel,
                cow.getUUID()
        );

        if (foldKey == null) {
            return null;
        }

        TroughFold fold = TroughFoldUtil.getSavedFoldByKey(serverLevel, foldKey);

        if (fold == null) {
            CowTroughReservations.unregister(serverLevel, cow.getUUID());
            return null;
        }

        CowTroughReservations.reassignIfAssignedTroughMissing(
                serverLevel,
                cow.getUUID(),
                fold,
                cow.blockPosition()
        );

        return fold;
    }

    public static boolean isInsideTroughArea(Cow cow, TroughFold fold) {
        return TroughAnimalMovementUtil.isInsideTroughArea(cow, fold);
    }

    public static void moveNearTrough(Cow cow, TroughFold fold, double speedModifier) {
        TroughAnimalMovementUtil.moveNearTrough(
                cow,
                getEatBlockPos(cow, fold),
                speedModifier
        );
    }

    public static void lookAtTrough(Cow cow, TroughFold fold) {
        TroughAnimalMovementUtil.lookAtTrough(
                cow,
                getAssignedOrCenterTroughPos(cow, fold)
        );
    }

    public static boolean isNearTroughCenter(Cow cow, TroughFold fold) {
        return TroughAnimalMovementUtil.isNearTroughCenter(
                cow,
                getAssignedOrCenterTroughPos(cow, fold),
                getEatBlockPos(cow, fold)
        );
    }

    public static BlockPos getEatBlockPos(Cow cow, TroughFold fold) {
        BlockPos assignedEatPos = CowTroughReservations.getAssignedEatBlock(
                cow.level(),
                cow.getUUID()
        );

        if (assignedEatPos != null) {
            return assignedEatPos;
        }

        BlockPos troughPos = getAssignedOrCenterTroughPos(cow, fold);

        return TroughAnimalMovementUtil.findFallbackEatBlock(cow, troughPos);
    }

    private static BlockPos getAssignedOrCenterTroughPos(Cow cow, TroughFold fold) {
        BlockPos assignedTroughPos = CowTroughReservations.getAssignedTroughBlock(
                cow.level(),
                cow.getUUID()
        );

        if (assignedTroughPos != null) {
            return assignedTroughPos;
        }

        return fold.centerPos();
    }

}