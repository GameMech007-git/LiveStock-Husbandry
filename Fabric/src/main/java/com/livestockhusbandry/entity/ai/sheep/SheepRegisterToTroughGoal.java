package com.livestockhusbandry.entity.ai.sheep;

import com.livestockhusbandry.block.trough.TroughUtil;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.sheep.Sheep;

public class SheepRegisterToTroughGoal extends Goal {

    private final Sheep sheep;
    private final int searchRange;

    private int cooldownTicks;

    public SheepRegisterToTroughGoal(Sheep sheep, int searchRange) {
        this.sheep = sheep;
        this.searchRange = searchRange;
        this.cooldownTicks = 20 + sheep.getRandom().nextInt(80);
    }

    @Override
    public boolean canUse() {
        if (!this.sheep.isAlive()) {
            return false;
        }

        if (--this.cooldownTicks > 0) {
            return false;
        }

        this.cooldownTicks = 100 + this.sheep.getRandom().nextInt(100);

        if (SheepTroughReservations.getRegisteredTrough(
                this.sheep.level(),
                this.sheep.getUUID()
        ) != null) {
            return false;
        }

        TroughUtil.TroughGroup group = SheepTroughUtil.findNearbyAvailableTroughGroup(
                this.sheep,
                this.searchRange
        );

        if (group == null) {
            return false;
        }

        SheepTroughReservations.register(
                this.sheep.level(),
                this.sheep.getUUID(),
                this.sheep.blockPosition(),
                group
        );

        return false;
    }
}