package com.livestockhusbandry.mixin;

import com.livestockhusbandry.entity.ai.cow.CowTroughReservations;
import com.livestockhusbandry.entity.ai.sheep.SheepTroughReservations;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.sheep.Sheep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "remove", at = @At("HEAD"))
    private void livestockhusbandry$unregisterLivestockOnRemove(
            Entity.RemovalReason reason,
            CallbackInfo ci
    ) {
        if (!reason.shouldDestroy()) {
            return;
        }

        Entity entity = (Entity) (Object) this;

        if (entity.level().isClientSide()) {
            return;
        }

        if (entity instanceof Sheep sheep) {
            SheepTroughReservations.unregister(
                    sheep.level(),
                    sheep.getUUID()
            );
            return;
        }

        if (entity instanceof Cow cow) {
            CowTroughReservations.unregister(
                    cow.level(),
                    cow.getUUID()
            );
        }
    }
}