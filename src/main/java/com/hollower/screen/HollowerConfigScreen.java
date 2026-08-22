package com.hollower.screen;

import com.hollower.Hollower;
import com.hollower.render.HiddenBlockGroup;
import com.hollower.render.SelectiveRender;
import com.hollower.screen.widget.ColorButton;
import com.hollower.screen.widget.KeyBindControl;
import com.hollower.screen.widget.ToggleButton;
import com.hollower.screen.widget.ValueSlider;
import com.hollower.utils.ClientCompat;
import com.hollower.utils.HollowerConfig;
import com.hollower.utils.RenderTweaks;
import com.hollower.utils.RouteUtils;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Hollower's config screen.
 * <p>
 * Two things shape the layout. First, every control is <em>self-labelling</em> — "Fullbright: ON",
 * "Max Reach: 20", "Ruby: Hidden" — so the name and the value share one widget. The screen this replaced
 * pinned each label to the far left and its control to the far right, which at any reasonable window
 * width left a gulf between them and made it genuinely hard to tell which toggle belonged to which row.
 * Fusing the two removes the problem rather than mitigating it, and it lets controls sit in narrow
 * columns instead of stretching across the screen.
 * <p>
 * Second, actions are buttons. The old screen expressed "export route" and "clear route" as boolean
 * options that fired when you saved and closed the menu; here they do the thing when clicked, and the two
 * destructive ones ask first by turning into their own confirmation.
 * <p>
 * Everything applies immediately, so there is no Save button — {@link #onClose()} just writes to disk.
 */
@Environment(EnvType.CLIENT)
public final class HollowerConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int MAX_CONTENT_WIDTH = 420;

    private enum Tab {
        ROUTE("Route"),
        APPEARANCE("Appearance"),
        SELECTIVE_RENDER("Selective Render"),
        PLAYER("Player");

        private final String title;

        Tab(String title) {
            this.title = title;
        }
    }

    private final Screen parent;
    private Tab tab = Tab.ROUTE;

    /** Resting labels of the confirm-first buttons, so one can be restored when its confirm lapses. */
    private final Map<Button, String> confirmLabels = new HashMap<>();

    /**
     * The tab's own widgets and the y each sits at when unscrolled. Tab bar and Done are not in here —
     * they stay put. Every build method works in these unscrolled coordinates and {@link #applyScroll()}
     * translates them, so no layout code has to know scrolling exists.
     */
    private final List<Placed> placed = new ArrayList<>();
    private int scroll;

    private record Placed(AbstractWidget widget, int baseY, boolean enabled) {
    }

    /** The keybind control waiting for a keystroke, or null. At most one is ever armed. */
    private KeyBindControl armedKey;
    /** The destructive action awaiting its second click, or null. Cleared whenever anything else moves. */
    private Button pendingConfirm;

    public HollowerConfigScreen(Screen parent) {
        super(Component.literal("Hollower"));
        this.parent = parent;
    }

    // ---------------------------------------------------------------- layout

    private int contentWidth() {
        return Math.min(MAX_CONTENT_WIDTH, width - 40);
    }

    private int contentLeft() {
        return (width - contentWidth()) / 2;
    }

    private int contentTop() {
        return 56;
    }

    private int viewportTop() {
        return 52;
    }

    private int viewportBottom() {
        return height - 34;
    }

    @Override
    protected void init() {
        armedKey = null;
        pendingConfirm = null;
        // The old buttons are gone after a rebuild; keeping their labels would leak an entry per rebuild.
        confirmLabels.clear();
        placed.clear();
        addTabBar();

        switch (tab) {
            case ROUTE -> buildRouteTab();
            case APPEARANCE -> buildAppearanceTab();
            case SELECTIVE_RENDER -> buildSelectiveRenderTab();
            case PLAYER -> buildPlayerTab();
        }

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(contentLeft() + contentWidth() / 2 - 50, height - 28, 100, 20)
                .build());

        applyScroll();
    }

    /** Registers a tab widget at its unscrolled y and adds it to the screen. */
    private <T extends AbstractWidget> T track(T widget, int baseY, boolean enabled) {
        placed.add(new Placed(widget, baseY, enabled));
        return addRenderableWidget(widget);
    }

    private int contentHeight() {
        int bottom = contentTop();
        for (Placed entry : placed) {
            bottom = Math.max(bottom, entry.baseY() + entry.widget().getHeight());
        }
        return bottom - contentTop();
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - (viewportBottom() - viewportTop()));
    }

    /**
     * Moves the tab's widgets by the current offset and hides whatever falls outside the viewport, so a
     * control scrolled under the Done button can neither be seen nor clicked.
     */
    private void applyScroll() {
        scroll = Math.clamp(scroll, 0, maxScroll());
        for (Placed entry : placed) {
            AbstractWidget widget = entry.widget();
            int y = entry.baseY() - scroll;
            widget.setY(y);
            boolean visible = y + widget.getHeight() > viewportTop() && y < viewportBottom();
            widget.visible = visible;
            widget.active = visible && entry.enabled();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll() <= 0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        scroll -= (int) (scrollY * ROW_HEIGHT);
        applyScroll();
        return true;
    }

    private void addTabBar() {
        addRenderableWidget(new StringWidget(
                contentLeft(), 12, contentWidth(), 12,
                Component.literal("Hollower").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                font));

        Tab[] tabs = Tab.values();
        int gap = 2;
        int totalGap = gap * (tabs.length - 1);
        int each = (contentWidth() - totalGap) / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            Tab target = tabs[i];
            boolean current = target == tab;
            // Every tab stays clickable. Marking the current one by disabling its button would have
            // Minecraft grey the label out, which reads as "unavailable" rather than "you are here".
            Button button = Button.builder(
                            current
                                    ? Component.literal("> " + target.title)
                                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                                    : Component.literal(target.title).withStyle(ChatFormatting.WHITE),
                            unused -> selectTab(target))
                    .bounds(contentLeft() + i * (each + gap), 30, each, 20)
                    .build();
            addRenderableWidget(button);
        }
    }

    private void selectTab(Tab target) {
        // A new tab starts at the top; re-picking the current one is a no-op you can click freely.
        if (target != tab) scroll = 0;
        tab = target;
        rebuildWidgets();
    }

    /**
     * Lays widgets out in {@code columns} equal columns, filling left to right then top to bottom.
     * Returns the y of the first free row so a caller can carry on beneath what it just placed.
     */
    private int place(int top, int columns, List<? extends AbstractWidget> widgets) {
        int gap = 4;
        int each = (contentWidth() - gap * (columns - 1)) / columns;
        for (int i = 0; i < widgets.size(); i++) {
            AbstractWidget widget = widgets.get(i);
            widget.setX(contentLeft() + (i % columns) * (each + gap));
            widget.setWidth(each);
            // active is set here rather than read back later, so applyScroll can restore it faithfully.
            track(widget, top + (i / columns) * ROW_HEIGHT, widget.active);
        }
        int rows = (widgets.size() + columns - 1) / columns;
        return top + rows * ROW_HEIGHT;
    }

    private int addHeading(int top, String text) {
        addLabel(top, Component.literal(text).withStyle(ChatFormatting.YELLOW));
        return top + 16;
    }

    private int addNote(int top, String text) {
        addLabel(top, Component.literal(text).withStyle(ChatFormatting.GRAY));
        return top + 12;
    }

    private void addLabel(int top, Component text) {
        track(new StringWidget(contentLeft(), top, contentWidth(), 12, text, font), top, false);
    }

    /**
     * Attaches hover help to a control. Every option gets one: the labels say what a setting is, and the
     * tooltip is where the detail that used to sit in the old menu's descriptions lives.
     */
    private static <T extends AbstractWidget> T tip(T widget, String text) {
        widget.setTooltip(Tooltip.create(Component.literal(text)));
        return widget;
    }

    // ---------------------------------------------------------------- tabs

    private void buildRouteTab() {
        int y = contentTop();

        int nodes = Hollower.positions.size();
        addLabel(y, nodes == 0
                ? Component.literal("No route loaded").withStyle(ChatFormatting.GRAY)
                : Component.literal("Route: " + nodes + (nodes == 1 ? " node" : " nodes"))
                        .withStyle(ChatFormatting.GREEN));
        y += 18;

        y = addHeading(y, "Actions");
        // Three of the four need a route to act on. Saying so in the tooltip beats leaving a greyed
        // button with no explanation for why it will not respond.
        String needsRoute = nodes > 0 ? "" : "\nUnavailable: add some nodes first.";

        List<Button> actions = new ArrayList<>();
        actions.add(tip(action("Export Route", nodes > 0,
                        button -> ClientCompat.setScreen(
                                Minecraft.getInstance(), new RouteExportScreen(this))),
                "Copy this route to your clipboard in Waypointer, SkyHanni or Skyblocker format."
                        + needsRoute));
        actions.add(tip(action("Import From Clipboard", true,
                        button -> {
                            RouteUtils.importRouteFromClipboard();
                            rebuildWidgets();
                        }),
                "Replace the current route with one copied to your clipboard."));
        actions.add(tip(confirmAction("Clear Route", nodes > 0, () -> {
                    RouteUtils.clearRoute();
                    rebuildWidgets();
                }),
                "Delete every node in the route. Asks for a second click first." + needsRoute));
        actions.add(tip(confirmAction("Set Blocks In Route", nodes > 0, RouteUtils::setBlocksInRoute),
                "Place a bedrock block at each node, so the route is visible as real blocks.\n"
                        + "Local worlds only. Asks for a second click first." + needsRoute));
        y = place(y, 2, actions) + 8;

        y = addHeading(y, "Editing");
        y = place(y, 2, List.of(
                tip(ValueSlider.ofInt(0, 0, 0, "Max Reach", 1, 100, Hollower.maxReach,
                                value -> Hollower.maxReach = value),
                        "How far to reach when creating, deleting or selecting route nodes."),
                tip(key("Nudge Key", () -> Hollower.nudgeKey, value -> Hollower.nudgeKey = value),
                        "Hold and scroll to move the selected node towards or away from you."),
                tip(key("Swap Order Key", () -> Hollower.swapOrderKey, value -> Hollower.swapOrderKey = value),
                        "Hold and scroll to rotate the order of every node, changing which one is first.\n"
                                + "Hold and select another node to swap it with the selected one.")));

        y += 6;
        y = addNote(y, "Route hotkeys need a wooden pickaxe in hand.");
        addNote(y, "Add / delete / select use your Use, Attack and Pick Block binds.");
    }

    private void buildAppearanceTab() {
        int y = contentTop();

        y = addHeading(y, "Route Line");
        y = place(y, 2, List.of(
                tip(color("Colour", () -> Hollower.routeLineColor, value -> Hollower.routeLineColor = value),
                        "Colour of the lines connecting one node to the next."),
                tip(ValueSlider.ofFloat(0, 0, 0, "Width", 0.0f, 10.0f, Hollower.routeLineWidth, 1,
                                value -> Hollower.routeLineWidth = value),
                        "Thickness of those lines. Set to 0 to hide them.")));
        y += 8;

        y = addHeading(y, "Block Outline");
        y = place(y, 2, List.of(
                tip(color("Colour", () -> Hollower.outlineBlockColor, value -> Hollower.outlineBlockColor = value),
                        "Colour of the box drawn around each node in the route."),
                tip(ValueSlider.ofFloat(0, 0, 0, "Width", 0.0f, 10.0f, Hollower.outlineBlockWidth, 1,
                                value -> Hollower.outlineBlockWidth = value),
                        "Thickness of those boxes. Set to 0 to hide them.")));
        y += 8;

        y = addHeading(y, "Selection & Labels");
        place(y, 2, List.of(
                tip(color("Selected Block", () -> Hollower.selectBlockColor,
                                value -> Hollower.selectBlockColor = value),
                        "Fill colour of the node you currently have selected."),
                tip(ValueSlider.ofFloat(0, 0, 0, "Order Text Size", 0.0f, 0.2f, Hollower.orderScale, 3,
                                value -> Hollower.orderScale = value),
                        "Size of the position number floating above each node. Set to 0 to hide them.")));
    }

    private void buildSelectiveRenderTab() {
        int y = contentTop();

        y = place(y, 2, List.of(
                tip(key("Toggle Key", Hollower::getToggleRenderKey, Hollower::setToggleRenderKey),
                        "Turns selective render on and off from in game."),
                tip(ToggleButton.create(0, 0, 0, "Selective Render",
                                () -> Hollower.renderToggle,
                                value -> {
                                    Hollower.renderToggle = value;
                                    RenderTweaks.reloadRender();
                                }),
                        "The master switch. While this is off, nothing below has any effect.")));
        y += 4;

        y = place(y, 2, List.of(
                tip(Button.builder(Component.literal("Hide All"), button -> {
                            SelectiveRender.setAll(true);
                            rebuildWidgets();
                        }).bounds(0, 0, 0, 20).build(),
                        "Hide every group at once."),
                tip(Button.builder(Component.literal("Show All"), button -> {
                            SelectiveRender.setAll(false);
                            rebuildWidgets();
                        }).bounds(0, 0, 0, 20).build(),
                        "Bring every group back into view.")));
        y += 8;

        // Three narrow columns: the labels are short, and short rows are what make a toggle grid
        // scannable — the eye never has to travel from a name on the left to a state on the right.
        for (HiddenBlockGroup.Category category : HiddenBlockGroup.Category.values()) {
            List<Button> toggles = new ArrayList<>();
            for (HiddenBlockGroup group : HiddenBlockGroup.values()) {
                if (group.category() == category) toggles.add(groupToggle(group));
            }
            if (toggles.isEmpty()) continue;
            y = addHeading(y, category.title());
            y = place(y, 3, toggles) + 6;
        }

        addNote(y, "Each hidden group adds lag when crossing chunk borders.");
    }

    private void buildPlayerTab() {
        int y = contentTop();

        y = addHeading(y, "Movement");
        y = place(y, 2, List.of(
                tip(key("Noclip Key", Hollower::getNoClipKey, Hollower::setNoClipKey),
                        "Fly through blocks in a local world. Move outside solid blocks before\n"
                                + "turning it back off, or the game will push you out."),
                tip(ToggleButton.create(0, 0, 0, "Fullbright",
                                () -> Hollower.fullBright, value -> Hollower.fullBright = value),
                        "Light local worlds fully, so you can see without torches.")));
        y += 4;
        y = addNote(y, "Noclip works in local worlds only. Sprint to move faster.") + 6;

        y = addHeading(y, "Etherwarp");
        y = place(y, 2, List.of(
                tip(key("Etherwarp Key", () -> Hollower.etherwarpKey, value -> Hollower.etherwarpKey = value),
                        "Hold this, then press Use Item to teleport on top of the block you\n"
                                + "are looking at."),
                tip(ValueSlider.ofInt(0, 0, 0, "Range", 1, 200, Hollower.etherwarpRange,
                                value -> Hollower.etherwarpRange = value),
                        "Furthest an etherwarp will carry you."),
                tip(color("Target Colour", () -> Hollower.etherwarpBlockColor,
                                value -> Hollower.etherwarpBlockColor = value),
                        "Highlight colour of the block you would land on.")));
        y += 8;

        y = addHeading(y, "Menu");
        place(y, 2, List.of(
                tip(key("Open Config Key", Hollower::getConfigKey, Hollower::setConfigKey),
                        "Opens this menu. Also listed under Hollower in Minecraft's Controls.")));
    }

    // ---------------------------------------------------------------- control factories

    private Button action(String label, boolean enabled, Consumer<Button> onPress) {
        Button button = Button.builder(Component.literal(label), pressed -> {
                    // Reaching for a different action means you thought better of the pending confirm.
                    clearPendingConfirm();
                    onPress.accept(pressed);
                })
                .bounds(0, 0, 0, 20)
                .build();
        // Disabled actions still draw, so the grid keeps its shape as a route is loaded or cleared.
        button.active = enabled;
        return button;
    }

    /**
     * A destructive action that asks first: the button becomes its own confirmation, so the second click
     * lands on the same target the first one did rather than on a dialog that appeared under the cursor.
     */
    private Button confirmAction(String label, boolean enabled, Runnable onConfirm) {
        Button[] self = new Button[1];
        self[0] = Button.builder(Component.literal(label), button -> {
                    if (pendingConfirm == button) {
                        pendingConfirm = null;
                        onConfirm.run();
                        return;
                    }
                    clearPendingConfirm();
                    pendingConfirm = button;
                    button.setMessage(Component.literal("Click again to confirm")
                            .withStyle(ChatFormatting.RED));
                })
                .bounds(0, 0, 0, 20)
                .build();
        self[0].active = enabled;
        confirmLabels.put(self[0], label);
        return self[0];
    }

    private void clearPendingConfirm() {
        if (pendingConfirm == null) return;
        String label = confirmLabels.get(pendingConfirm);
        if (label != null) pendingConfirm.setMessage(Component.literal(label));
        pendingConfirm = null;
    }

    private Button key(
            String label,
            Supplier<InputConstants.Key> getter,
            Consumer<InputConstants.Key> setter
    ) {
        KeyBindControl control = new KeyBindControl(label, getter, setter, armed -> {
            if (armedKey != null) armedKey.setArmed(false);
            clearPendingConfirm();
            armedKey = armed;
            armed.setArmed(true);
        });
        return control.widget();
    }

    private Button color(String label, IntSupplier getter, IntConsumer setter) {
        return ColorButton.create(0, 0, 0, label, getter,
                () -> ClientCompat.setScreen(Minecraft.getInstance(),
                        new ColorPickerScreen(this, label, getter.getAsInt(), setter)));
    }

    private Button groupToggle(HiddenBlockGroup group) {
        return tip(Button.builder(groupLabel(group), button -> {
                            SelectiveRender.toggle(group);
                            button.setMessage(groupLabel(group));
                        })
                        .bounds(0, 0, 0, 20)
                        .build(),
                group.help());
    }

    /** The group's own colour for its name, so the grid is scannable by hue as well as by word. */
    private Component groupLabel(HiddenBlockGroup group) {
        boolean hidden = SelectiveRender.isHidden(group);
        return Component.literal(group.label() + ": ")
                .withStyle(Style.EMPTY.withColor(group.accent()))
                .append(Component.literal(hidden ? "Hidden" : "Shown")
                        .withStyle(hidden ? ChatFormatting.RED : ChatFormatting.GRAY));
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean keyPressed(KeyEvent event) {
        int pressed = event.key();
        if (armedKey != null) {
            // Escape backs out of the capture without rebinding, so a mis-click is recoverable.
            if (pressed != GLFW.GLFW_KEY_ESCAPE) {
                armedKey.bind(Hollower.key(pressed));
            } else {
                armedKey.setArmed(false);
            }
            armedKey = null;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        HollowerConfig.save();
        ClientCompat.setScreen(Minecraft.getInstance(), parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
