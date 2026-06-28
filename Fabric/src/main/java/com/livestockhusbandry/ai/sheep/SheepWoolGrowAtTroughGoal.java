package com.livestockhusbandry.ai.sheep;

import com.livestockhusbandry.trough.fold.TroughFold;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.sheep.Sheep;

import java.util.EnumSet;

public class SheepWoolGrowAtTroughGoal extends Goal {

    private final Sheep sheep;
    private final double speedModifier;

    private TroughFold targetFold;

    private int cooldownTicks;
    private int eatTicks;
    private boolean eating;

    private int stuckTicks;
    private double lastX;
    private double lastZ;

    public SheepWoolGrowAtTroughGoal(Sheep sheep) {
        this(sheep, 1.0D);
    }

    public SheepWoolGrowAtTroughGoal(Sheep sheep, double speedModifier) {
        this.sheep = sheep;
        this.speedModifier = speedModifier;
        this.cooldownTicks = 20 * 60 + sheep.getRandom().nextInt(20 * 120);

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.sheep.isAlive()) {
            return false;
        }

        if (this.sheep.isBaby()) {
            return false;
        }

        if (!this.sheep.isSheared()) {
            return false;
        }

        TroughFold fold = SheepTroughUtil.getRegisteredFold(this.sheep);

        if (fold == null) {
            this.cooldownTicks = 20 * 30;
            return false;
        }

        if (!SheepTroughUtil.hasWheatForWoolGrowth(this.sheep, fold)) {
            this.cooldownTicks = 20 * 30;
            return false;
        }

        if (--this.cooldownTicks > 0) {
            return false;
        }

        this.targetFold = fold;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetFold == null) {
            return false;
        }

        if (!this.sheep.isAlive()) {
            return false;
        }

        if (this.sheep.isBaby()) {
            return false;
        }

        return this.sheep.isSheared();
    }

    @Override
    public void start() {
        this.eating = false;
        this.eatTicks = 0;
        this.stuckTicks = 0;
        this.lastX = this.sheep.getX();
        this.lastZ = this.sheep.getZ();

        if (this.targetFold != null) {
            SheepTroughUtil.moveNearTrough(
                    this.sheep,
                    this.targetFold,
                    this.speedModifier
            );
        }
    }

    @Override
    public void stop() {
        this.targetFold = null;
        this.eating = false;
        this.eatTicks = 0;
        this.stuckTicks = 0;

        this.cooldownTicks = 20 * 180 + this.sheep.getRandom().nextInt(20 * 240);

        this.sheep.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.targetFold == null) {
            return;
        }

        SheepTroughUtil.lookAtTrough(this.sheep, this.targetFold);

        if (!this.eating) {
            updateStuckTicks();

            if (SheepTroughUtil.isNearTroughCenter(this.sheep, this.targetFold)) {
                if (!SheepTroughUtil.consumeWheatForWoolGrowth(this.sheep, this.targetFold)) {
                    this.cooldownTicks = 20 * 30;
                    return;
                }

                this.sheep.getNavigation().stop();

                this.eating = true;
                this.eatTicks = 40;

                SheepTroughUtil.playEatAnimation(this.sheep);
                return;
            }

            if (this.stuckTicks >= 60 && this.sheep.getNavigation().isDone()) {
                SheepTroughUtil.reassignEatBlockNearCurrentPosition(
                        this.sheep,
                        this.targetFold
                );
                this.stuckTicks = 0;
            }

            SheepTroughUtil.moveNearTrough(
                    this.sheep,
                    this.targetFold,
                    this.speedModifier
            );
            return;
        }

        this.sheep.getNavigation().stop();
        SheepTroughUtil.lookAtTrough(this.sheep, this.targetFold);

        this.eatTicks--;

        if (this.eatTicks <= 0) {
            this.sheep.setSheared(false);
        }
    }

    private void updateStuckTicks() {
        double dx = this.sheep.getX() - this.lastX;
        double dz = this.sheep.getZ() - this.lastZ;

        if (dx * dx + dz * dz < 0.0025D) {
            this.stuckTicks++;
        } else {
            this.stuckTicks = 0;
            this.lastX = this.sheep.getX();
            this.lastZ = this.sheep.getZ();
        }
    }
}