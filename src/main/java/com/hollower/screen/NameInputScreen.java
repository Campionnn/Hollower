package com.hollower.screen;

import com.hollower.utils.ClientCompat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

// Prompts for a single line of text: a route/folder name to save, or a new name when renaming one.
// Confirm is disabled on blank input; Cancel and Escape both just return to the parent screen untouched.
@Environment(EnvType.CLIENT)
public final class NameInputScreen extends Screen {
    private final Screen parent;
    private final String prompt;
    private final String initialValue;
    private final Consumer<String> onAccept;

    private EditBox nameField;
    private Button confirmButton;

    public NameInputScreen(Screen parent, String prompt, String initialValue, Consumer<String> onAccept) {
        super(Component.literal(prompt));
        this.parent = parent;
        this.prompt = prompt;
        this.initialValue = initialValue == null ? "" : initialValue;
        this.onAccept = onAccept;
    }

    @Override
    protected void init() {
        int fieldWidth = Math.min(240, width - 40);
        int left = (width - fieldWidth) / 2;
        int top = height / 2 - 30;

        addRenderableWidget(new StringWidget(
                left, top, fieldWidth, 20, Component.literal(prompt), font));

        nameField = new EditBox(font, left, top + 24, fieldWidth, 20, Component.literal(prompt));
        nameField.setMaxLength(64);
        nameField.setValue(initialValue);
        nameField.setResponder(unused -> updateConfirmActive());
        addRenderableWidget(nameField);
        setInitialFocus(nameField);

        int half = (fieldWidth - 4) / 2;
        confirmButton = Button.builder(Component.literal("Confirm"), button -> accept())
                .bounds(left, top + 52, half, 20)
                .build();
        addRenderableWidget(confirmButton);
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(left + half + 4, top + 52, fieldWidth - half - 4, 20)
                .build());

        updateConfirmActive();
    }

    private void updateConfirmActive() {
        confirmButton.active = !nameField.getValue().isBlank();
    }

    private void accept() {
        if (nameField.getValue().isBlank()) return;
        onAccept.accept(nameField.getValue().strip());
    }

    @Override
    public void onClose() {
        ClientCompat.setScreen(Minecraft.getInstance(), parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
