package com.github.frcsty.advancementapi.event.offline;

import com.github.frcsty.advancementapi.manager.AdvancementManager;
import com.github.frcsty.advancementapi.wrapper.Advancement;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public final class OfflineAdvancementRevokeEvent extends Event {

    public static final HandlerList handlers = new HandlerList();
    private final AdvancementManager manager;
    private final Advancement advancement;
    private final UUID uuid;
    public OfflineAdvancementRevokeEvent(final AdvancementManager manager,
                                         final Advancement advancement,
                                         final UUID uuid) {
        this.manager = manager;
        this.advancement = advancement;
        this.uuid = uuid;
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
     * @return Reciever UUID
     */
    public UUID getUUID() {
        return uuid;
    }


}
