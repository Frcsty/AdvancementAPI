package com.github.frcsty.advancementapi.wrapper;

import net.minecraft.server.v1_16_R1.IChatBaseComponent;

@SuppressWarnings("unused")
public final class JSONMessage {

    private final String json;

    /**
     * @param json A JSON representation of an in-game Message {@link <a href="https://github.com/skylinerw/guides/blob/master/java/text%20component.md">Read More</a>}
     */
    public JSONMessage(final String json) {
        this.json = json;
    }

    /**
     * @return the JSON representation of an in-game Message
     */
    public String getJson() {
        return json;
    }

    /**
     * @return An {@link IChatBaseComponent} representation of an in-game Message
     */
    public IChatBaseComponent getBaseComponent() {
        return IChatBaseComponent.ChatSerializer.a(json);
    }

    @Override
    public String toString() {
        return json;
    }

}
