package com.livestockhusbandry.mixin;

import com.livestockhusbandry.block.ModBlocks;
import com.livestockhusbandry.ai.chicken.ChickenNestEggManager;
import com.livestockhusbandry.ai.chicken.ChickenNestReservations;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chicken.class)
public abstract class ChickenEggMixin extends Animal {

    @Shadow
    public int eggTime;

    @Shadow
    public abstract boolean isChickenJockey();

    protected ChickenEggMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void livestockhusbandry$pauseVanillaEggsWhileNested(CallbackInfo ci) {
        Chicken chicken = (Chicken) (Object) this;

        if (chicken.level().isClientSide()) {
            return;
        }

        BlockPos reservedNest = ChickenNestReservations.getReservedNest(
                chicken.level(),
                chicken.getUUID()
        );

        if (reservedNest == null) {
            return;
        }

        if (!chicken.isAlive() || chicken.isBaby() || this.isChickenJockey()) {
            return;
        }

        this.eggTime = Math.max(this.eggTime, 6000);
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void livestockhusbandry$layNestEggsEarlyMorning(CallbackInfo ci) {
        Chicken chicken = (Chicken) (Object) this;

        if (!(chicken.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!chicken.isAlive() || chicken.isBaby() || this.isChickenJockey()) {
            return;
        }

        BlockPos reservedNest = ChickenNestReservations.getReservedNest(
                serverLevel,
                chicken.getUUID()
        );

        if (reservedNest == null) {
            return;
        }

        if (!serverLevel.getBlockState(reservedNest).is(ModBlocks.CHICKEN_NEST)) {
            return;
        }

        if (!ChickenNestEggManager.tryLayEggsNow(serverLevel, chicken)) {
            return;
        }

        int eggCount = 1 + chicken.getRandom().nextInt(2);

        double eggX = reservedNest.getX() + 0.5D;
        double eggY = reservedNest.getY() + 0.55D;
        double eggZ = reservedNest.getZ() + 0.5D;

        ItemEntity eggEntity = new ItemEntity(
                serverLevel,
                eggX,
                eggY,
                eggZ,
                new ItemStack(Items.EGG, eggCount)
        );

        eggEntity.setDeltaMovement(0.0D, 0.0D, 0.0D);
        eggEntity.setPickUpDelay(10);

        serverLevel.addFreshEntity(eggEntity);

        boolean laidAnyEgg = true;

        if (laidAnyEgg) {
            chicken.playSound(
                    SoundEvents.CHICKEN_EGG,
                    1.0F,
                    (chicken.getRandom().nextFloat() - chicken.getRandom().nextFloat()) * 0.2F + 1.0F
            );

            chicken.gameEvent(GameEvent.ENTITY_PLACE);
        }

        this.eggTime = chicken.getRandom().nextInt(6000) + 6000;
    }
}