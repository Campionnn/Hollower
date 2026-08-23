package com.hollower.screen;

import com.hollower.Hollower;
import com.hollower.utils.ClientCompat;
import com.hollower.utils.RouteStorage;
import com.hollower.utils.RouteUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Browses the saved-route library: navigate nested folders, save the current route into one, load, rename,
// delete or move any entry. Reused as its own destination picker while moving something, by staying in
// normal browse mode and swapping the footer for a "Move here" / "Cancel move" bar.
@Environment(EnvType.CLIENT)
public final class RouteManagerScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int MAX_CONTENT_WIDTH = 420;

    private final Screen parent;
    private final Path folder;

    // Set while an entry is being relocated; browsing continues underneath it in normal mode.
    private final RouteStorage.Entry moving;

    private final List<Placed> placed = new ArrayList<>();
    private int scroll;
    private int rowCount;

    private record Placed(AbstractWidget widget, int baseY, boolean enabled) {
    }

    private final Map<Button, String> confirmLabels = new HashMap<>();
    private Button pendingConfirm;

    private String errorMessage;

    public RouteManagerScreen(Screen parent) {
        this(parent, Path.of(""), null);
    }

    private RouteManagerScreen(Screen parent, Path folder, RouteStorage.Entry moving) {
        super(Component.literal("Routes"));
        this.parent = parent;
        this.folder = folder;
        this.moving = moving;
    }

    // ---------------------------------------------------------------- layout

    private int contentWidth() {
        return Math.min(MAX_CONTENT_WIDTH, width - 40);
    }

    private int contentLeft() {
        return (width - contentWidth()) / 2;
    }

    private int viewportTop() {
        return errorMessage == null ? 60 : 74;
    }

    private int viewportBottom() {
        return height - 60;
    }

    @Override
    protected void init() {
        pendingConfirm = null;
        confirmLabels.clear();
        placed.clear();
        rowCount = 0;

        addRenderableWidget(new StringWidget(
                contentLeft(), 8, contentWidth(), 12,
                Component.literal("Routes").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), font));

        int breadcrumbWidth = contentWidth() - 50;
        addRenderableWidget(new StringWidget(
                contentLeft(), 24, breadcrumbWidth, 12,
                Component.literal("/" + folder.toString().replace('\\', '/')).withStyle(ChatFormatting.GRAY), font));
        Button up = Button.builder(Component.literal("Up"), button -> navigateTo(parentFolder()))
                .bounds(contentLeft() + breadcrumbWidth, 20, 50, 20)
                .build();
        up.active = folder.getNameCount() > 0;
        addRenderableWidget(up);

        if (errorMessage != null) {
            addRenderableWidget(new StringWidget(
                    contentLeft(), 42, contentWidth(), 12,
                    Component.literal(errorMessage).withStyle(ChatFormatting.RED), font));
        }

        for (RouteStorage.Entry entry : RouteStorage.list(folder)) {
            addEntryRow(entry);
        }

        addFooter();
        applyScroll();
    }

    private void track(AbstractWidget widget, int baseY) {
        placed.add(new Placed(widget, baseY, widget.active));
        addRenderableWidget(widget);
    }

    private int contentHeight() {
        int bottom = viewportTop();
        for (Placed entry : placed) {
            bottom = Math.max(bottom, entry.baseY() + entry.widget().getHeight());
        }
        return bottom - viewportTop();
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - (viewportBottom() - viewportTop()));
    }

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

    // ---------------------------------------------------------------- rows

    private void addEntryRow(RouteStorage.Entry entry) {
        int y = nextRowY();

        boolean isMoving = moving != null && moving.path().equals(entry.path());
        int gap = 4;
        int primaryWidth = (contentWidth() - gap * 3) * 4 / 10;
        int actionWidth = (contentWidth() - primaryWidth - gap * 3) / 3;
        int x = contentLeft();

        String label = (entry.folder() ? "[Folder] " : "") + entry.name();
        Button primary = Button.builder(Component.literal(label), button -> {
                    clearPendingConfirm();
                    if (entry.folder()) {
                        navigateTo(entry.path());
                    } else {
                        loadRoute(entry);
                    }
                })
                .bounds(x, y, primaryWidth, 20)
                .build();
        primary.active = !isMoving;
        track(primary, y);
        x += primaryWidth + gap;

        Button rename = Button.builder(Component.literal("Rename"), button ->
                        ClientCompat.setScreen(Minecraft.getInstance(),
                                new NameInputScreen(this, "Rename \"" + entry.name() + "\"", entry.name(),
                                        newName -> renameEntry(entry, newName))))
                .bounds(x, y, actionWidth, 20)
                .build();
        rename.active = !isMoving;
        track(rename, y);
        x += actionWidth + gap;

        Button delete = confirmAction("Delete", !isMoving, () -> deleteEntry(entry));
        delete.setX(x);
        delete.setY(y);
        delete.setWidth(actionWidth);
        track(delete, y);
        x += actionWidth + gap;

        Button move = Button.builder(Component.literal(isMoving ? "Moving" : "Move"),
                        button -> ClientCompat.setScreen(Minecraft.getInstance(),
                                new RouteManagerScreen(parent, folder, entry)))
                .bounds(x, y, actionWidth, 20)
                .build();
        move.active = moving == null;
        track(move, y);
    }

    private int nextRowY() {
        return viewportTop() + (rowCount++) * ROW_HEIGHT;
    }

    private void addFooter() {
        int footerTop = height - 52;
        int half = (contentWidth() - 4) / 2;

        if (moving != null) {
            addRenderableWidget(Button.builder(
                            Component.literal("Move \"" + moving.name() + "\" here"),
                            button -> performMove())
                    .bounds(contentLeft(), footerTop, contentWidth(), 20)
                    .build());
            addRenderableWidget(Button.builder(Component.literal("Cancel Move"), button ->
                            ClientCompat.setScreen(Minecraft.getInstance(), new RouteManagerScreen(parent, folder, null)))
                    .bounds(contentLeft(), footerTop + 24, contentWidth(), 20)
                    .build());
            return;
        }

        boolean hasRoute = !Hollower.positions.isEmpty();
        Button newFolder = Button.builder(Component.literal("New Folder"), button ->
                        ClientCompat.setScreen(Minecraft.getInstance(),
                                new NameInputScreen(this, "New folder name", "", this::createFolder)))
                .bounds(contentLeft(), footerTop, half, 20)
                .build();
        addRenderableWidget(newFolder);

        Button saveHere = Button.builder(Component.literal("Save Route Here"), button ->
                        ClientCompat.setScreen(Minecraft.getInstance(),
                                new NameInputScreen(this, "Route name", "", this::saveRoute)))
                .bounds(contentLeft() + half + 4, footerTop, contentWidth() - half - 4, 20)
                .build();
        saveHere.active = hasRoute;
        addRenderableWidget(saveHere);

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(contentLeft() + contentWidth() / 2 - 50, footerTop + 24, 100, 20)
                .build());
    }

    // ---------------------------------------------------------------- actions

    private void navigateTo(Path target) {
        scroll = 0;
        ClientCompat.setScreen(Minecraft.getInstance(),
                new RouteManagerScreen(parent, target == null ? Path.of("") : target, moving));
    }

    // folder.getParent() is unreliable here: for a *relative* path with exactly one element, Java's
    // Path.getParent() returns null just like it does for the empty root path, so it can't tell "one level
    // deep" apart from "already at the root". Name-count arithmetic avoids that ambiguity.
    private Path parentFolder() {
        int count = folder.getNameCount();
        return count <= 1 ? Path.of("") : folder.subpath(0, count - 1);
    }

    private void loadRoute(RouteStorage.Entry entry) {
        try {
            RouteUtils.loadRoute(RouteStorage.loadRoute(entry.path()));
            onClose();
        } catch (RouteStorage.RouteStorageException e) {
            showError(e.getMessage());
        }
    }

    private void createFolder(String name) {
        try {
            RouteStorage.createFolder(folder, name);
            refresh(null);
        } catch (RouteStorage.RouteStorageException e) {
            refresh(e.getMessage());
        }
    }

    private void saveRoute(String name) {
        try {
            RouteStorage.saveRoute(folder, name, Hollower.positions);
            Hollower.sendChatMessage("Route '" + name + "' saved");
            refresh(null);
        } catch (RouteStorage.RouteStorageException e) {
            refresh(e.getMessage());
        }
    }

    private void renameEntry(RouteStorage.Entry entry, String newName) {
        try {
            if (entry.folder()) {
                RouteStorage.renameFolder(entry.path(), newName);
            } else {
                RouteStorage.renameRoute(entry.path(), newName);
            }
            refresh(null);
        } catch (RouteStorage.RouteStorageException e) {
            refresh(e.getMessage());
        }
    }

    private void deleteEntry(RouteStorage.Entry entry) {
        try {
            if (entry.folder()) {
                RouteStorage.deleteFolder(entry.path());
            } else {
                RouteStorage.deleteRoute(entry.path());
            }
            refresh(null);
        } catch (RouteStorage.RouteStorageException e) {
            refresh(e.getMessage());
        }
    }

    private void performMove() {
        try {
            if (moving.folder()) {
                RouteStorage.moveFolder(moving.path(), folder);
            } else {
                RouteStorage.moveRoute(moving.path(), folder);
            }
            ClientCompat.setScreen(Minecraft.getInstance(), new RouteManagerScreen(parent, folder, null));
        } catch (RouteStorage.RouteStorageException e) {
            showError(e.getMessage());
        }
    }

    // Re-shows this same folder, refreshed from disk, after a mutation.
    private void refresh(String error) {
        RouteManagerScreen next = new RouteManagerScreen(parent, folder, moving);
        next.errorMessage = error;
        ClientCompat.setScreen(Minecraft.getInstance(), next);
    }

    private void showError(String error) {
        errorMessage = error;
        rebuildWidgets();
    }

    // A destructive action that asks first by turning the button itself into its own confirmation.
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
                    button.setMessage(Component.literal("Confirm?").withStyle(ChatFormatting.RED));
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

    @Override
    public void onClose() {
        ClientCompat.setScreen(Minecraft.getInstance(), parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
