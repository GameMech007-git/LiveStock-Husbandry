package com.livestockhusbandry.ai.sheep;

import com.livestockhusbandry.trough.fold.TroughFold;
import com.livestockhusbandry.trough.fold.TroughFoldKey;
import com.livestockhusbandry.trough.fold.TroughFoldUtil;
import com.livestockhusbandry.ai.trough.TroughPendingBabyRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SheepTroughBreedingManager {

    private static final int WHEAT_COST = 2;

    private SheepTroughBreedingManager() {
    }

    public static boolean tryBreed(
            ServerLevel level,
            TroughFold fold
    ) {
        if (TroughFoldUtil.getTotalWheat(level, fold) < WHEAT_COST) {
            return false;
        }

        int registeredCount = SheepTroughReservations.getRegisteredCount(
                level,
                fold.key()
        );

        if (registeredCount >= fold.sheepLimit()) {
            return false;
        }

        List<Sheep> candidates = findBreedingCandidates(level, fold);

        if (candidates.size() < 2) {
            return false;
        }

        Sheep first = candidates.get(0);
        Sheep second = candidates.get(1);

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
            Sheep firstParent,
            Sheep secondParent,
            Sheep babySheep
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

        int registeredCount = SheepTroughReservations.getRegisteredCount(
                level,
                fold.key()
        );

        if (registeredCount >= fold.sheepLimit()) {
            return;
        }

        SheepTroughReservations.register(
                level,
                babySheep.getUUID(),
                babySheep.blockPosition(),
                fold
        );
    }

    private static List<Sheep> findBreedingCandidates(
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

        List<Sheep> sheep = level.getEntitiesOfClass(
                Sheep.class,
                searchBox,
                candidate -> isValidBreedingCandidate(level, fold, candidate)
        );

        List<Sheep> sorted = new ArrayList<>(sheep);

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
            Sheep sheep
    ) {
        if (!sheep.isAlive()) {
            return false;
        }

        if (sheep.isBaby()) {
            return false;
        }

        if (sheep.isInLove()) {
            return false;
        }

        TroughFoldKey registeredFoldKey = SheepTroughReservations.getRegisteredFoldKey(
                level,
                sheep.getUUID()
        );

        if (!fold.key().equals(registeredFoldKey)) {
            return false;
        }

        return SheepTroughUtil.isInsideTroughArea(sheep, fold);
    }

}