package com.github.frcsty.advancementapi.event;

import com.github.frcsty.advancementapi.manager.AdvancementManager;
import com.github.frcsty.advancementapi.wrapper.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class AdvancementRevokeEvent extends Event {

    public static final HandlerList handlers = new HandlerList();
    private final AdvancementManager manager;
    private final Advancement advancement;
    private final Player player;

    public AdvancementRevokeEvent(final AdvancementManager advancementManager_v2,
                                  final Advancement advancement,
                                  final Player player) {
        this.manager = advancementManager_v2;
        this.advancement = advancement;
        this.player = player;
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
     * @return The Advancement that has been revoked
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

}