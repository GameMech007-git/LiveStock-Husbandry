package com.livestockhusbandry.ai.trough;

import com.livestockhusbandry.trough.fold.TroughFold;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public final class TroughAssignmentUtil {

    private TroughAssignmentUtil() {
    }

    @Nullable
    public static BlockPos findNearestTroughBlock(
            BlockPos animalPos,
            TroughFold fold
    ) {
        BlockPos bestPos = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos troughPos : fold.troughPositions()) {
            double distance = troughPos.distSqr(animalPos);

            if (distance < bestDistance) {
                bestDistance = distance;
                bestPos = troughPos.immutable();
            }
        }

        return bestPos;
    }

    public static BlockPos findBestEatBlock(
            BlockPos animalPos,
            BlockPos troughPos,
            TroughFold fold
    ) {
        BlockPos bestPos = null;
        double bestDistance = Double.MAX_VALUE;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = troughPos.relative(direction);

            if (isTroughInsideFold(candidate, fold)) {
                continue;
            }

            double distance = candidate.distSqr(animalPos);

            if (distance < bestDistance) {
                bestDistance = distance;
                bestPos = candidate.immutable();
            }
        }

        if (bestPos == null) {
            return troughPos.immutable();
        }

        return bestPos;
    }

    public static boolean isTroughInsideFold(BlockPos pos, TroughFold fold) {
        for (BlockPos foldTroughPos : fold.troughPositions()) {
            if (foldTroughPos.equals(pos)) {
                return true;
            }
        }

        return false;
    }
}