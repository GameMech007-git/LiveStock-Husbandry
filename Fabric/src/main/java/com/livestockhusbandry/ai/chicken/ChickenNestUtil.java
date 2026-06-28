package com.livestockhusbandry.ai.chicken;

import com.livestockhusbandry.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class ChickenNestUtil {

    private ChickenNestUtil() {
    }

    public static long getDayTime(Chicken chicken) {
        return Math.floorMod(chicken.level().getOverworldClockTime(), 24000L);
    }

    public static boolean isChickenRestTime(Chicken chicken) {
        long dayTime = getDayTime(chicken);
        return dayTime >= 13000L && dayTime <= 23000L;
    }

    public static boolean isChickenWorkTime(Chicken chicken) {
        return !isChickenRestTime(chicken);
    }

    public static boolean isEarlyMorningLayTime(Chicken chicken) {
        long dayTime = getDayTime(chicken);
        return dayTime >= 22800L && dayTime <= 23000L;
    }

    @Nullable
    public static BlockPos findAvailableNest(Chicken chicken, int searchRange) {
        Level level = chicken.level();
        BlockPos chickenPos = chicken.blockPosition();

        BlockPos bestPos = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                chickenPos.offset(-searchRange, -2, -searchRange),
                chickenPos.offset(searchRange, 2, searchRange)
        )) {
            if (!level.getBlockState(pos).is(ModBlocks.CHICKEN_NEST)) {
                continue;
            }

            if (ChickenNestReservations.isReservedByOther(
                    level,
                    pos,
                    chicken.getUUID()
            )) {
                continue;
            }

            double distance = pos.distSqr(chickenPos);

            if (distance < bestDistance) {
                bestDistance = distance;
                bestPos = pos.immutable();
            }
        }

        return bestPos;
    }

    public static boolean isOnNestCenter(Chicken chicken, BlockPos nestPos) {
        double centerX = nestPos.getX() + 0.5D;
        double centerZ = nestPos.getZ() + 0.5D;

        double dx = chicken.getX() - centerX;
        double dz = chicken.getZ() - centerZ;

        return dx * dx + dz * dz < 0.04D;
    }

    public static void moveToNestCenter(Chicken chicken, BlockPos nestPos, double speedModifier) {
        chicken.getNavigation().moveTo(
                nestPos.getX() + 0.5D,
                nestPos.getY() + 0.1D,
                nestPos.getZ() + 0.5D,
                speedModifier
        );
    }

    public static void lookAtNest(Chicken chicken, BlockPos nestPos) {
        chicken.getLookControl().setLookAt(
                nestPos.getX() + 0.5D,
                nestPos.getY() + 0.4D,
                nestPos.getZ() + 0.5D
        );
    }

    public static void stopHorizontalMovement(Chicken chicken) {
        chicken.setDeltaMovement(
                chicken.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D)
        );
    }
}