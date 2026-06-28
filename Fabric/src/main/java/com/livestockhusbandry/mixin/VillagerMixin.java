package com.livestockhusbandry.mixin;

import com.livestockhusbandry.ai.butcher.ButcherWorkManager;
import com.livestockhusbandry.ai.shepherd.ShepherdShearingManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerMixin extends AbstractVillager {

    protected VillagerMixin(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void livestockhusbandry$livestockVillagerWork(ServerLevel level, CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;

        ShepherdShearingManager.tick(level, villager);
        ButcherWorkManager.tick(level, villager);
    }
}