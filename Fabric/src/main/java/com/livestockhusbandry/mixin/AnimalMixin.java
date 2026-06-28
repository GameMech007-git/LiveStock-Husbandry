package com.livestockhusbandry.mixin;

import com.livestockhusbandry.ai.cow.CowTroughBreedingManager;
import com.livestockhusbandry.ai.pig.PigTroughBreedingManager;
import com.livestockhusbandry.ai.sheep.SheepTroughBreedingManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Animal.class)
public abstract class AnimalMixin {

    @Inject(method = "finalizeSpawnChildFromBreeding", at = @At("TAIL"))
    private void livestockhusbandry$registerTroughBredBaby(
            ServerLevel level,
            Animal partner,
            AgeableMob offspring,
            CallbackInfo ci
    ) {
        Animal parent = (Animal) (Object) this;

        if (parent instanceof Cow firstParent
                && partner instanceof Cow secondParent
                && offspring instanceof Cow babyCow) {

            CowTroughBreedingManager.registerTroughBredBabyIfPending(
                    level,
                    firstParent,
                    secondParent,
                    babyCow
            );

            return;
        }

        if (parent instanceof Pig firstParent
                && partner instanceof Pig secondParent
                && offspring instanceof Pig babyPig) {

            PigTroughBreedingManager.registerTroughBredBabyIfPending(
                    level,
                    firstParent,
                    secondParent,
                    babyPig
            );

            return;
        }

        if (parent instanceof Sheep firstParent
                && partner instanceof Sheep secondParent
                && offspring instanceof Sheep babySheep) {

            SheepTroughBreedingManager.registerTroughBredBabyIfPending(
                    level,
                    firstParent,
                    secondParent,
                    babySheep
            );
        }
    }
}