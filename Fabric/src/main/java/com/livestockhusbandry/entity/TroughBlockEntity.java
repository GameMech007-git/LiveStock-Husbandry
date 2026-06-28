package com.livestockhusbandry.entity;

import com.livestockhusbandry.trough.TroughAnimalType;
import com.livestockhusbandry.trough.fold.TroughFold;
import com.livestockhusbandry.trough.fold.TroughFoldUtil;
import com.livestockhusbandry.ai.largelivestock.LargeLivestockFoldRules;
import com.livestockhusbandry.ai.cow.CowTroughBreedingManager;
import com.livestockhusbandry.ai.cow.CowTroughReservations;
import com.livestockhusbandry.ai.pig.PigTroughBreedingManager;
import com.livestockhusbandry.ai.pig.PigTroughReservations;
import com.livestockhusbandry.ai.sheep.SheepTroughBreedingManager;
import com.livestockhusbandry.ai.sheep.SheepTroughReservations;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;

public class TroughBlockEntity extends BlockEntity {

    public static final int MAX_FEED_PER_TYPE = 384;

    private static final int BREEDING_FOOD_COST = 2;

    private int wheatCount = 0;
    private int carrotCount = 0;
    private int potatoCount = 0;
    private int beetrootCount = 0;

    private static final int SLOW_TICK_INTERVAL = 1000;

    private int breedingCooldownTicks = 20 * 60;

    private boolean hasFenceArea = false;
    private int fenceMinX = 0;
    private int fenceMinZ = 0;
    private int fenceMaxX = 0;
    private int fenceMaxZ = 0;
    private int tickCounter = 0;

    private TroughAnimalType animalType = TroughAnimalType.EMPTY;

