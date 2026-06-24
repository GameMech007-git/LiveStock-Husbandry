package com.livestockhusbandry.entity.ai.sheep;

import com.livestockhusbandry.block.entity.TroughBlockEntity;
import com.livestockhusbandry.block.trough.TroughUtil;
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
            TroughUtil.TroughGroup group,
            TroughBlockEntity trough
    ) {
        if (trough.getWheatCount() < WHEAT_COST) {
            return false;
        }

        int registeredCount = SheepTroughReservations.getRegisteredCount(
                level,
                group.controllerPos()
        );

        if (registeredCount >= group.capacity()) {
            return false;
        }

        List<Sheep> candidates = findBreedingCandidates(level, group);

        if (candidates.size() < 2) {
            return false;
        }

        Sheep first = candidates.get(0);
        Sheep second = candidates.get(1);

        if (!trough.consumeWheat(WHEAT_COST)) {
            return false;
        }

        first.setInLove(null);
        second.setInLove(null);

        return true;
    }

    private static List<Sheep> findBreedingCandidates(
            ServerLevel level,
            TroughUtil.TroughGroup group
    ) {
        int searchRadius = Math.max(group.radius(), 8) + 4;

        AABB searchBox = new AABB(group.centerPos()).inflate(
                searchRadius,
                3.0D,
                searchRadius
        );

        List<Sheep> sheep = level.getEntitiesOfClass(
                Sheep.class,
                searchBox,
                candidate -> isValidBreedingCandidate(level, group, candidate)
        );

        List<Sheep> sorted = new ArrayList<>(sheep);

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

        if (!group.controllerPos().equals(
                SheepTroughReservations.getRegisteredTrough(level, sheep.getUUID())
        )) {
            return false;
        }

        return SheepTroughUtil.isInsideTroughArea(sheep, group);
    }
}