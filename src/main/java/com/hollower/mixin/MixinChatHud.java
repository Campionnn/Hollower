package com.hollower.mixin;

import com.hollower.Hollower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;

@Environment(EnvType.CLIENT)
@Mixin(ChatComponent.class)
public class MixinChatHud {
    @Inject(
            method = "addServerSystemMessage(Lnet/minecraft/network/chat/Component;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hollower$filterCommandResponse(Component message, CallbackInfo callback) {
        Iterator<String> commands = Hollower.lastCommands.iterator();
        while (commands.hasNext()) {
            String command = commands.next();
            if (message.getString().contains(command)
                    || (command.equals("Changed") && message.getString().contains("Could"))) {
                commands.remove();
                callback.cancel();
                return;
            }
        }
    }
}
