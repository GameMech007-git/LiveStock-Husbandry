package com.livestockhusbandry.block.entity;

import com.livestockhusbandry.block.trough.TroughFenceAreaUtil;
import com.livestockhusbandry.block.trough.TroughUtil;
import com.livestockhusbandry.entity.ai.sheep.SheepTroughBreedingManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class TroughBlockEntity extends BlockEntity {

    public static final int MAX_WHEAT = 384;
    private int breedingCooldownTicks = 20 * 60;

    private boolean hasFenceArea = false;
    private int fenceMinX = 0;
    private int fenceMinZ = 0;
    private int fenceMaxX = 0;
    private int fenceMaxZ = 0;

    private int wheatCount = 0;
    private int tickCounter = 0;

    public TroughBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.TROUGH, pos, blockState);
    }

    public int getWheatCount() {
        return wheatCount;
    }

    public int insertWheat(int amount) {
        int space = MAX_WHEAT - wheatCount;
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

    public void refreshFenceArea(ServerLevel level) {
        TroughUtil.TroughGroup group = TroughUtil.resolveGroup(level, worldPosition);

        if (!group.controllerPos().equals(worldPosition)) {
            BlockEntity controllerEntity = level.getBlockEntity(group.controllerPos());

            if (controllerEntity instanceof TroughBlockEntity controllerTrough) {
                controllerTrough.refreshFenceArea(level);
            }

            return;
        }

        TroughFenceAreaUtil.Result result = TroughFenceAreaUtil.scan(level, group.centerPos());

        if (result.found()) {
            hasFenceArea = true;
            fenceMinX = result.minX();
            fenceMinZ = result.minZ();
            fenceMaxX = result.maxX();
            fenceMaxZ = result.maxZ();
        } else {
            hasFenceArea = false;
            fenceMinX = 0;
            fenceMinZ = 0;
            fenceMaxX = 0;
            fenceMaxZ = 0;
        }

        setChanged();
    }

    public boolean isInsideSavedArea(BlockPos pos, TroughUtil.TroughGroup group) {
        if (hasFenceArea) {
            return pos.getX() >= fenceMinX
                    && pos.getX() <= fenceMaxX
                    && pos.getZ() >= fenceMinZ
                    && pos.getZ() <= fenceMaxZ;
        }

        int radius = group.radius();

        return Math.abs(pos.getX() - group.centerPos().getX()) <= radius
                && Math.abs(pos.getZ() - group.centerPos().getZ()) <= radius;
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

        if (trough.tickCounter < 200) {
            return;
        }

        trough.tickCounter = 0;
        trough.tickSlow(serverLevel);
    }

    private void tickSlow(ServerLevel level) {
        TroughUtil.TroughGroup group = TroughUtil.resolveGroup(level, worldPosition);

        if (!group.controllerPos().equals(worldPosition)) {
            return;
        }

        if (breedingCooldownTicks > 0) {
            breedingCooldownTicks -= 200;
            return;
        }

        boolean bred = SheepTroughBreedingManager.tryBreed(level, group, this);

        if (bred) {
            breedingCooldownTicks = 20 * 120 + level.getRandom().nextInt(20 * 120);
        } else {
            breedingCooldownTicks = 20 * 30 + level.getRandom().nextInt(20 * 60);
        }

        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.putInt("WheatCount", wheatCount);

        output.putInt("HasFenceArea", hasFenceArea ? 1 : 0);
        output.putInt("FenceMinX", fenceMinX);
        output.putInt("FenceMinZ", fenceMinZ);
        output.putInt("FenceMaxX", fenceMaxX);
        output.putInt("FenceMaxZ", fenceMaxZ);
        output.putInt("BreedingCooldownTicks", breedingCooldownTicks);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        wheatCount = input.getInt("WheatCount").orElse(0);

        hasFenceArea = input.getInt("HasFenceArea").orElse(0) == 1;
        fenceMinX = input.getInt("FenceMinX").orElse(0);
        fenceMinZ = input.getInt("FenceMinZ").orElse(0);
        fenceMaxX = input.getInt("FenceMaxX").orElse(0);
        fenceMaxZ = input.getInt("FenceMaxZ").orElse(0);
        breedingCooldownTicks = input.getInt("BreedingCooldownTicks").orElse(20 * 60);
    }
}