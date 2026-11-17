package xyz.bluspring.twill.mixin.launch.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.bluspring.twill.internal.TwillModLoaders;
import xyz.bluspring.twill.loader.TwillOverrides;

import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;registerPanoramaTextures(Lnet/minecraft/client/renderer/texture/TextureManager;)V", shift = At.Shift.AFTER))
    private void twill$finishLoadingFML(CallbackInfo ci) {
        if (!TwillOverrides.getInstance().getHasLaunchOverride())
            TwillModLoaders.INSTANCE.getClientModLoader().finish();
    }
}
