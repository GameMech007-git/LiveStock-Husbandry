package com.livestockhusbandry.entity.ai.sheep;

import com.livestockhusbandry.block.trough.TroughUtil;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.sheep.Sheep;

import java.util.EnumSet;

public class SheepReturnToTroughGoal extends Goal {

    private final Sheep sheep;
    private final double speedModifier;

    private TroughUtil.TroughGroup targetGroup;

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

        TroughUtil.TroughGroup group = SheepTroughUtil.getRegisteredGroup(this.sheep);

        if (group == null) {
            return false;
        }

        if (SheepTroughUtil.isInsideTroughArea(this.sheep, group)) {
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

        if (!this.sheep.isAlive()) {
            return false;
        }

        if (SheepTroughUtil.isInsideTroughArea(this.sheep, this.targetGroup)) {
            return false;
        }

        return !SheepTroughUtil.isNearTroughCenter(this.sheep, this.targetGroup);
    }

    @Override
    public void start() {
        if (this.targetGroup != null) {
            SheepTroughUtil.moveNearTrough(this.sheep, this.targetGroup, this.speedModifier);
        }
    }

    @Override
    public void stop() {
        this.targetGroup = null;
        this.sheep.getNavigation().stop();
        this.cooldownTicks = 80 + this.sheep.getRandom().nextInt(120);
    }

    @Override
    public void tick() {
        if (this.targetGroup == null) {
            return;
        }

        SheepTroughUtil.lookAtTrough(this.sheep, this.targetGroup);

        if (this.sheep.getNavigation().isDone()) {
            SheepTroughUtil.moveNearTrough(this.sheep, this.targetGroup, this.speedModifier);
        }
    }
}