package com.livestockhusbandry.mixin;

import com.livestockhusbandry.entity.ai.sheep.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Sheep.class)
public abstract class SheepMixin extends Animal {

    protected SheepMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void livestockhusbandry$addSheepTroughGoals(CallbackInfo ci) {
        this.goalSelector.addGoal(
                4,
                new SheepReturnToTroughGoal((Sheep) (Object) this, 1.0D)
        );

        this.goalSelector.addGoal(
                6,
                new SheepRegisterToTroughGoal((Sheep) (Object) this, 12)
        );

        this.goalSelector.addGoal(
                3,
                new SheepWoolGrowAtTroughGoal((Sheep) (Object) this)
        );
    }

    @Inject(method = "ate", at = @At("HEAD"), cancellable = true)
    private void livestockhusbandry$stopRegisteredSheepGrassRegrow(CallbackInfo ci) {
        Sheep sheep = (Sheep) (Object) this;

        if (SheepTroughReservations.getRegisteredTrough(
                sheep.level(),
                sheep.getUUID()
        ) != null) {
            ci.cancel();
        }
    }
}