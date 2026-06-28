package com.livestockhusbandry.ai.pig;

import com.livestockhusbandry.trough.fold.TroughFold;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.pig.Pig;

import java.util.EnumSet;

public final class PigReturnToTroughGoal extends Goal {

    private final Pig pig;
    private final double speedModifier;

    private TroughFold targetFold;
    private int repathCooldown;

    public PigReturnToTroughGoal(Pig pig, double speedModifier) {
        this.pig = pig;
        this.speedModifier = speedModifier;

        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        TroughFold fold = PigTroughUtil.getRegisteredFold(pig);

        if (fold == null) {
            return false;
        }

        if (PigTroughUtil.isInsideTroughArea(pig, fold)) {
            return false;
        }

        targetFold = fold;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return targetFold != null
                && pig.isAlive()
                && !PigTroughUtil.isInsideTroughArea(pig, targetFold);
    }

    @Override
    public void start() {
        repathCooldown = 0;
        PigTroughUtil.moveNearTrough(pig, targetFold, speedModifier);
    }

    @Override
    public void tick() {
        if (targetFold == null) {
            return;
        }

        PigTroughUtil.lookAtTrough(pig, targetFold);

        if (repathCooldown > 0) {
            repathCooldown--;
            return;
        }

        repathCooldown = 20;
        PigTroughUtil.moveNearTrough(pig, targetFold, speedModifier);
    }

    @Override
    public void stop() {
        targetFold = null;
        pig.getNavigation().stop();
    }
}