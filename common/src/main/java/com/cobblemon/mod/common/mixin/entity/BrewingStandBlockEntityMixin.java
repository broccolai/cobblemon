package com.cobblemon.mod.common.mixin.entity;

import com.cobblemon.mod.common.CobblemonItems;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin
{
    @Inject(
        method = "isValid",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    private void cobblemon$isValid(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        final BrewingStandBlockEntity entity = (BrewingStandBlockEntity) (Object) this;
        if (slot < 3)
        {
            if ((stack.isOf(CobblemonItems.MEDICINAL_BREW) ||
                    stack.isOf(CobblemonItems.POTION) ||
                    stack.isOf(CobblemonItems.SUPER_POTION) ||
                    stack.isOf(CobblemonItems.HYPER_POTION))
                    && entity.getStack(slot).isEmpty())
                cir.setReturnValue(true);
        }
    }
}
