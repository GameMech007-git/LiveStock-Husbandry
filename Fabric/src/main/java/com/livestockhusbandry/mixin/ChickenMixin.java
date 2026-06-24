package com.livestockhusbandry.mixin;

import com.livestockhusbandry.entity.ai.chicken.ChickenRestOnNestGoal;
import com.livestockhusbandry.entity.ai.chicken.ChickenSleepInNestGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chicken.class)
public abstract class ChickenMixin extends Animal {

    protected ChickenMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void livestockhusbandry$addChickenNestGoals(CallbackInfo ci) {
        this.goalSelector.addGoal(
                4,
                new ChickenSleepInNestGoal((Chicken) (Object) this, 1.0D, 8)
        );

        this.goalSelector.addGoal(
                7,
                new ChickenRestOnNestGoal((Chicken) (Object) this, 1.0D, 8)
        );
    }
}