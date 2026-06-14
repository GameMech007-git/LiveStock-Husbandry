package com.livestockhusbandry.entity.ai;

import com.livestockhusbandry.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

public class ChickenRestOnNestGoal extends Goal {

    private final Chicken chicken;
    private final double speedModifier;
    private final int searchRange;

    @Nullable
    private BlockPos targetNestPos;

    private int restTicks;
    private int cooldownTicks = 20 * 30;

    public ChickenRestOnNestGoal(Chicken chicken, double speedModifier, int searchRange) {
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

        if (ChickenNestUtil.isChickenRestTime(this.chicken)) {
            return false;
        }

        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return false;
        }


        if (this.chicken.getRandom().nextInt(80) != 0) {
            return false;
        }

        BlockPos foundNest = ChickenNestUtil.findAvailableNest(this.chicken, this.searchRange);

        if (foundNest == null) {
            this.cooldownTicks = 20 * 20;
            return false;
        }

        if (!ChickenNestReservations.reserve(
                this.chicken.level(),
                foundNest,
                this.chicken.getUUID()
        )) {
            this.cooldownTicks = 20 * 10;
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

        if (ChickenNestUtil.isChickenRestTime(this.chicken)) {
            return false;
        }

        if (!this.chicken.level().getBlockState(this.targetNestPos).is(ModBlocks.CHICKEN_NEST)) {
            return false;
        }

        if (ChickenNestReservations.isReservedByOther(
                this.chicken.level(),
                this.targetNestPos,
                this.chicken.getUUID()
        )) {
            return false;
        }

        return this.restTicks > 0 || !ChickenNestUtil.isOnNestCenter(this.chicken, this.targetNestPos);
    }

    @Override
    public void start() {
        this.restTicks = 20 * (4 + this.chicken.getRandom().nextInt(7));
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
        this.restTicks = 0;
        this.cooldownTicks = 20 * (45 + this.chicken.getRandom().nextInt(90));

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
            this.restTicks--;
        } else {
            ChickenNestUtil.moveToNestCenter(this.chicken, this.targetNestPos, this.speedModifier);
        }
    }
}