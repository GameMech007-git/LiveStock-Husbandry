package com.livestockhusbandry.entity.ai.cow;

import com.livestockhusbandry.block.trough.TroughUtil;
import com.livestockhusbandry.entity.ai.cow.CowTroughUtil;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.cow.Cow;

import java.util.EnumSet;

public class CowReturnToTroughGoal extends Goal {

    private final Cow cow;
    private final double speedModifier;

    private TroughUtil.TroughGroup targetGroup;

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

        TroughUtil.TroughGroup group = CowTroughUtil.getRegisteredGroup(this.cow);

        if (group == null) {
            return false;
        }

        if (CowTroughUtil.isInsideTroughArea(this.cow, group)) {
            return false;
        }

        this.targetGroup = group;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetGroup == null) {
            return false;
        }

        if (!this.cow.isAlive()) {
            return false;
        }

        if (CowTroughUtil.isInsideTroughArea(this.cow, this.targetGroup)) {
            return false;
        }

        return !CowTroughUtil.isNearTroughCenter(this.cow, this.targetGroup);
    }

    @Override
    public void start() {
        if (this.targetGroup != null) {
            CowTroughUtil.moveNearTrough(this.cow, this.targetGroup, this.speedModifier);
        }
    }

    @Override
    public void stop() {
        this.targetGroup = null;
        this.cow.getNavigation().stop();
        this.cooldownTicks = 80 + this.cow.getRandom().nextInt(120);
    }

    @Override
    public void tick() {
        if (this.targetGroup == null) {
            return;
        }

        CowTroughUtil.lookAtTrough(this.cow, this.targetGroup);

        if (this.cow.getNavigation().isDone()) {
            CowTroughUtil.moveNearTrough(this.cow, this.targetGroup, this.speedModifier);
        }
    }
}