package com.livestockhusbandry.trough;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

public final class TroughFenceAreaUtil {

    private static final int MAX_SCAN_RADIUS = 24;

    private TroughFenceAreaUtil() {
    }

    public static Result scan(ServerLevel level, BlockPos centerPos) {
        BlockPos west = findFence(level, centerPos, Direction.WEST);
        BlockPos east = findFence(level, centerPos, Direction.EAST);
        BlockPos north = findFence(level, centerPos, Direction.NORTH);
        BlockPos south = findFence(level, centerPos, Direction.SOUTH);

        if (west == null || east == null || north == null || south == null) {
            return Result.none();
        }

        int minX = west.getX() + 1;
        int maxX = east.getX() - 1;
        int minZ = north.getZ() + 1;
        int maxZ = south.getZ() - 1;

        if (minX > maxX || minZ > maxZ) {
            return Result.none();
        }

        return new Result(
                true,
                minX,
                minZ,
                maxX,
                maxZ
        );
    }

    private static BlockPos findFence(ServerLevel level, BlockPos startPos, Direction direction) {
        for (int distance = 1; distance <= MAX_SCAN_RADIUS; distance++) {
            BlockPos checkPos = startPos.relative(direction, distance);

            if (isFenceBoundary(level, checkPos)) {
                return checkPos;
            }
        }

        return null;
    }

    private static boolean isFenceBoundary(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState aboveState = level.getBlockState(pos.above());
        BlockState belowState = level.getBlockState(pos.below());

        return isFenceLike(state)
                || isFenceLike(aboveState)
                || isFenceLike(belowState);
    }

    private static boolean isFenceLike(BlockState state) {
        return state.is(BlockTags.FENCES)
                || state.is(BlockTags.WOODEN_FENCES)
                || state.is(BlockTags.WALLS)
                || state.is(BlockTags.FENCE_GATES);
    }

    public record Result(
            boolean found,
            int minX,
            int minZ,
            int maxX,
            int maxZ
    ) {
        public static Result none() {
            return new Result(false, 0, 0, 0, 0);
        }
    }
}