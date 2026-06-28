package com.livestockhusbandry.ai.cow;

import com.livestockhusbandry.trough.fold.TroughFold;
import com.livestockhusbandry.trough.fold.TroughFoldKey;
import com.livestockhusbandry.trough.fold.TroughFoldUtil;
import com.livestockhusbandry.ai.largelivestock.LargeLivestockFoldRules;
import com.livestockhusbandry.ai.trough.TroughPendingBabyRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CowTroughBreedingManager {

    private static final int WHEAT_COST = 2;

    private CowTroughBreedingManager() {
    }

    public static boolean tryBreed(
            ServerLevel level,
            TroughFold fold
    ) {
        if (TroughFoldUtil.getTotalWheat(level, fold) < WHEAT_COST) {
            return false;
        }

        int breedingLimit = LargeLivestockFoldRules.breedingLimit(
                fold.troughCount()
        );

        int registeredCount = CowTroughReservations.getRegisteredCount(
                level,
                fold.key()
        );

        if (registeredCount >= breedingLimit) {
            return false;
        }

        List<Cow> candidates = findBreedingCandidates(level, fold);

        if (candidates.size() < 2) {
            return false;
        }

        Cow first = candidates.get(0);
        Cow second = candidates.get(1);

        if (!TroughFoldUtil.consumeWheat(level, fold, WHEAT_COST)) {
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
            Cow firstParent,
            Cow secondParent,
            Cow babyCow
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

        int registeredCount = CowTroughReservations.getRegisteredCount(
                level,
                fold.key()
        );

        if (registeredCount >= breedingLimit) {
            return;
        }

        CowTroughReservations.register(
                level,
                babyCow.getUUID(),
                babyCow.blockPosition(),
                fold
        );
    }

    private static List<Cow> findBreedingCandidates(
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

        List<Cow> cows = level.getEntitiesOfClass(
                Cow.class,
                searchBox,
                candidate -> isValidBreedingCandidate(level, fold, candidate)
        );

        List<Cow> sorted = new ArrayList<>(cows);

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
            Cow cow
    ) {
        if (!cow.isAlive()) {
            return false;
        }

        if (cow.isBaby()) {
            return false;
        }

        TroughFoldKey registeredFoldKey = CowTroughReservations.getRegisteredFoldKey(
                level,
                cow.getUUID()
        );

        if (!fold.key().equals(registeredFoldKey)) {
            return false;
        }

        return CowTroughUtil.isInsideTroughArea(cow, fold);
    }

}