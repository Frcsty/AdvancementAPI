package com.github.frcsty.advancementapi.event.offline;

import com.github.frcsty.advancementapi.manager.AdvancementManager;
import com.github.frcsty.advancementapi.wrapper.Advancement;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public final class OfflineCriteriaProgressChangeEvent extends Event {

    public static final HandlerList handlers = new HandlerList();
    private final AdvancementManager manager;
    private final Advancement advancement;
    private final UUID uuid;
    private final int progressBefore;
    private int progress;

    public OfflineCriteriaProgressChangeEvent(final AdvancementManager manager,
                                              final Advancement advancement,
                                              final UUID uuid,
                                              final int progressBefore,
                                              final int progress) {
        this.manager = manager;
        this.advancement = advancement;
        this.uuid = uuid;
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
     * @return Reciever UUID
     */
    public UUID getUUID() {
        return uuid;
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
