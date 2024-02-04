package com.cobblemon.mod.common.mixin;

import com.cobblemon.mod.common.sherds.CobblemonSherds;
import net.minecraft.block.DecoratedPotPatterns;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DecoratedPotPatterns.class)
public abstract class DecoratedPotPatternsMixin {
    @Inject(method = "fromSherd", at=@At("HEAD"), cancellable = true)
    private static void cobblemon$getCobblemonSherdTexture(Item sherd,
        CallbackInfoReturnable<RegistryKey<String>> cir) {
        if (CobblemonSherds.INSTANCE.getSherdToPattern().containsKey(sherd)) {
            cir.setReturnValue(CobblemonSherds.INSTANCE.getSherdToPattern().get(sherd));
            return;
        }
    }
}
