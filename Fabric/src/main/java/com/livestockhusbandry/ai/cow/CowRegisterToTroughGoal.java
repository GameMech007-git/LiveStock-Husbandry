package com.livestockhusbandry.ai.cow;

import com.livestockhusbandry.trough.fold.TroughFold;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.cow.Cow;

public class CowRegisterToTroughGoal extends Goal {

    private final Cow cow;
    private final int searchRange;

    private int cooldownTicks;

    public CowRegisterToTroughGoal(Cow cow, int searchRange) {
        this.cow = cow;
        this.searchRange = searchRange;
        this.cooldownTicks = 160 + this.cow.getRandom().nextInt(160);
    }

    @Override
    public boolean canUse() {
        if (!this.cow.isAlive()) {
            return false;
        }

        if (--this.cooldownTicks > 0) {
            return false;
        }

        this.cooldownTicks = 100 + this.cow.getRandom().nextInt(100);

        if (CowTroughReservations.getRegisteredFoldKey(
                this.cow.level(),
                this.cow.getUUID()
        ) != null) {
            return false;
        }

        TroughFold fold = CowTroughUtil.findNearbyAvailableFold(
                this.cow,
                this.searchRange
        );

        if (fold == null) {
            return false;
        }

        CowTroughReservations.register(
                this.cow.level(),
                this.cow.getUUID(),
                this.cow.blockPosition(),
                fold
        );

        return false;
    }
}