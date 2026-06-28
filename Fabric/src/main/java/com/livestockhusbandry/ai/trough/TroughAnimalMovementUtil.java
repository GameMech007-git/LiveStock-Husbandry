package com.livestockhusbandry.ai.trough;

import com.livestockhusbandry.block.ModBlocks;
import com.livestockhusbandry.trough.fold.TroughFold;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.PathfinderMob;

public final class TroughAnimalMovementUtil {

    private static final double EAT_TOLERANCE_SQR = 0.72D;

    private TroughAnimalMovementUtil() {
    }

    public static void moveNearTrough(
            PathfinderMob animal,
            BlockPos eatPos,
            double speedModifier
    ) {
        animal.getNavigation().moveTo(
                eatPos.getX() + 0.5D,
                animal.getY(),
                eatPos.getZ() + 0.5D,
                speedModifier
        );
    }

    public static void lookAtTrough(
            PathfinderMob animal,
            BlockPos troughPos
    ) {
        animal.getLookControl().setLookAt(
                troughPos.getX() + 0.5D,
                troughPos.getY() + 0.4D,
                troughPos.getZ() + 0.5D
        );
    }

    public static boolean isNearTroughCenter(
            PathfinderMob animal,
            BlockPos troughPos,
            BlockPos eatPos
    ) {
        if (isStandingOnTrough(animal)) {
            return false;
        }

        if (isAtEatBlockOrOneBehind(animal.blockPosition(), troughPos, eatPos)) {
            return true;
        }

        if (isBesideBlock(animal.blockPosition(), troughPos)) {
            return true;
        }

        double dx = animal.getX() - (eatPos.getX() + 0.5D);
        double dz = animal.getZ() - (eatPos.getZ() + 0.5D);

        return dx * dx + dz * dz < EAT_TOLERANCE_SQR;
    }

    public static boolean isStandingOnTrough(PathfinderMob animal) {
        return animal.level().getBlockState(animal.blockPosition()).is(ModBlocks.TROUGH)
                || animal.level().getBlockState(animal.blockPosition().below()).is(ModBlocks.TROUGH);
    }

    public static BlockPos findFallbackEatBlock(
            PathfinderMob animal,
            BlockPos troughPos
    ) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = troughPos.relative(direction);

            if (!animal.level().getBlockState(candidate).is(ModBlocks.TROUGH)) {
                return candidate;
            }
        }

        return troughPos;
    }

    public static BlockPos findNearestOpenEatBlock(
            PathfinderMob animal,
            BlockPos troughPos
    ) {
        BlockPos bestPos = null;
        double bestDistance = Double.MAX_VALUE;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = troughPos.relative(direction);

            if (animal.level().getBlockState(candidate).is(ModBlocks.TROUGH)) {
                continue;
            }

            double distance = candidate.distSqr(animal.blockPosition());

            if (distance < bestDistance) {
                bestDistance = distance;
                bestPos = candidate.immutable();
            }
        }

        if (bestPos == null) {
            return troughPos;
        }

        return bestPos;
    }

    public static boolean isInsideTroughArea(
            PathfinderMob animal,
            TroughFold fold
    ) {
        return fold.contains(animal.blockPosition());
    }

    private static boolean isBesideBlock(BlockPos animalPos, BlockPos troughPos) {
        int dx = Math.abs(animalPos.getX() - troughPos.getX());
        int dz = Math.abs(animalPos.getZ() - troughPos.getZ());

        if (dx == 0 && dz == 0) {
            return false;
        }

        return dx <= 1 && dz <= 1;
    }

    private static boolean isAtEatBlockOrOneBehind(
            BlockPos animalPos,
            BlockPos troughPos,
            BlockPos eatPos
    ) {
        if (animalPos.equals(eatPos)) {
            return true;
        }

        int dx = Integer.compare(eatPos.getX() - troughPos.getX(), 0);
        int dz = Integer.compare(eatPos.getZ() - troughPos.getZ(), 0);

        BlockPos oneBehindEatPos = eatPos.offset(dx, 0, dz);

        return animalPos.equals(oneBehindEatPos);
    }
}