package xyz.bluspring.twill.mixin.launch.server;

import com.llamalad7.mixinextras.sugar.Local;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.bluspring.twill.internal.TwillModLoaders;

import net.minecraft.server.Main;

@Mixin(Main.class)
public abstract class MainMixin {
    @Inject(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/dedicated/DedicatedServerSettings;<init>(Ljava/nio/file/Path;)V"))
    private static void twill$constructClientModLoader(String[] args, CallbackInfo ci, @Local(name = "options") OptionSet options, @Local(name = "initSettings") OptionSpec<Void> initSettings) {
        if (!options.has(initSettings)) {
            TwillModLoaders.INSTANCE.getServerModLoader().load(false);
        }
    }
}
