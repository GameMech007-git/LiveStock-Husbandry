package com.livestockhusbandry.ai.cow;

import com.livestockhusbandry.trough.fold.TroughFold;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.cow.Cow;

import java.util.EnumSet;

public class CowReturnToTroughGoal extends Goal {

    private final Cow cow;
    private final double speedModifier;

    private TroughFold targetFold;

    private int cooldownTicks;

    public CowReturnToTroughGoal(Cow cow, double speedModifier) {
        this.cow = cow;
        this.speedModifier = speedModifier;
        this.cooldownTicks = 160 + this.cow.getRandom().nextInt(160);

        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.cow.isAlive()) {
            return false;
        }

        if (--this.cooldownTicks > 0) {
            return false;
        }

        this.cooldownTicks = 60 + this.cow.getRandom().nextInt(80);

        TroughFold fold = CowTroughUtil.getRegisteredFold(this.cow);

        if (fold == null) {
            return false;
        }

        if (CowTroughUtil.isInsideTroughArea(this.cow, fold)) {
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

        if (!this.cow.isAlive()) {
            return false;
        }

        if (CowTroughUtil.isInsideTroughArea(this.cow, this.targetFold)) {
            return false;
        }

        return !CowTroughUtil.isNearTroughCenter(this.cow, this.targetFold);
    }

    @Override
    public void start() {
        if (this.targetFold != null) {
            CowTroughUtil.moveNearTrough(this.cow, this.targetFold, this.speedModifier);
        }
    }

    @Override
    public void stop() {
        this.targetFold = null;
        this.cow.getNavigation().stop();
        this.cooldownTicks = 80 + this.cow.getRandom().nextInt(120);
    }

    @Override
    public void tick() {
        if (this.targetFold == null) {
            return;
        }

        CowTroughUtil.lookAtTrough(this.cow, this.targetFold);

        if (this.cow.getNavigation().isDone()) {
            CowTroughUtil.moveNearTrough(this.cow, this.targetFold, this.speedModifier);
        }
    }
}