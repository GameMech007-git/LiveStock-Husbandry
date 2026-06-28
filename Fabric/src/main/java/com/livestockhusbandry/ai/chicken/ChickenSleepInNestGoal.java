package com.livestockhusbandry.ai.chicken;

import com.livestockhusbandry.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

public class ChickenSleepInNestGoal extends Goal {

    private final Chicken chicken;
    private final double speedModifier;
    private final int searchRange;

    @Nullable
    private BlockPos targetNestPos;

    private int searchCooldown = 0;

    public ChickenSleepInNestGoal(Chicken chicken, double speedModifier, int searchRange) {
        this.chicken = chicken;
        this.speedModifier = speedModifier;
        this.searchRange = searchRange;

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.chicken.isBaby()) {
            return false;
        }

        if (!ChickenNestUtil.isChickenRestTime(this.chicken)) {
            return false;
        }

        if (--this.searchCooldown > 0) {
            return false;
        }

        this.searchCooldown = 40;

        BlockPos foundNest = ChickenNestUtil.findAvailableNest(this.chicken, this.searchRange);

        if (foundNest == null) {
            return false;
        }

        if (!ChickenNestReservations.reserve(
                this.chicken.level(),
                foundNest,
                this.chicken.getUUID()
        )) {
            return false;
        }

        this.targetNestPos = foundNest;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetNestPos == null) {
            return false;
        }

        if (!this.chicken.isAlive()) {
            return false;
        }

        if (!ChickenNestUtil.isChickenRestTime(this.chicken)) {
            return false;
        }

        if (!this.chicken.level().getBlockState(this.targetNestPos).is(ModBlocks.CHICKEN_NEST)) {
            return false;
        }

        return !ChickenNestReservations.isReservedByOther(
                this.chicken.level(),
                this.targetNestPos,
                this.chicken.getUUID()
        );
    }

    @Override
    public void start() {
        ChickenNestUtil.moveToNestCenter(this.chicken, this.targetNestPos, this.speedModifier);
    }

    @Override
    public void stop() {
        if (this.targetNestPos != null) {
            ChickenNestReservations.release(
                    this.chicken.level(),
                    this.targetNestPos,
                    this.chicken.getUUID()
            );
        }

        this.targetNestPos = null;
        this.chicken.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.targetNestPos == null) {
            return;
        }

        ChickenNestUtil.lookAtNest(this.chicken, this.targetNestPos);

        if (ChickenNestUtil.isOnNestCenter(this.chicken, this.targetNestPos)) {
            this.chicken.getNavigation().stop();
            ChickenNestUtil.stopHorizontalMovement(this.chicken);
        } else {
            ChickenNestUtil.moveToNestCenter(this.chicken, this.targetNestPos, this.speedModifier);
        }
    }
}