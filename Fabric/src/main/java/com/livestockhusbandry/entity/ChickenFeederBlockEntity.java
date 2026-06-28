package com.livestockhusbandry.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

public class ChickenFeederBlockEntity extends BlockEntity {

    public static final int RANGE = 6;
    public static final int VERTICAL_RANGE = 3;

    public static final int MAX_SEEDS = 384;
    public static final int MAX_POPULATION = 20;
    public static final int MAX_PAIRS_PER_DAY = 4;

    private int seedCount = 0;
    private long lastFedDay = -1L;
    private int tickCounter = 0;

    public ChickenFeederBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CHICKEN_FEEDER, pos, blockState);
    }

    public int getSeedCount() {
        return seedCount;
    }

    public boolean hasFedToday() {
        if (level == null) {
            return false;
        }

        long currentDay = level.getGameTime() / 24000L;
        return lastFedDay == currentDay;
    }

    public int insertSeeds(int amount) {
        int space = MAX_SEEDS - seedCount;
        int inserted = Math.min(space, amount);

        if (inserted > 0) {
            seedCount += inserted;
            setChanged();
        }

        return inserted;
    }

    public int countNearbyChickens() {
        if (level == null) {
            return 0;
        }

        return getNearbyChickens().size();
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            ChickenFeederBlockEntity feeder
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        feeder.tickCounter++;

        if (feeder.tickCounter < 200) {
            return;
        }

        feeder.tickCounter = 0;
        feeder.tryFeedChickens(serverLevel);
    }

    private void tryFeedChickens(ServerLevel level) {
        long currentDay = level.getGameTime() / 24000L;


        if (lastFedDay == currentDay) {
            return;
        }

        if (!isPrimaryFeeder(level)) {
            return;
        }

        List<Chicken> chickens = getNearbyChickens();
        int population = chickens.size();

        if (population >= MAX_POPULATION) {
            return;
        }

        List<Chicken> adults = chickens.stream()
                .filter(chicken -> !chicken.isBaby())
                .filter(chicken -> chicken.getAge() == 0)
                .sorted(Comparator.comparingDouble(chicken -> chicken.distanceToSqr(
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 0.5D,
                        worldPosition.getZ() + 0.5D
                )))
                .toList();

        if (adults.size() < 2) {
            return;
        }

        int freeSpace = MAX_POPULATION - population;
        int possiblePairsFromPopulation = freeSpace;
        int possiblePairsFromAdults = adults.size() / 2;
        int possiblePairsFromSeeds = seedCount / 2;

        int pairsToBreed = Math.min(
                Math.min(possiblePairsFromPopulation, possiblePairsFromAdults),
                Math.min(possiblePairsFromSeeds, MAX_PAIRS_PER_DAY)
        );

        if (pairsToBreed <= 0) {
            return;
        }

        int adultIndex = 0;

        for (int i = 0; i < pairsToBreed; i++) {
            Chicken first = adults.get(adultIndex++);
            Chicken second = adults.get(adultIndex++);

            seedCount -= 2;

            first.spawnChildFromBreeding(level, second);
        }


        lastFedDay = currentDay;
        setChanged();
    }

    private List<Chicken> getNearbyChickens() {
        AABB area = new AABB(worldPosition)
                .inflate(RANGE, VERTICAL_RANGE, RANGE);

        return level.getEntitiesOfClass(
                Chicken.class,
                area,
                chicken -> chicken.isAlive()
        );
    }

    private boolean isPrimaryFeeder(ServerLevel level) {
        BlockPos start = worldPosition.offset(-RANGE, -VERTICAL_RANGE, -RANGE);
        BlockPos end = worldPosition.offset(RANGE, VERTICAL_RANGE, RANGE);

        for (BlockPos checkPos : BlockPos.betweenClosed(start, end)) {
            if (checkPos.equals(worldPosition)) {
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(checkPos);

            if (blockEntity instanceof ChickenFeederBlockEntity) {
                if (hasHigherPriority(checkPos, worldPosition)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean hasHigherPriority(BlockPos other, BlockPos self) {
        if (other.getY() != self.getY()) {
            return other.getY() < self.getY();
        }

        if (other.getX() != self.getX()) {
            return other.getX() < self.getX();
        }

        return other.getZ() < self.getZ();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.putInt("SeedCount", seedCount);
        output.putLong("LastFedDay", lastFedDay);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        seedCount = input.getInt("SeedCount").orElse(0);
        lastFedDay = input.getLong("LastFedDay").orElse(-1L);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();

        if (level instanceof ServerLevel serverLevel && seedCount > 0) {
            ItemStack stack = new ItemStack(Items.WHEAT_SEEDS, seedCount);

            ItemEntity itemEntity = new ItemEntity(
                    serverLevel,
                    worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 0.5D,
                    worldPosition.getZ() + 0.5D,
                    stack
            );

            serverLevel.addFreshEntity(itemEntity);
        }
    }
}