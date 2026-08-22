package com.hollower.render;

import com.hollower.Hollower;
import com.hollower.utils.RenderTweaks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Which {@link HiddenBlockGroup}s are currently hidden, and the one place that pushes them into
 * {@link Hollower#renderBlacklistID}.
 * <p>
 * The blacklist is rebuilt wholesale from the enabled set rather than patched group by group, so a group
 * can be added or its block list edited without anyone having to write the matching removal. A chunk
 * refresh is expensive, so {@link #apply()} does one only when the resulting id set actually differs from
 * what is already loaded.
 */
@Environment(EnvType.CLIENT)
public final class SelectiveRender {
    private static final EnumSet<HiddenBlockGroup> hidden = EnumSet.noneOf(HiddenBlockGroup.class);

    private SelectiveRender() {
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

    public static int hiddenCount() {
        return hidden.size();
    }

    /** Enum names of the hidden groups, for {@code HollowerConfig} to persist. */
    public static List<String> saveState() {
        return hidden.stream().map(Enum::name).toList();
    }

    /**
     * Restores a persisted set. Unknown names are skipped rather than fatal, so a config written by a
     * newer build (or one whose group was renamed) still loads the groups it can.
     */
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

    /** Rebuilds the blacklist from the enabled groups, refreshing loaded chunks only if it changed. */
    public static void apply() {
        Set<String> wanted = new HashSet<>();
        for (HiddenBlockGroup group : hidden) {
            wanted.addAll(group.blockIds());
        }

        if (wanted.equals(new HashSet<>(Hollower.renderBlacklistID.values()))) return;

        Hollower.renderBlacklistID.clear();
        for (String id : wanted) {
            Hollower.renderBlacklistID.put(id.hashCode(), id);
        }
        RenderTweaks.refreshRender();
    }
}
