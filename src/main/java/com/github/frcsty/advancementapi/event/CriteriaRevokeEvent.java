package com.github.frcsty.advancementapi.event;

import com.github.frcsty.advancementapi.manager.AdvancementManager;
import com.github.frcsty.advancementapi.wrapper.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class CriteriaRevokeEvent extends Event {

    public static final HandlerList handlers = new HandlerList();
    private final AdvancementManager manager;
    private final Advancement advancement;
    private final String[] criteria;
    private final Player player;
    public CriteriaRevokeEvent(final AdvancementManager manager,
                               final Advancement advancement,
                               final String[] criteria,
                               final Player player) {
        this.manager = manager;
        this.advancement = advancement;
        this.criteria = criteria;
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
     * @return Reciever
     */
    public Player getPlayer() {
        return player;
    }

}
