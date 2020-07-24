package com.github.frcsty.advancementapi.event.offline;

import com.github.frcsty.advancementapi.manager.AdvancementManager;
import com.github.frcsty.advancementapi.wrapper.Advancement;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public final class OfflineCriteriaRevokeEvent extends Event {

    public static final HandlerList handlers = new HandlerList();
    private final AdvancementManager manager;
    private final Advancement advancement;
    private final String[] criteria;
    private final UUID uuid;

    public OfflineCriteriaRevokeEvent(final AdvancementManager manager,
                                      final Advancement advancement,
                                      final String[] criteria,
                                      final UUID uuid) {
        this.manager = manager;
        this.advancement = advancement;
        this.criteria = criteria;
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
     * @return Advancement
     */
    public Advancement getAdvancement() {
        return advancement;
    }

    /**
     * @return Revoked Criteria
     */
    public String[] getCriteria() {
        return criteria;
    }

    /**
     * @return Receiver
     */
    public UUID getUuid() {
        return uuid;
    }

}
