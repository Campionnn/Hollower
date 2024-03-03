package com.hollower.mixin;

import com.hollower.Hollower;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class MixinChatScreen {
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V", at = @At(value = "HEAD"), cancellable = true)
    private void onAddMessage(Text message, MessageSignatureData signature, int ticks, MessageIndicator indicator, boolean refresh, CallbackInfo ci) {
         if (Hollower.lastTeleportPos != null && message.getString().startsWith("Teleported ")) {
                String[] messageParts = message.getString().split(" ");
                float x = Float.parseFloat(messageParts[messageParts.length-3].replace(",", "")) - 0.5f;
                float y = Float.parseFloat(messageParts[messageParts.length-2].replace(",", ""));
                float z = Float.parseFloat(messageParts[messageParts.length-1].replace(",", "")) - 0.5f;
                if (Hollower.lastTeleportPos.getX() == x && Hollower.lastTeleportPos.getY() == y && Hollower.lastTeleportPos.getZ() == z) {
                    Hollower.lastTeleportPos = null;
                    ci.cancel();
                }
         }
    }
}
