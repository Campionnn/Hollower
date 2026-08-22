package com.hollower.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.List;

/**
 * A group of blocks that selective render can hide as a unit.
 * <p>
 * Each constant carries its display name and an accent colour matched to the block it hides, alongside
 * the block ids that do the hiding. Ids are stored as bare names and expanded to
 * {@code block.minecraft.<name>} by {@link #blockIds()}, because that is the form the blacklist is keyed
 * by: {@code RenderTweaks.findBlocksChunk} looks up {@code getDescriptionId().hashCode()}.
 * <p>
 * Adding a group is a matter of adding a constant. The screen enumerates {@link #values()} and
 * {@link SelectiveRender} applies whatever it finds, so neither needs touching.
 */
@Environment(EnvType.CLIENT)
public enum HiddenBlockGroup {
    RUBY(Category.GEMSTONE, "Ruby", 0xFF5555,
            "red_stained_glass", "red_stained_glass_pane"),
    TOPAZ(Category.GEMSTONE, "Topaz", 0xFFDD33,
            "yellow_stained_glass", "yellow_stained_glass_pane"),
    SAPPHIRE(Category.GEMSTONE, "Sapphire", 0x55BBFF,
            "light_blue_stained_glass", "light_blue_stained_glass_pane"),
    AMETHYST(Category.GEMSTONE, "Amethyst", 0xAA66FF,
            "purple_stained_glass", "purple_stained_glass_pane"),
    JADE(Category.GEMSTONE, "Jade", 0x55FF55,
            "lime_stained_glass", "lime_stained_glass_pane"),
    AMBER(Category.GEMSTONE, "Amber", 0xFFAA33,
            "orange_stained_glass", "orange_stained_glass_pane"),

    MITHRIL(Category.ORE, "Mithril", 0x55FFDD,
            "light_blue_wool", "prismarine", "prismarine_bricks", "dark_prismarine"),
    COAL(Category.ORE, "Coal", 0x999999, "coal_ore"),
    IRON(Category.ORE, "Iron", 0xDDAA88, "iron_ore"),
    REDSTONE(Category.ORE, "Redstone", 0xFF4444, "redstone_ore"),
    GOLD(Category.ORE, "Gold", 0xFFCC33, "gold_ore"),
    LAPIS(Category.ORE, "Lapis", 0x6688EE, "lapis_ore"),
    DIAMOND(Category.ORE, "Diamond", 0x55FFFF, "diamond_ore"),
    EMERALD(Category.ORE, "Emerald", 0x33DD77, "emerald_ore"),

    MISC(Category.OTHER, "Everything else", 0xAAAAAA, MiscBlocks.NAMES);

    /** How the groups are banded in the config screen. */
    public enum Category {
        GEMSTONE("Gemstones"),
        ORE("Ores"),
        OTHER("Terrain & decoration");

        private final String title;

        Category(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }

    private final Category category;
    private final String label;
    private final int accent;
    private final List<String> blockNames;

    HiddenBlockGroup(Category category, String label, int accent, String... blockNames) {
        this(category, label, accent, List.of(blockNames));
    }

    HiddenBlockGroup(Category category, String label, int accent, List<String> blockNames) {
        this.category = category;
        this.label = label;
        this.accent = accent;
        this.blockNames = blockNames;
    }

    public Category category() {
        return category;
    }

    public String label() {
        return label;
    }

    /** RGB used for this group's name in the GUI, chosen to match the block it hides. */
    public int accent() {
        return accent;
    }

    /** The translation keys this group hides, in the {@code block.minecraft.*} form the blacklist uses. */
    public List<String> blockIds() {
        return blockNames.stream().map(name -> "block.minecraft." + name).toList();
    }

    /**
     * Hover help for the group's toggle. Derived from the category rather than stored per constant —
     * every gemstone and every ore says the same thing about itself, so writing it out fifteen times
     * would only create fifteen chances to let one drift.
     */
    public String help() {
        return switch (category) {
            case GEMSTONE -> "Hides " + label + " gemstone crystals, in both block and pane form.";
            case ORE -> "Hides " + label + " ore wherever it generates.";
            case OTHER -> "Hides the terrain and decoration the Hollows generate,\n"
                    + "leaving gemstones and ores in an empty space.";
        };
    }
}
