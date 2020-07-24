package com.github.frcsty.advancementapi.event;

import com.github.frcsty.advancementapi.manager.AdvancementManager;
import com.github.frcsty.advancementapi.wrapper.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class CriteriaProgressChangeEvent extends Event {

    public static final HandlerList handlers = new HandlerList();
    private final AdvancementManager manager;
    private final Advancement advancement;
    private final Player player;
    private final int progressBefore;
    private int progress;
    public CriteriaProgressChangeEvent(final AdvancementManager manager,
                                       final Advancement advancement,
                                       final Player player,
                                       final int progressBefore,
                                       final int progress) {
        this.manager = manager;
        this.advancement = advancement;
        this.player = player;
        this.progressBefore = progressBefore;
        this.progress = progress;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /**
     * @return The Manager this event has been fired from
     */
    public AdvancementManager getManager() {
        return manager;
    }

    /**
     * @return The Advancement that has been granted
     */
    public Advancement getAdvancement() {
        return advancement;
    }

    /**
     * @return Reciever
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The progress before it has been changed
     */
    public int getProgressBefore() {
        return progressBefore;
    }

    /**
     * @return The new progress
     */
    public int getProgress() {
        return progress;
    }

    /**
     * Sets the progress
     *
     * @param progress The new progress
     */
    public void setProgress(final int progress) {
        this.progress = progress;
    }

}
