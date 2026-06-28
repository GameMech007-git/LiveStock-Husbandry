package com.livestockhusbandry.ai.sheep;

import com.livestockhusbandry.block.ModBlocks;
import com.livestockhusbandry.trough.TroughAnimalType;
import com.livestockhusbandry.trough.fold.TroughFold;
import com.livestockhusbandry.trough.fold.TroughFoldKey;
import com.livestockhusbandry.trough.fold.TroughFoldUtil;
import com.livestockhusbandry.ai.trough.TroughAnimalMovementUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.sheep.Sheep;
import org.jspecify.annotations.Nullable;

public final class SheepTroughUtil {

    private SheepTroughUtil() {
    }

    @Nullable
    public static TroughFold findNearbyAvailableFold(
            Sheep sheep,
            int searchRange
    ) {
        if (!(sheep.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        TroughFold fold = TroughFoldUtil.findSavedFoldContaining(
                serverLevel,
                sheep.blockPosition(),
                searchRange
        );

        if (fold == null) {
            return null;
        }

        if (!TroughFoldUtil.canRegister(fold, TroughAnimalType.SHEEP)) {
            return null;
        }

        int registeredCount = SheepTroughReservations.getRegisteredCount(
                serverLevel,
                fold.key()
        );

        if (registeredCount >= fold.sheepLimit()) {
            return null;
        }

        return fold;
    }

    @Nullable
    public static TroughFold getRegisteredFold(Sheep sheep) {
        if (!(sheep.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        TroughFoldKey foldKey = SheepTroughReservations.getRegisteredFoldKey(
                serverLevel,
                sheep.getUUID()
        );

        if (foldKey == null) {
            return null;
        }

        TroughFold fold = TroughFoldUtil.getSavedFoldByKey(serverLevel, foldKey);

        if (fold == null) {
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

        return fold;
    }

    public static boolean isInsideTroughArea(Sheep sheep, TroughFold fold) {
        return TroughAnimalMovementUtil.isInsideTroughArea(sheep, fold);
    }

    public static void moveNearTrough(Sheep sheep, TroughFold fold, double speedModifier) {
        TroughAnimalMovementUtil.moveNearTrough(
                sheep,
                getEatBlockPos(sheep, fold),
                speedModifier
        );
    }

    public static void lookAtTrough(Sheep sheep, TroughFold fold) {
        TroughAnimalMovementUtil.lookAtTrough(
                sheep,
                getAssignedOrCenterTroughPos(sheep, fold)
        );
    }

    public static boolean isNearTroughCenter(Sheep sheep, TroughFold fold) {
        return TroughAnimalMovementUtil.isNearTroughCenter(
                sheep,
                getAssignedOrCenterTroughPos(sheep, fold),
                getEatBlockPos(sheep, fold)
        );
    }

    public static BlockPos getEatBlockPos(Sheep sheep, TroughFold fold) {
        BlockPos assignedEatPos = SheepTroughReservations.getAssignedEatBlock(
                sheep.level(),
                sheep.getUUID()
        );

        if (assignedEatPos != null) {
            return assignedEatPos;
        }

        BlockPos troughPos = getAssignedOrCenterTroughPos(sheep, fold);

        return TroughAnimalMovementUtil.findFallbackEatBlock(sheep, troughPos);
    }

    public static void playEatAnimation(Sheep sheep) {
        sheep.level().broadcastEntityEvent(sheep, (byte) 10);
    }

    private static BlockPos getAssignedOrCenterTroughPos(Sheep sheep, TroughFold fold) {
        BlockPos assignedTroughPos = SheepTroughReservations.getAssignedTroughBlock(
                sheep.level(),
                sheep.getUUID()
        );

        if (assignedTroughPos != null) {
            return assignedTroughPos;
        }

        return fold.centerPos();
    }

    public static void reassignEatBlockNearCurrentPosition(Sheep sheep, TroughFold fold) {
        BlockPos troughPos = getAssignedOrCenterTroughPos(sheep, fold);

        BlockPos bestPos = TroughAnimalMovementUtil.findNearestOpenEatBlock(
                sheep,
                troughPos
        );

        SheepTroughReservations.updateAssignedEatBlock(
                sheep.level(),
                sheep.getUUID(),
                bestPos
        );
    }

    public static boolean consumeWheatForWoolGrowth(Sheep sheep, TroughFold fold) {
        if (!(sheep.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        return TroughFoldUtil.consumeWheat(serverLevel, fold, 1);
    }

    public static boolean hasWheatForWoolGrowth(Sheep sheep, TroughFold fold) {
        if (!(sheep.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        return TroughFoldUtil.getTotalWheat(serverLevel, fold) >= 1;
    }

}