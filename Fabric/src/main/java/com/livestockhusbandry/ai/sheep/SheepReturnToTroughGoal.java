package com.livestockhusbandry.ai.sheep;

import com.livestockhusbandry.trough.fold.TroughFold;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.sheep.Sheep;

import java.util.EnumSet;

public class SheepReturnToTroughGoal extends Goal {

    private final Sheep sheep;
    private final double speedModifier;

    private TroughFold targetFold;

    private int cooldownTicks;

    public SheepReturnToTroughGoal(Sheep sheep, double speedModifier) {
        this.sheep = sheep;
        this.speedModifier = speedModifier;
        this.cooldownTicks = 40 + sheep.getRandom().nextInt(80);

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.sheep.isAlive()) {
            return false;
        }

        if (--this.cooldownTicks > 0) {
            return false;
        }

        this.cooldownTicks = 60 + this.sheep.getRandom().nextInt(80);

        TroughFold fold = SheepTroughUtil.getRegisteredFold(this.sheep);

        if (fold == null) {
            return false;
        }

        if (SheepTroughUtil.isInsideTroughArea(this.sheep, fold)) {
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

        if (SheepTroughUtil.isInsideTroughArea(this.sheep, this.targetFold)) {
            return false;
        }

        return !SheepTroughUtil.isNearTroughCenter(this.sheep, this.targetFold);
    }

    @Override
    public void start() {
        if (this.targetFold != null) {
            SheepTroughUtil.moveNearTrough(this.sheep, this.targetFold, this.speedModifier);
        }
    }

    @Override
    public void stop() {
        this.targetFold = null;
        this.sheep.getNavigation().stop();
        this.cooldownTicks = 80 + this.sheep.getRandom().nextInt(120);
    }

    @Override
    public void tick() {
        if (this.targetFold == null) {
            return;
        }

        SheepTroughUtil.lookAtTrough(this.sheep, this.targetFold);

        if (this.sheep.getNavigation().isDone()) {
            SheepTroughUtil.moveNearTrough(this.sheep, this.targetFold, this.speedModifier);
        }
    }
}