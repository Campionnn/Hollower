package com.hollower.screen;

import com.hollower.Hollower;
import com.hollower.screen.widget.ToggleButton;
import com.hollower.screen.widget.ValueSlider;
import com.hollower.utils.ClientCompat;
import com.hollower.utils.RouteOptimizer;
import com.hollower.utils.RouteUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// Tunes and runs the route order optimizer. Opened from the config screen's Route tab and returns to
// it when done. The settings here are the persisted Hollower.optimize* fields, so they are edited
// directly and saved with the rest of the config on close.
@Environment(EnvType.CLIENT)
public final class RouteOptimizeScreen extends Screen {
    private static final int ROW = 24;

    private final Screen parent;
    // The summary of the last run, kept so it survives the rebuild that follows applying a result.
    private Component summary = Component.empty();

    public RouteOptimizeScreen(Screen parent) {
        super(Component.literal("Optimize Route Order"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(260, width - 40);
        int left = (width - contentWidth) / 2;
        int y = Math.max(30, height / 2 - 110);

        int nodes = Hollower.positions.size();
        boolean busy = RouteUtils.isOptimizing();

        addRenderableWidget(new StringWidget(left, y, contentWidth, 12,
                Component.literal(nodes + (nodes == 1 ? " node" : " nodes") + " in this route")
                        .withStyle(ChatFormatting.GRAY), font));
        y += 20;

        addRenderableWidget(new StringWidget(left, y, contentWidth, 12,
                Component.literal("Cost").withStyle(ChatFormatting.YELLOW), font));
        y += 14;

        add(tip(ValueSlider.ofFloat(left, y, contentWidth, "Horizontal Scale", 0.1f, 5.0f,
                        Hollower.optimizeHorizontalScale, 1,
                        value -> Hollower.optimizeHorizontalScale = value),
                "Weight on movement along X and Z.\n"
                        + "Raise it to make flat distance matter more than height."), busy);
        y += ROW;

        add(tip(ValueSlider.ofFloat(left, y, contentWidth, "Vertical Scale", 0.1f, 10.0f,
                        Hollower.optimizeVerticalScale, 1,
                        value -> Hollower.optimizeVerticalScale = value),
                "Weight on movement along Y.\n"
                        + "At 2, one block of height costs as much as two blocks of flat travel, so\n"
                        + "the route finishes a level before changing height."), busy);
        y += ROW;

        add(tip(ValueSlider.ofFloat(left, y, contentWidth, "Turn Penalty", 0.0f, 50.0f,
                        Hollower.optimizeTurnWeight, 1,
                        value -> Hollower.optimizeTurnWeight = value),
                "Roughly how many blocks of extra travel a full 180° reversal is worth;\n"
                        + "a 90° turn costs half this. Raise it to trade a little extra distance\n"
                        + "for longer straight runs. Set to 0 for shortest distance only."), busy);
        y += ROW + 6;

        addRenderableWidget(new StringWidget(left, y, contentWidth, 12,
                Component.literal("Shape").withStyle(ChatFormatting.YELLOW), font));
        y += 14;

        add(tip(ToggleButton.create(left, y, contentWidth, "Closed Loop",
                        () -> Hollower.optimizeClosedLoop,
                        value -> Hollower.optimizeClosedLoop = value),
                "ON: the route is a lap, so the trip from the last node back to the first counts.\n"
                        + "OFF: the route is a one-way path and the return trip is free."), busy);
        y += ROW;

        add(tip(ToggleButton.create(left, y, contentWidth, "Keep First Node",
                        () -> Hollower.optimizePinFirst,
                        value -> Hollower.optimizePinFirst = value),
                "ON: node 1 stays where it is, so your entry point doesn't move.\n"
                        + "OFF: the optimizer picks where the route starts."), busy);
        y += ROW + 8;

        add(tip(Button.builder(
                                Component.literal(busy ? "Optimizing..." : "Optimize"),
                                button -> runOptimize())
                        .bounds(left, y, contentWidth, 20)
                        .build(),
                "Reorder the nodes using the settings above. The route's nodes don't move,\n"
                        + "only the order you visit them in."), busy || nodes < 4);
        y += ROW;

        add(Button.builder(Component.literal("Undo Optimize"), button -> {
                            RouteUtils.undoOptimize();
                            summary = Component.empty();
                            rebuildWidgets();
                        })
                        .bounds(left, y, contentWidth, 20)
                        .build(),
                busy || !RouteUtils.canUndoOptimize());
        y += ROW + 4;

        addRenderableWidget(new StringWidget(left, y, contentWidth, 12, summary, font));
        y += 20;

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(left, y, contentWidth, 20)
                .build());
    }

    private void runOptimize() {
        summary = Component.literal("Working...").withStyle(ChatFormatting.GRAY);
        RouteUtils.optimizeRoute(
                new RouteOptimizer.Options(
                        Hollower.optimizeHorizontalScale,
                        Hollower.optimizeVerticalScale,
                        Hollower.optimizeTurnWeight,
                        Hollower.optimizeClosedLoop,
                        Hollower.optimizePinFirst,
                        400),
                result -> {
                    summary = describe(result);
                    // The player may have closed this screen while the worker was running.
                    if (ClientCompat.currentScreen(Minecraft.getInstance()) == this) rebuildWidgets();
                });
        // Redraw straight away so the button reads "Optimizing..." while the worker runs.
        rebuildWidgets();
    }

    private static Component describe(RouteOptimizer.Result result) {
        if (result == null) return Component.empty();
        if (!result.changed()) {
            return Component.literal("Already optimal for these settings").withStyle(ChatFormatting.GRAY);
        }
        return Component.literal(String.format("Cost %.0f → %.0f  (-%.1f%%)",
                        result.costBefore(), result.costAfter(), result.improvementPercent()))
                .withStyle(ChatFormatting.GREEN);
    }

    // Disabled controls still draw, so the layout doesn't jump while the optimizer is running.
    private <T extends AbstractWidget> void add(T widget, boolean disabled) {
        widget.active = !disabled;
        addRenderableWidget(widget);
    }

    private static <T extends AbstractWidget> T tip(T widget, String text) {
        widget.setTooltip(Tooltip.create(Component.literal(text)));
        return widget;
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
