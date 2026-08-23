package com.hollower.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(BackupConfirmScreen.class)
public abstract class MixinBackupConfirmScreen {
    @Shadow
    @Final
    private BackupConfirmScreen.Listener onProceed;

    /**
     * Auto-clicks "I Know What I'm Doing!" on the "Worlds using Experimental
     * Settings are not supported" confirmation screen, skipping the prompt entirely.
     */
    @Inject(at = @At("HEAD"), method = "init", cancellable = true)
    private void hollower$skipExperimentalWarning(CallbackInfo callback) {
        BackupConfirmScreen self = (BackupConfirmScreen) (Object) this;
        String title = self.getTitle().getString();
        String experimentalTitle = Component.translatable("selectWorld.backupQuestion.experimental").getString();
        if (!title.equals(experimentalTitle)) return;

        onProceed.proceed(false, false);
        callback.cancel();
    }
}
