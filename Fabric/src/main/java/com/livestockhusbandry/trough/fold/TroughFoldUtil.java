package com.livestockhusbandry.trough.fold;

import com.livestockhusbandry.block.ModBlocks;
import com.livestockhusbandry.entity.TroughBlockEntity;
import com.livestockhusbandry.trough.TroughAnimalType;
import com.livestockhusbandry.trough.TroughFenceAreaUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class TroughFoldUtil {

    private TroughFoldUtil() {
    }

    @Nullable
    public static TroughFold scanFromTrough(ServerLevel level, BlockPos troughPos) {
        TroughFenceAreaUtil.Result result = TroughFenceAreaUtil.scan(level, troughPos);

        if (!result.found()) {
            return null;
        }

        List<BlockPos> troughPositions = findTroughsInsideArea(
                level,
                result.minX(),
                result.minZ(),
                result.maxX(),
                result.maxZ(),
                troughPos.getY()
        );

        if (troughPositions.isEmpty()) {
            return null;
        }

        TroughAnimalType animalType = findFoldAnimalType(level, troughPositions);

        return new TroughFold(
                level.dimension(),
                result.minX(),
                result.minZ(),
                result.maxX(),
                result.maxZ(),
                troughPositions,
                animalType
        );
    }

    @Nullable
    public static TroughFold getSavedFoldFromTrough(ServerLevel level, BlockPos troughPos) {
        BlockEntity blockEntity = level.getBlockEntity(troughPos);

        if (!(blockEntity instanceof TroughBlockEntity trough)) {
            return null;
        }

        if (!trough.hasFenceArea()) {
            return null;
        }

        List<BlockPos> troughPositions = findTroughsInsideArea(
                level,
                trough.getFenceMinX(),
                trough.getFenceMinZ(),
                trough.getFenceMaxX(),
                trough.getFenceMaxZ(),
                troughPos.getY()
        );

        if (troughPositions.isEmpty()) {
            return null;
        }

        TroughAnimalType animalType = findFoldAnimalType(level, troughPositions);

        return new TroughFold(
                level.dimension(),
                trough.getFenceMinX(),
                trough.getFenceMinZ(),
                trough.getFenceMaxX(),
                trough.getFenceMaxZ(),
                troughPositions,
                animalType
        );
    }

    @Nullable
    public static TroughFold findSavedFoldContaining(ServerLevel level, BlockPos animalPos, int searchRange) {
        TroughFold bestFold = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                animalPos.offset(-searchRange, -2, -searchRange),
                animalPos.offset(searchRange, 2, searchRange)
        )) {
            if (!level.getBlockState(pos).is(ModBlocks.TROUGH)) {
                continue;
            }

            TroughFold fold = getSavedFoldFromTrough(level, pos);

            if (fold == null) {
                continue;
            }

            if (!fold.contains(animalPos)) {
                continue;
            }

            double distance = pos.distSqr(animalPos);

            if (distance < bestDistance) {
                bestDistance = distance;
                bestFold = fold;
            }
        }

        return bestFold;
    }

    public static boolean canRegister(TroughFold fold, TroughAnimalType type) {
        return fold.animalType() == TroughAnimalType.EMPTY
                || fold.animalType() == type;
    }

    public static void lockFoldAnimalType(
            ServerLevel level,
            TroughFold fold,
            TroughAnimalType animalType
    ) {
        if (animalType == TroughAnimalType.EMPTY) {
            return;
        }

        boolean lockedAnyTrough = false;

        for (BlockPos troughPos : fold.troughPositions()) {
            BlockEntity blockEntity = level.getBlockEntity(troughPos);

            if (blockEntity instanceof TroughBlockEntity trough
                    && trough.getAnimalType() == TroughAnimalType.EMPTY) {
                trough.lockAnimalType(animalType);
                lockedAnyTrough = true;
            }
        }

        if (lockedAnyTrough) {
            ejectWrongFoodForAnimalType(level, fold, animalType);
        }
    }

    public static int getTotalWheat(ServerLevel level, TroughFold fold) {
        int total = 0;

        for (BlockPos troughPos : fold.troughPositions()) {
            BlockEntity blockEntity = level.getBlockEntity(troughPos);

            if (blockEntity instanceof TroughBlockEntity trough) {
                total += trough.getWheatCount();
            }
        }

        return total;
    }

    public static boolean consumeWheat(ServerLevel level, TroughFold fold, int amount) {
        if (amount <= 0) {
            return true;
        }

        if (getTotalWheat(level, fold) < amount) {
            return false;
        }

        int remaining = amount;

        List<BlockPos> troughs = new ArrayList<>(fold.troughPositions());
        troughs.sort(TroughFoldUtil::compareBlockPos);

        for (BlockPos troughPos : troughs) {
            if (remaining <= 0) {
                return true;
            }

            BlockEntity blockEntity = level.getBlockEntity(troughPos);

            if (!(blockEntity instanceof TroughBlockEntity trough)) {
                continue;
            }

            int available = trough.getWheatCount();
            int consumed = Math.min(available, remaining);

            if (consumed > 0 && trough.consumeWheat(consumed)) {
                remaining -= consumed;
            }
        }

        return remaining <= 0;
    }

    public static TroughBlockEntity getPrimaryTrough(ServerLevel level, TroughFold fold) {
        BlockPos primaryPos = getPrimaryTroughPos(fold);
        BlockEntity blockEntity = level.getBlockEntity(primaryPos);

        if (blockEntity instanceof TroughBlockEntity trough) {
            return trough;
        }

        throw new IllegalStateException("Fold primary trough is missing at " + primaryPos);
    }

    public static BlockPos getPrimaryTroughPos(TroughFold fold) {
        return fold.troughPositions()
                .stream()
                .min(TroughFoldUtil::compareBlockPos)
                .orElseThrow();
    }

    private static List<BlockPos> findTroughsInsideArea(
            ServerLevel level,
            int minX,
            int minZ,
            int maxX,
            int maxZ,
            int y
    ) {
        List<BlockPos> positions = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos pos = new BlockPos(x, y, z);

                if (level.getBlockState(pos).is(ModBlocks.TROUGH)) {
                    positions.add(pos.immutable());
                }
            }
        }

        positions.sort(TroughFoldUtil::compareBlockPos);
        return positions;
    }

    private static TroughAnimalType findFoldAnimalType(ServerLevel level, List<BlockPos> troughPositions) {
        TroughAnimalType found = TroughAnimalType.EMPTY;

        for (BlockPos troughPos : troughPositions) {
            BlockEntity blockEntity = level.getBlockEntity(troughPos);

            if (!(blockEntity instanceof TroughBlockEntity trough)) {
                continue;
            }

            TroughAnimalType type = trough.getAnimalType();

            if (type == TroughAnimalType.EMPTY) {
                continue;
            }

            if (found == TroughAnimalType.EMPTY) {
                found = type;
                continue;
            }

            if (found != type) {
                return found;
            }
        }

        return found;
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

    @Nullable
    public static TroughFold getSavedFoldByKey(ServerLevel level, TroughFoldKey key) {
        List<BlockPos> troughPositions = findTroughsInsideArea(
                level,
                key.minX(),
                key.minZ(),
                key.maxX(),
                key.maxZ(),
                key.y()
        );

        if (troughPositions.isEmpty()) {
            return null;
        }

        TroughAnimalType animalType = findFoldAnimalType(level, troughPositions);

        return new TroughFold(
                key.dimension(),
                key.minX(),
                key.minZ(),
                key.maxX(),
                key.maxZ(),
                troughPositions,
                animalType
        );
    }

    public static void clearFoldAnimalType(ServerLevel level, TroughFold fold) {
        for (BlockPos troughPos : fold.troughPositions()) {
            BlockEntity blockEntity = level.getBlockEntity(troughPos);

            if (blockEntity instanceof TroughBlockEntity trough) {
                trough.clearAnimalType();
            }
        }
    }

    public static int getTotalPigFood(ServerLevel level, TroughFold fold) {
        int total = 0;

        for (BlockPos troughPos : fold.troughPositions()) {
            BlockEntity blockEntity = level.getBlockEntity(troughPos);

            if (blockEntity instanceof TroughBlockEntity trough) {
                total += trough.getPigFoodCount();
            }
        }

        return total;
    }

    public static boolean consumePigFood(ServerLevel level, TroughFold fold, int amount) {
        if (getTotalPigFood(level, fold) < amount) {
            return false;
        }

        int remaining = amount;

        List<BlockPos> troughs = new ArrayList<>(fold.troughPositions());
        troughs.sort(TroughFoldUtil::compareBlockPos);

        for (BlockPos troughPos : troughs) {
            if (remaining <= 0) {
                return true;
            }

            BlockEntity blockEntity = level.getBlockEntity(troughPos);

            if (!(blockEntity instanceof TroughBlockEntity trough)) {
                continue;
            }

            int available = trough.getPigFoodCount();
            int consumed = Math.min(available, remaining);

            if (consumed > 0 && trough.consumePigFood(consumed)) {
                remaining -= consumed;
            }
        }

        return remaining <= 0;
    }

    private static void ejectWrongFoodForAnimalType(
            ServerLevel level,
            TroughFold fold,
            TroughAnimalType animalType
    ) {
        for (BlockPos troughPos : fold.troughPositions()) {
            BlockEntity blockEntity = level.getBlockEntity(troughPos);

            if (!(blockEntity instanceof TroughBlockEntity trough)) {
                continue;
            }

            if (animalType == TroughAnimalType.PIG) {
                int wheat = trough.drainAllWheat();

                if (wheat > 0) {
                    Block.popResource(
                            level,
                            troughPos,
                            new ItemStack(Items.WHEAT, wheat)
                    );
                }

                continue;
            }

            if (animalType == TroughAnimalType.SHEEP
                    || animalType == TroughAnimalType.COW) {
                int carrots = trough.drainAllCarrots();
                int potatoes = trough.drainAllPotatoes();
                int beetroots = trough.drainAllBeetroots();

                if (carrots > 0) {
                    Block.popResource(
                            level,
                            troughPos,
                            new ItemStack(Items.CARROT, carrots)
                    );
                }

                if (potatoes > 0) {
                    Block.popResource(
                            level,
                            troughPos,
                            new ItemStack(Items.POTATO, potatoes)
                    );
                }

                if (beetroots > 0) {
                    Block.popResource(
                            level,
                            troughPos,
                            new ItemStack(Items.BEETROOT, beetroots)
                    );
                }
            }
        }
    }
}