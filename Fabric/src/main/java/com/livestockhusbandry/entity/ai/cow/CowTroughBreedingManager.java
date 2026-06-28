package com.livestockhusbandry.entity.ai.cow;

import com.livestockhusbandry.block.entity.TroughBlockEntity;
import com.livestockhusbandry.block.trough.TroughUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CowTroughBreedingManager {

    private static final int WHEAT_COST = 2;

    private static final Map<PendingPairKey, PendingBabyRegistration> PENDING_BABIES =
            new HashMap<>();

    private CowTroughBreedingManager() {
    }

    public static boolean tryBreed(
            ServerLevel level,
            TroughUtil.TroughGroup group,
            TroughBlockEntity trough
    ) {
        if (trough.getWheatCount() < WHEAT_COST) {
            return false;
        }

        int breedingLimit = getBreedingLimit(group);

        int registeredCount = CowTroughReservations.getRegisteredCount(
                level,
                group.controllerPos()
        );

        if (registeredCount >= breedingLimit) {
            return false;
        }

        List<Cow> candidates = findBreedingCandidates(level, group);

        if (candidates.size() < 2) {
            return false;
        }

        Cow first = candidates.get(0);
        Cow second = candidates.get(1);

        if (!trough.consumeWheat(WHEAT_COST)) {
            return false;
        }

        PendingPairKey pairKey = PendingPairKey.of(
                level.dimension(),
                first.getUUID(),
                second.getUUID()
        );

        PENDING_BABIES.put(
                pairKey,
                new PendingBabyRegistration(
                        level.dimension(),
                        group.controllerPos().immutable(),
                        level.getGameTime() + 20L
                )
        );

        first.spawnChildFromBreeding(level, second);

        PENDING_BABIES.remove(pairKey);

        return true;
    }

    public static void registerTroughBredBabyIfPending(
            ServerLevel level,
            Cow firstParent,
            Cow secondParent,
            Cow babyCow
    ) {
        PendingPairKey pairKey = PendingPairKey.of(
                level.dimension(),
                firstParent.getUUID(),
                secondParent.getUUID()
        );

        PendingBabyRegistration pending = PENDING_BABIES.get(pairKey);

        if (pending == null) {
            return;
        }

        if (!pending.dimension().equals(level.dimension())) {
            return;
        }

        if (level.getGameTime() > pending.expireGameTime()) {
            PENDING_BABIES.remove(pairKey);
            return;
        }

        TroughUtil.TroughGroup group = TroughUtil.resolveGroup(
                level,
                pending.controllerPos()
        );

        int breedingLimit = getBreedingLimit(group);

        int registeredCount = CowTroughReservations.getRegisteredCount(
                level,
                group.controllerPos()
        );

        if (registeredCount >= breedingLimit) {
            return;
        }

        CowTroughReservations.register(
                level,
                babyCow.getUUID(),
                babyCow.blockPosition(),
                group
        );
    }

    private static int getBreedingLimit(TroughUtil.TroughGroup group) {
        return group.capacity() + 2;
    }

    private static List<Cow> findBreedingCandidates(
            ServerLevel level,
            TroughUtil.TroughGroup group
    ) {
        int searchRadius = Math.max(group.radius(), 8) + 4;

        AABB searchBox = new AABB(group.centerPos()).inflate(
                searchRadius,
                3.0D,
                searchRadius
        );

        List<Cow> cows = level.getEntitiesOfClass(
                Cow.class,
                searchBox,
                candidate -> isValidBreedingCandidate(level, group, candidate)
        );

        List<Cow> sorted = new ArrayList<>(cows);

        sorted.sort(
                Comparator.comparingDouble(
                        candidate -> candidate.distanceToSqr(
                                group.centerPos().getX() + 0.5D,
                                group.centerPos().getY() + 0.5D,
                                group.centerPos().getZ() + 0.5D
                        )
                )
        );

        return sorted;
    }

    private static boolean isValidBreedingCandidate(
            ServerLevel level,
            TroughUtil.TroughGroup group,
            Cow cow
    ) {
        if (!cow.isAlive()) {
            return false;
        }

        if (cow.isBaby()) {
            return false;
        }

        if (!group.controllerPos().equals(
                CowTroughReservations.getRegisteredTrough(level, cow.getUUID())
        )) {
            return false;
        }

        return CowTroughUtil.isInsideTroughArea(cow, group);
    }

    private record PendingBabyRegistration(
            ResourceKey<Level> dimension,
            BlockPos controllerPos,
            long expireGameTime
    ) {
    }

    private record PendingPairKey(
            ResourceKey<Level> dimension,
            UUID firstParent,
            UUID secondParent
    ) {
        private static PendingPairKey of(
                ResourceKey<Level> dimension,
                UUID firstParent,
                UUID secondParent
        ) {
            if (firstParent.compareTo(secondParent) <= 0) {
                return new PendingPairKey(dimension, firstParent, secondParent);
            }

            return new PendingPairKey(dimension, secondParent, firstParent);
        }
    }
}