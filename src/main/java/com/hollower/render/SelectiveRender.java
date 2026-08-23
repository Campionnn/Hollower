package com.hollower.render;

import com.hollower.Hollower;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Tracks which HiddenBlockGroups are hidden, and answers the two questions the rest of the mod asks
// about a block: should it be drawn (isHidden) and should the player pass through it (isPassable).
// Hiding is done by changing what a block answers about itself in MixinBlockStateBase, not by editing
// the world, so it works under any chunk mesher including Sodium's.
@Environment(EnvType.CLIENT)
public final class SelectiveRender {
    private static final EnumSet<HiddenBlockGroup> hidden = EnumSet.noneOf(HiddenBlockGroup.class);

    // Immutable snapshot; swapped, never mutated, so mesher threads can read it without locking.
    private static volatile Set<Block> blocks = Set.of();
    private static volatile boolean renderActive;
    private static volatile boolean passThroughActive;

    private SelectiveRender() {
    }

    public static void initialize() {
        // passThrough depends on being in a local world, so it has to be re-derived every tick.
        ClientTickEvents.END_CLIENT_TICK.register(client ->
                passThroughActive = renderActive && client.hasSingleplayerServer());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> setEnabled(false));
    }

    // True when this block should be invisible and stop occluding. Called on every mesh rebuild and
    // face-culling test, so it stays cheap.
    public static boolean isHidden(BlockState state) {
        return renderActive && blocks.contains(state.getBlock());
    }

    // True when the player should pass straight through this block. Only ever true in a local world.
    public static boolean isPassable(BlockState state) {
        return passThroughActive && blocks.contains(state.getBlock());
    }

    public static boolean isHidden(HiddenBlockGroup group) {
        return hidden.contains(group);
    }

    public static void setHidden(HiddenBlockGroup group, boolean value) {
        if (value) {
            hidden.add(group);
        } else {
            hidden.remove(group);
        }
        apply();
    }

    public static void toggle(HiddenBlockGroup group) {
        setHidden(group, !isHidden(group));
    }

    public static void setAll(boolean value) {
        hidden.clear();
        if (value) {
            hidden.addAll(EnumSet.allOf(HiddenBlockGroup.class));
        }
        apply();
    }

    // The master switch. Flipping it re-meshes the loaded sections; nothing else changes.
    public static void setEnabled(boolean value) {
        if (Hollower.renderToggle == value) return;
        Hollower.renderToggle = value;
        refreshFlags();
        markDirty();
    }

    public static int hiddenCount() {
        return hidden.size();
    }

    // How many block types the enabled groups actually resolved to. Zero means nothing will hide.
    public static int blockCount() {
        return blocks.size();
    }

    // Enum names of the hidden groups, for HollowerConfig to persist.
    public static List<String> saveState() {
        return hidden.stream().map(Enum::name).toList();
    }

    // Restores a persisted set, skipping any name that no longer matches a group.
    public static void loadState(List<String> names) {
        hidden.clear();
        if (names == null) return;
        for (String name : names) {
            for (HiddenBlockGroup group : HiddenBlockGroup.values()) {
                if (group.name().equals(name)) {
                    hidden.add(group);
                    break;
                }
            }
        }
    }

    // Rebuilds the block set from the enabled groups, re-meshing only if it actually changed.
    public static void apply() {
        Set<Block> wanted = new HashSet<>();
        for (HiddenBlockGroup group : hidden) {
            for (String name : group.blockNames()) {
                // Unknown ids are skipped rather than fatal, since some only exist in certain versions.
                BuiltInRegistries.BLOCK.getOptional(Identifier.withDefaultNamespace(name))
                        .ifPresent(wanted::add);
            }
        }

        if (wanted.equals(blocks)) return;

        blocks = Set.copyOf(wanted);
        refreshFlags();
        markDirty();
    }

    private static void refreshFlags() {
        boolean active = Hollower.renderToggle && !blocks.isEmpty();
        renderActive = active;
        passThroughActive = active && Minecraft.getInstance().hasSingleplayerServer();
    }

    // Queues a rebuild of every loaded section. Paid once per toggle, not per chunk.
    private static void markDirty() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            ClientLevel level = client.level;
            LocalPlayer player = client.player;
            if (level == null || player == null) return;

            int radius = client.options.getEffectiveRenderDistance() + 1;
            int sectionX = SectionPos.blockToSectionCoord(player.getBlockX());
            int sectionZ = SectionPos.blockToSectionCoord(player.getBlockZ());
            level.setSectionRangeDirty(
                    sectionX - radius, level.getMinSectionY(), sectionZ - radius,
                    sectionX + radius, level.getMaxSectionY(), sectionZ + radius);
        });
    }
}
