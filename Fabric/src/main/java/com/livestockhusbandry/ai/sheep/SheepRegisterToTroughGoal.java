package com.livestockhusbandry.ai.sheep;

import com.livestockhusbandry.trough.fold.TroughFold;
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

        if (SheepTroughReservations.getRegisteredFoldKey(
                this.sheep.level(),
                this.sheep.getUUID()
        ) != null) {
            return false;
        }

        TroughFold fold = SheepTroughUtil.findNearbyAvailableFold(
                this.sheep,
                this.searchRange
        );

        if (fold == null) {
            return false;
        }

        SheepTroughReservations.register(
                this.sheep.level(),
                this.sheep.getUUID(),
                this.sheep.blockPosition(),
                fold
        );

        return false;
    }
}