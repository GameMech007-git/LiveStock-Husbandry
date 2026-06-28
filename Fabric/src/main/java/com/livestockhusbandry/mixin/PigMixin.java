package com.livestockhusbandry.mixin;

import com.livestockhusbandry.ai.pig.PigRegisterToTroughGoal;
import com.livestockhusbandry.ai.pig.PigReturnToTroughGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Pig.class)
public abstract class PigMixin extends Animal {

    protected PigMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void livestockhusbandry$addPigTroughGoals(CallbackInfo ci) {
        Pig pig = (Pig) (Object) this;

        this.goalSelector.addGoal(
                4,
                new PigReturnToTroughGoal(pig, 1.0D)
        );

        this.goalSelector.addGoal(
                6,
                new PigRegisterToTroughGoal(pig, 12)
        );
    }
}