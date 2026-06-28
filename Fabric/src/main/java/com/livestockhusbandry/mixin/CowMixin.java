package com.livestockhusbandry.mixin;

import com.livestockhusbandry.entity.ai.cow.CowRegisterToTroughGoal;
import com.livestockhusbandry.entity.ai.cow.CowReturnToTroughGoal;
import com.livestockhusbandry.entity.ai.cow.CowTroughReservations;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractCow.class)
public abstract class CowMixin extends Animal {

    protected CowMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void livestockhusbandry$addCowTroughGoals(CallbackInfo ci) {
        if (!((Object) this instanceof Cow cow)) {
            return;
        }

        this.goalSelector.addGoal(
                4,
                new CowReturnToTroughGoal(cow, 1.0D)
        );

        this.goalSelector.addGoal(
                6,
                new CowRegisterToTroughGoal(cow, 12)
        );
    }
}