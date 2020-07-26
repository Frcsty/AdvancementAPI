package com.github.frcsty.advancementapi.wrapper;

import org.bukkit.entity.Player;

import java.util.Arrays;

public abstract class AdvancementVisibility {

    public static final AdvancementVisibility ALWAYS = new AdvancementVisibility("ALWAYS") {

        @Override
        public boolean isVisible(final Player player, final Advancement advancement) {
            return true;
        }
    }, PARENT_GRANTED = new AdvancementVisibility("PARENT_GRANTED") {

        @Override
        public boolean isVisible(final Player player, final Advancement advancement) {
            if (advancement.getProgressComponent().isGranted(player)) return true;
            final Advancement parent = advancement.getParent();

            return parent == null || parent.getProgressComponent().isGranted(player);
        }
    }, VANILLA = new AdvancementVisibility("VANILLA") {

        @Override
        public boolean isVisible(final Player player, final Advancement advancement) {
            if (advancement.getProgressComponent().isGranted(player)) return true;

            final Advancement parent = advancement.getParent();

            if (parent != null && !parent.getProgressComponent().isGranted(player)) {
                final Advancement grandParent = parent.getParent();

                return grandParent == null || grandParent.getParent() == null || grandParent.getProgressComponent().isGranted(player);
            }

            return true;
        }
    }, HIDDEN = new AdvancementVisibility("HIDDEN") {

        @Override
        public boolean isVisible(final Player player, final Advancement advancement) {
            return advancement.getProgressComponent().isGranted(player);
        }
    };

    private final String name;
    public AdvancementVisibility() {
        name = "CUSTOM";
    }

    private AdvancementVisibility(final String name) {
        this.name = name;
    }

    /**
     * Parses a visibility
     *
     * @param name Visibility Name
     * @return A visibility with a matching {@link #getName()} or {@link #VANILLA}
     */
    public static AdvancementVisibility parseVisibility(final String name) {
        for (final AdvancementVisibility visibility : Arrays.asList(ALWAYS, PARENT_GRANTED, VANILLA, HIDDEN)) {
            if (visibility.getName().equalsIgnoreCase(name)) {
                return visibility;
            }
        }
        return VANILLA;
    }

    /**
     * @param player      Player to check
     * @param advancement Advancement to check
     * @return true if advancement should be visible
     */
    public abstract boolean isVisible(final Player player, final Advancement advancement);

    /**
     * @return true if advancement should always be visible if any child should be visible, defaults to true
     */
    public boolean isAlwaysVisibleWhenAdvancementAfterIsVisible() {
        return true;
    }

    /**
     * @return Custom Name, only for pre-defined visibilities: {@link #ALWAYS}, {@link #PARENT_GRANTED}, {@link #VANILLA}, {@link #HIDDEN}
     */
    public String getName() {
        return name;
    }

}