    public TroughBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.TROUGH, pos, blockState);
    }

    public int getCarrotCount() {
        return carrotCount;
    }

    public int getPotatoCount() {
        return potatoCount;
    }

    public int getBeetrootCount() {
        return beetrootCount;
    }

    public int getPigFoodCount() {
        return carrotCount + potatoCount + beetrootCount;
    }

    public int insertCarrot(int amount) {
        int space = MAX_FEED_PER_TYPE - carrotCount;
        int inserted = Math.min(space, amount);

        if (inserted > 0) {
            carrotCount += inserted;
            setChanged();
        }

        return inserted;
    }

    public int insertPotato(int amount) {
        int space = MAX_FEED_PER_TYPE - potatoCount;
        int inserted = Math.min(space, amount);

        if (inserted > 0) {
            potatoCount += inserted;
            setChanged();
        }

        return inserted;
    }

    public int insertBeetroot(int amount) {
        int space = MAX_FEED_PER_TYPE - beetrootCount;
        int inserted = Math.min(space, amount);

        if (inserted > 0) {
            beetrootCount += inserted;
            setChanged();
        }

        return inserted;
    }

    public int drainAllWheat() {
        int amount = wheatCount;

        if (amount > 0) {
            wheatCount = 0;
            setChanged();
        }

        return amount;
    }

    public int drainAllCarrots() {
        int amount = carrotCount;

        if (amount > 0) {
            carrotCount = 0;
            setChanged();
        }

        return amount;
    }

    public int drainAllPotatoes() {
        int amount = potatoCount;

        if (amount > 0) {
            potatoCount = 0;
            setChanged();
        }

        return amount;
    }

    public int drainAllBeetroots() {
        int amount = beetrootCount;

        if (amount > 0) {
            beetrootCount = 0;
            setChanged();
        }

        return amount;
    }

    public boolean consumePigFood(int amount) {
        if (getPigFoodCount() < amount) {
            return false;
        }

        int remaining = amount;

        int carrots = Math.min(carrotCount, remaining);
        carrotCount -= carrots;
        remaining -= carrots;

        int potatoes = Math.min(potatoCount, remaining);
        potatoCount -= potatoes;
        remaining -= potatoes;

        int beetroots = Math.min(beetrootCount, remaining);
        beetrootCount -= beetroots;
        remaining -= beetroots;

        setChanged();
        return true;
    }

    public int getWheatCount() {
        return wheatCount;
    }

    public int insertWheat(int amount) {
        int space = MAX_FEED_PER_TYPE - wheatCount;
        int inserted = Math.min(space, amount);

        if (inserted > 0) {
            wheatCount += inserted;
            setChanged();
        }

        return inserted;
    }

    public boolean consumeWheat(int amount) {
        if (wheatCount < amount) {
            return false;
        }

        wheatCount -= amount;
        setChanged();
        return true;
    }

    public TroughAnimalType getAnimalType() {
        return animalType;
    }

    public void lockAnimalType(TroughAnimalType type) {
        if (type == TroughAnimalType.EMPTY) {
            return;
        }

        if (animalType != TroughAnimalType.EMPTY) {
            return;
        }

        animalType = type;
        setChanged();
    }

    public boolean hasFenceArea() {
        return hasFenceArea;
    }

    public int getFenceMinX() {
        return fenceMinX;
    }

    public int getFenceMinZ() {
        return fenceMinZ;
    }

    public int getFenceMaxX() {
        return fenceMaxX;
    }

    public int getFenceMaxZ() {
        return fenceMaxZ;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            TroughBlockEntity trough
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        trough.tickCounter++;

        if (trough.tickCounter < SLOW_TICK_INTERVAL) {
            return;
        }

        trough.tickCounter = 0;
        trough.tickSlow(serverLevel);
    }

    private void tickSlow(ServerLevel level) {
        TroughFold fold = TroughFoldUtil.getSavedFoldFromTrough(level, worldPosition);

        if (fold == null) {
            return;
        }

        if (!TroughFoldUtil.getPrimaryTroughPos(fold).equals(worldPosition)) {
            return;
        }

        clearAnimalTypeIfFoldEmpty(level, fold);

        if (animalType == TroughAnimalType.EMPTY) {
            return;
        }

        if (breedingCooldownTicks > 0) {
            breedingCooldownTicks -= SLOW_TICK_INTERVAL;
            return;
        }

        if (animalType == TroughAnimalType.SHEEP
                || animalType == TroughAnimalType.COW) {
            if (TroughFoldUtil.getTotalWheat(level, fold) < BREEDING_FOOD_COST) {
                breedingCooldownTicks = 20 * 30 + level.getRandom().nextInt(20 * 60);
                setChanged();
                return;
            }
        }

        if (animalType == TroughAnimalType.PIG) {
            if (TroughFoldUtil.getTotalPigFood(level, fold) < BREEDING_FOOD_COST) {
                breedingCooldownTicks = 20 * 30 + level.getRandom().nextInt(20 * 60);
                setChanged();
                return;
            }
        }

        boolean bred = false;

        if (animalType == TroughAnimalType.SHEEP) {
            int sheepCount = SheepTroughReservations.getRegisteredCount(
                    level,
                    fold.key()
            );

            if (sheepCount >= 2 && sheepCount < fold.sheepLimit()) {
                bred = SheepTroughBreedingManager.tryBreed(level, fold);
            }
        } else if (animalType == TroughAnimalType.COW) {
            int cowCount = CowTroughReservations.getRegisteredCount(
                    level,
                    fold.key()
            );

            int cowBreedingLimit = LargeLivestockFoldRules.breedingLimit(
                    fold.troughCount()
            );

            if (cowCount >= 2 && cowCount < cowBreedingLimit) {
                bred = CowTroughBreedingManager.tryBreed(level, fold);
            }
        }  else if (animalType == TroughAnimalType.PIG) {
            int pigCount = PigTroughReservations.getRegisteredCount(
                    level,
                    fold.key()
            );

            int pigBreedingLimit = LargeLivestockFoldRules.breedingLimit(
                    fold.troughCount()
            );

            if (pigCount >= 2 && pigCount < pigBreedingLimit) {
                bred = PigTroughBreedingManager.tryBreed(level, fold);
            }
        }

        if (bred) {
            breedingCooldownTicks = 20 * 120 + level.getRandom().nextInt(20 * 120);
        } else {
            breedingCooldownTicks = 20 * 30 + level.getRandom().nextInt(20 * 60);
        }

        setChanged();
    }

    private void clearAnimalTypeIfFoldEmpty(ServerLevel level, TroughFold fold) {
        if (animalType == TroughAnimalType.SHEEP) {
            int sheepCount = SheepTroughReservations.getRegisteredCount(
                    level,
                    fold.key()
            );

            if (sheepCount <= 0) {
                TroughFoldUtil.clearFoldAnimalType(level, fold);
            }

            return;
        }

        if (animalType == TroughAnimalType.COW) {
            int cowCount = CowTroughReservations.getRegisteredCount(
                    level,
                    fold.key()
            );

            if (cowCount <= 0) {
                TroughFoldUtil.clearFoldAnimalType(level, fold);
            }

            return;
        }

        if (animalType == TroughAnimalType.PIG) {
            int pigCount = PigTroughReservations.getRegisteredCount(
                    level,
                    fold.key()
            );

            if (pigCount <= 0) {
                TroughFoldUtil.clearFoldAnimalType(level, fold);
            }
        }
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);

        output.putInt("WheatCount", wheatCount);
        output.putInt("CarrotCount", carrotCount);
        output.putInt("PotatoCount", potatoCount);
        output.putInt("BeetrootCount", beetrootCount);

        output.putInt("HasFenceArea", hasFenceArea ? 1 : 0);
        output.putInt("FenceMinX", fenceMinX);
        output.putInt("FenceMinZ", fenceMinZ);
        output.putInt("FenceMaxX", fenceMaxX);
        output.putInt("FenceMaxZ", fenceMaxZ);
        output.putInt("BreedingCooldownTicks", breedingCooldownTicks);
        output.putInt("AnimalType", animalType.ordinal());
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);

        wheatCount = input.getInt("WheatCount").orElse(0);
        carrotCount = input.getInt("CarrotCount").orElse(0);
        potatoCount = input.getInt("PotatoCount").orElse(0);
        beetrootCount = input.getInt("BeetrootCount").orElse(0);

        hasFenceArea = input.getInt("HasFenceArea").orElse(0) == 1;
        fenceMinX = input.getInt("FenceMinX").orElse(0);
        fenceMinZ = input.getInt("FenceMinZ").orElse(0);
        fenceMaxX = input.getInt("FenceMaxX").orElse(0);
        fenceMaxZ = input.getInt("FenceMaxZ").orElse(0);
        breedingCooldownTicks = input.getInt("BreedingCooldownTicks").orElse(20 * 60);

        int savedAnimalType = input.getInt("AnimalType").orElse(0);
        TroughAnimalType[] values = TroughAnimalType.values();

        if (savedAnimalType < 0 || savedAnimalType >= values.length) {
            animalType = TroughAnimalType.EMPTY;
        } else {
            animalType = values[savedAnimalType];
        }
    }

    public void saveFenceArea(int minX, int minZ, int maxX, int maxZ) {
        this.hasFenceArea = true;
        this.fenceMinX = minX;
        this.fenceMinZ = minZ;
        this.fenceMaxX = maxX;
        this.fenceMaxZ = maxZ;
        setChanged();
    }

    public void clearFenceArea() {
        this.hasFenceArea = false;
        this.fenceMinX = 0;
        this.fenceMinZ = 0;
        this.fenceMaxX = 0;
        this.fenceMaxZ = 0;
        setChanged();
    }

    public void clearAnimalType() {
        if (animalType == TroughAnimalType.EMPTY) {
            return;
        }

        animalType = TroughAnimalType.EMPTY;
        setChanged();
    }
}