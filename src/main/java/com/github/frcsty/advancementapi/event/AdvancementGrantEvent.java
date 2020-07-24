package com.github.frcsty.advancementapi.event;

import com.github.frcsty.advancementapi.manager.AdvancementManager;
import com.github.frcsty.advancementapi.wrapper.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class AdvancementGrantEvent extends Event {

    public static final HandlerList handlers = new HandlerList();
    private final AdvancementManager manager;
    private final Advancement advancement;
    private final Player player;
    private boolean displayMessage;
    public AdvancementGrantEvent(final AdvancementManager manager,
                                 final Advancement advancement,
                                 final Player player,
                                 final boolean displayMessage) {
        this.manager = manager;
        this.advancement = advancement;
        this.player = player;
        this.displayMessage = displayMessage;
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
     * @return Receiver
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return true if a message will be displayed
     */
    public boolean isDisplayMessage() {
        return displayMessage;
    }

    /**
     * Sets if a message will be displayed
     *
     * @param displayMessage message status
     */
    public void setDisplayMessage(final boolean displayMessage) {
        this.displayMessage = displayMessage;
    }


}
