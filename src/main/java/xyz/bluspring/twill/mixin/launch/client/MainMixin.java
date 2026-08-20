package xyz.bluspring.twill.mixin.launch.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.bluspring.twill.internal.TwillModLoaders;
import xyz.bluspring.twill.loader.TwillOverrides;

import net.minecraft.client.main.Main;

@Mixin(Main.class)
public abstract class MainMixin {
    @Inject(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/Bootstrap;validate()V", shift = At.Shift.AFTER))
    private static void twill$constructClientModLoader(String[] args, CallbackInfo ci) {
        if (!TwillOverrides.getInstance().getHasLaunchOverride())
            TwillModLoaders.INSTANCE.getClientModLoader().begin();
    }
}
