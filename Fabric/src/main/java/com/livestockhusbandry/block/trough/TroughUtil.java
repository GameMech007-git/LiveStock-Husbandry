package com.livestockhusbandry.block.trough;

import com.livestockhusbandry.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

public final class TroughUtil {

    public static final int MAX_GROUP_SIZE = 4;

    private TroughUtil() {
    }

    public static TroughGroup resolveGroup(ServerLevel level, BlockPos originPos) {
        List<BlockPos> line = collectBestStraightLine(level, originPos);

        if (line.isEmpty()) {
            line = List.of(originPos.immutable());
        }

        Direction facing = getFacingForLine(line);
        line = sortForFacing(line, facing);

        int indexInLine = findIndex(line, originPos);
        int batchStart = (indexInLine / MAX_GROUP_SIZE) * MAX_GROUP_SIZE;
        int batchEnd = Math.min(batchStart + MAX_GROUP_SIZE, line.size());

        List<BlockPos> batch = new ArrayList<>(line.subList(batchStart, batchEnd));

        BlockPos controllerPos = batch.stream()
                .min(TroughUtil::compareBlockPos)
                .orElse(originPos);

        BlockPos centerPos = calculateCenter(batch);
        int size = batch.size();

        return new TroughGroup(
                controllerPos,
                batch,
                centerPos,
                size,
                getCapacityForSize(size),
                getRadiusForSize(size)
        );
    }

    public static boolean isTrough(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.TROUGH);
    }

    public static int getCapacityForSize(int size) {
        return Math.max(1, Math.min(size, MAX_GROUP_SIZE));
    }

    public static int getRadiusForSize(int size) {
        return Math.min(size * 2, 8);
    }

    private static List<BlockPos> collectBestStraightLine(ServerLevel level, BlockPos originPos) {
        List<BlockPos> xLine = collectFullLine(level, originPos, Axis.X);
        List<BlockPos> zLine = collectFullLine(level, originPos, Axis.Z);

        if (xLine.size() > zLine.size()) {
            return xLine;
        }

        if (zLine.size() > xLine.size()) {
            return zLine;
        }

        return xLine;
    }

    private static List<BlockPos> collectFullLine(ServerLevel level, BlockPos originPos, Axis axis) {
        List<BlockPos> positions = new ArrayList<>();
        positions.add(originPos.immutable());

        collectDirection(level, originPos, axis, 1, positions);
        collectDirection(level, originPos, axis, -1, positions);

        return positions.stream()
                .distinct()
                .sorted(TroughUtil::compareBlockPos)
                .toList();
    }

    private static void collectDirection(
            ServerLevel level,
            BlockPos originPos,
            Axis axis,
            int direction,
            List<BlockPos> positions
    ) {
        for (int distance = 1; distance <= 32; distance++) {
            BlockPos checkPos;

            if (axis == Axis.X) {
                checkPos = originPos.offset(direction * distance, 0, 0);
            } else {
                checkPos = originPos.offset(0, 0, direction * distance);
            }

            if (!isTrough(level, checkPos)) {
                return;
            }

            positions.add(checkPos.immutable());
        }
    }

    private static Direction getFacingForLine(List<BlockPos> line) {
        if (line.size() < 2) {
            return Direction.NORTH;
        }

        BlockPos first = line.get(0);
        BlockPos last = line.get(line.size() - 1);

        if (first.getX() != last.getX()) {
            return Direction.EAST;
        }

        return Direction.SOUTH;
    }

    private static List<BlockPos> sortForFacing(List<BlockPos> positions, Direction facing) {
        if (facing.getAxis() == Direction.Axis.X) {
            return positions.stream()
                    .sorted((first, second) -> Integer.compare(first.getX(), second.getX()))
                    .toList();
        }

        return positions.stream()
                .sorted((first, second) -> Integer.compare(first.getZ(), second.getZ()))
                .toList();
    }

    private static int findIndex(List<BlockPos> line, BlockPos originPos) {
        for (int index = 0; index < line.size(); index++) {
            if (line.get(index).equals(originPos)) {
                return index;
            }
        }

        return 0;
    }

    private static BlockPos calculateCenter(List<BlockPos> positions) {
        if (positions.isEmpty()) {
            return BlockPos.ZERO;
        }

        int x = 0;
        int y = 0;
        int z = 0;

        for (BlockPos pos : positions) {
            x += pos.getX();
            y += pos.getY();
            z += pos.getZ();
        }

        int count = positions.size();

        return new BlockPos(
                x / count,
                y / count,
                z / count
        );
    }

    private static int compareBlockPos(BlockPos first, BlockPos second) {
        if (first.getY() != second.getY()) {
            return Integer.compare(first.getY(), second.getY());
        }

        if (first.getX() != second.getX()) {
            return Integer.compare(first.getX(), second.getX());
        }

        return Integer.compare(first.getZ(), second.getZ());
    }

    private enum Axis {
        X,
        Z
    }

    public record TroughGroup(
            BlockPos controllerPos,
            List<BlockPos> troughPositions,
            BlockPos centerPos,
            int size,
            int capacity,
            int radius
    ) {
    }
}