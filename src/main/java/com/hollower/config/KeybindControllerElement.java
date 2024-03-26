package com.hollower.config;

import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.ControllerWidget;
import dev.isxander.yacl3.gui.utils.GuiUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

public class KeybindControllerElement extends ControllerWidget<IKeybindController<?>> {
    protected final boolean instantApply;
    protected String inputField;
    protected Dimension<Integer> inputFieldBounds;
    protected boolean inputFieldFocused;
    protected int selectionLength;
    protected int renderOffset;
    protected float ticks;
    private final Text emptyText;

    public KeybindControllerElement(IKeybindController<?> control, YACLScreen screen, Dimension<Integer> dim, boolean instantApply) {
        super(control, screen, dim);
        this.instantApply = instantApply;
        inputField = control.getString();
        inputFieldFocused = false;
        selectionLength = 0;
        emptyText = Text.literal("Click to set keybind...").formatted(Formatting.GRAY);
        control.option().addListener((opt, val) -> {
            inputField = control.getString();
        });
        setDimension(dim);
    }

    @Override
    protected void drawValueText(DrawContext graphics, int mouseX, int mouseY, float delta) {
        Text valueText = getValueText();
        if (!isHovered()) valueText = Text.literal(GuiUtils.shortenString(valueText.getString(), textRenderer, getMaxUnwrapLength(), "...")).setStyle(valueText.getStyle());

        int textX = getDimension().xLimit() - textRenderer.getWidth(valueText) + renderOffset - getXPadding();
        graphics.enableScissor(inputFieldBounds.x(), inputFieldBounds.y() - 2, inputFieldBounds.xLimit() + 1, inputFieldBounds.yLimit() + 4);
        graphics.drawText(textRenderer, valueText, textX, getTextY(), getValueColor(), true);

        if (isHovered()) {
            ticks += delta;
            graphics.fill(inputFieldBounds.x(), inputFieldBounds.yLimit(), inputFieldBounds.xLimit(), inputFieldBounds.yLimit() + 1, -1);
            graphics.fill(inputFieldBounds.x() + 1, inputFieldBounds.yLimit() + 1, inputFieldBounds.xLimit() + 1, inputFieldBounds.yLimit() + 2, 0xFF404040);
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isAvailable() && getDimension().isPointInside((int) mouseX, (int) mouseY)) {
            inputFieldFocused = true;
            return true;
        } else {
            inputFieldFocused = false;
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!inputFieldFocused) return false;
        inputField = "";
        write(InputUtil.fromKeyCode(keyCode, scanCode).getLocalizedText().getString());
        unfocus();
        return true;
    }

    protected void checkRenderOffset() {
        if (textRenderer.getWidth(inputField) < getUnshiftedLength()) {
            renderOffset = 0;
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!inputFieldFocused) return false;
        if (!Screen.hasControlDown()) {
            write(Character.toString(chr));
            return true;
        }
        return false;
    }

    public void write(String string) {
        if (modifyInput(builder -> builder.insert(0, string))) {
            checkRenderOffset();
        }
    }

    public boolean modifyInput(Consumer<StringBuilder> consumer) {
        StringBuilder temp = new StringBuilder(inputField);
        consumer.accept(temp);
        if (!control.isInputValid(temp.toString()))
            return false;
        inputField = temp.toString();
        if (instantApply)
            updateControl();
        return true;
    }

    public int getUnshiftedLength() {
        if (optionNameString.isEmpty())
            return getDimension().width() - getXPadding() * 2;
        return getDimension().width() / 8 * 5;
    }

    public int getMaxUnwrapLength() {
        if (optionNameString.isEmpty())
            return getDimension().width() - getXPadding() * 2;
        return getDimension().width() / 2;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        inputFieldFocused = focused;
    }

    @Override
    public void unfocus() {
        super.unfocus();
        inputFieldFocused = false;
        renderOffset = 0;
        if (!instantApply) updateControl();
    }

    @Override
    public void setDimension(Dimension<Integer> dim) {
        super.setDimension(dim);
        int width = Math.max(6, Math.min(textRenderer.getWidth(getValueText()), getUnshiftedLength()));
        inputFieldBounds = Dimension.ofInt(dim.xLimit() - getXPadding() - width, dim.centerY() - textRenderer.fontHeight / 2, width, textRenderer.fontHeight);
    }

    @Override
    public boolean isHovered() {
        return super.isHovered() || inputFieldFocused;
    }

    protected void updateControl() {
        control.setFromString(inputField);
    }

    @Override
    protected int getUnhoveredControlWidth() {
        return !isHovered() ? Math.min(getHoveredControlWidth(), getMaxUnwrapLength()) : getHoveredControlWidth();
    }

    @Override
    protected int getHoveredControlWidth() {
        return Math.min(textRenderer.getWidth(getValueText()), getUnshiftedLength());
    }

    @Override
    protected Text getValueText() {
        if (!inputFieldFocused && inputField.isEmpty()) return emptyText;
        return instantApply || !inputFieldFocused ? control.formatValue() : Text.literal(inputField);
    }
}
