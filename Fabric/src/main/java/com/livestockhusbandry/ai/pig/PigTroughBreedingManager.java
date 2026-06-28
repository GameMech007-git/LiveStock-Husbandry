package com.livestockhusbandry.ai.pig;

import com.livestockhusbandry.trough.fold.TroughFold;
import com.livestockhusbandry.trough.fold.TroughFoldKey;
import com.livestockhusbandry.trough.fold.TroughFoldUtil;
import com.livestockhusbandry.ai.largelivestock.LargeLivestockFoldRules;
import com.livestockhusbandry.ai.trough.TroughPendingBabyRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PigTroughBreedingManager {

    private static final int FOOD_COST = 2;

    private PigTroughBreedingManager() {
    }

    public static boolean tryBreed(
            ServerLevel level,
            TroughFold fold
    ) {
        if (TroughFoldUtil.getTotalWheat(level, fold) < FOOD_COST) {
            return false;
        }

        int breedingLimit = LargeLivestockFoldRules.breedingLimit(
                fold.troughCount()
        );

        int registeredCount = PigTroughReservations.getRegisteredCount(
                level,
                fold.key()
        );

        if (registeredCount >= breedingLimit) {
            return false;
        }

        List<Pig> candidates = findBreedingCandidates(level, fold);

        if (candidates.size() < 2) {
            return false;
        }

        Pig first = candidates.get(0);
        Pig second = candidates.get(1);

        if (!TroughFoldUtil.consumeWheat(level, fold, FOOD_COST)) {
            return false;
        }

        TroughPendingBabyRegistry.add(
                level.dimension(),
                first,
                second,
                fold.key(),
                level.getGameTime() + 20L
        );

        first.spawnChildFromBreeding(level, second);

        TroughPendingBabyRegistry.remove(
                level.dimension(),
                first,
                second
        );

        return true;
    }

    public static void registerTroughBredBabyIfPending(
            ServerLevel level,
            Pig firstParent,
            Pig secondParent,
            Pig babyPig
    ) {
        TroughPendingBabyRegistry.PendingBabyRegistration pending =
                TroughPendingBabyRegistry.get(
                        level.dimension(),
                        firstParent,
                        secondParent
                );

        if (pending == null) {
            return;
        }

        if (!TroughPendingBabyRegistry.isValid(
                pending,
                level.dimension(),
                level.getGameTime()
        )) {
            TroughPendingBabyRegistry.remove(
                    level.dimension(),
                    firstParent,
                    secondParent
            );
            return;
        }

        TroughFold fold = TroughFoldUtil.getSavedFoldByKey(
                level,
                pending.foldKey()
        );

        if (fold == null) {
            return;
        }

        int breedingLimit = LargeLivestockFoldRules.breedingLimit(
                fold.troughCount()
        );

        int registeredCount = PigTroughReservations.getRegisteredCount(
                level,
                fold.key()
        );

        if (registeredCount >= breedingLimit) {
            return;
        }

        PigTroughReservations.register(
                level,
                babyPig.getUUID(),
                babyPig.blockPosition(),
                fold
        );
    }

    private static List<Pig> findBreedingCandidates(
            ServerLevel level,
            TroughFold fold
    ) {
        int searchRadius = Math.max(
                Math.max(fold.maxX() - fold.minX(), fold.maxZ() - fold.minZ()),
                8
        ) + 4;

        BlockPos centerPos = fold.centerPos();

        AABB searchBox = new AABB(centerPos).inflate(
                searchRadius,
                3.0D,
                searchRadius
        );

        List<Pig> pigs = level.getEntitiesOfClass(
                Pig.class,
                searchBox,
                candidate -> isValidBreedingCandidate(level, fold, candidate)
        );

        List<Pig> sorted = new ArrayList<>(pigs);

        sorted.sort(
                Comparator.comparingDouble(
                        candidate -> candidate.distanceToSqr(
                                centerPos.getX() + 0.5D,
                                centerPos.getY() + 0.5D,
                                centerPos.getZ() + 0.5D
                        )
                )
        );

        return sorted;
    }

    private static boolean isValidBreedingCandidate(
            ServerLevel level,
            TroughFold fold,
            Pig pig
    ) {
        if (!pig.isAlive()) {
            return false;
        }

        if (pig.isBaby()) {
            return false;
        }

        TroughFoldKey registeredFoldKey = PigTroughReservations.getRegisteredFoldKey(
                level,
                pig.getUUID()
        );

        if (!fold.key().equals(registeredFoldKey)) {
            return false;
        }

        return PigTroughUtil.isInsideTroughArea(pig, fold);
    }
}