package com.github.frcsty.advancementapi.wrapper;

import net.minecraft.server.v1_16_R1.IChatBaseComponent;

@SuppressWarnings("unused")
public final class JSONMessage {

    private final String json;

    public JSONMessage(final String json) {
        this.json = json;
    }

    public String getJson() {
        return json;
    }

    public IChatBaseComponent getBaseComponent() {
        return IChatBaseComponent.ChatSerializer.a(json);
    }

    @Override
    public String toString() {
        return json;
    }

}
