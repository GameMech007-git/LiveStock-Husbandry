package com.livestockhusbandry.ai.pig;

import com.livestockhusbandry.trough.fold.TroughFold;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.pig.Pig;

import java.util.EnumSet;

public final class PigRegisterToTroughGoal extends Goal {

    private final Pig pig;
    private final int searchRange;

    private int cooldownTicks;

    public PigRegisterToTroughGoal(Pig pig, int searchRange) {
        this.pig = pig;
        this.searchRange = searchRange;
        this.cooldownTicks = pig.getRandom().nextInt(100);

        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        cooldownTicks = 100 + pig.getRandom().nextInt(100);

        if (PigTroughReservations.getRegisteredFoldKey(
                pig.level(),
                pig.getUUID()
        ) != null) {
            return false;
        }

        TroughFold fold = PigTroughUtil.findNearbyAvailableFold(
                pig,
                searchRange
        );

        if (fold == null) {
            return false;
        }

        return PigTroughReservations.register(
                pig.level(),
                pig.getUUID(),
                pig.blockPosition(),
                fold
        );
    }
}