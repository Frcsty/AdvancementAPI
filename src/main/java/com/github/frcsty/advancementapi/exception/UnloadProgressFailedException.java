package com.github.frcsty.advancementapi.exception;

import java.util.UUID;

public final class UnloadProgressFailedException extends RuntimeException {

    private static final long serialVersionUID = 5052062325162108824L;

    private final UUID uuid;
    private String message = "Unable to unload Progress for online Players!";

    public UnloadProgressFailedException(final UUID uuid) {
        this.uuid = uuid;
    }

    public UnloadProgressFailedException(final UUID uuid, final String message) {
        this.uuid = uuid;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return "Failed to unload Advancement Progress for Player with UUID " + uuid + ": " + message;
    }


}
