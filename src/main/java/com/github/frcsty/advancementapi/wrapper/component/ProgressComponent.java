package com.github.frcsty.advancementapi.wrapper.component;

import net.minecraft.server.v1_16_R1.AdvancementProgress;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ProgressComponent {

    private Map<String, AdvancementProgress> progress = new HashMap<>();

    public AdvancementProgress getProgress(final Player player) {
        if (this.progress == null) progress = new HashMap<>();
        return this.progress.get(player.getUniqueId().toString()) != null ? this.progress.get(player.getUniqueId().toString()) : new AdvancementProgress();
    }

    public AdvancementProgress getProgress(final UUID uuid) {
        if (this.progress == null) progress = new HashMap<>();
        return this.progress.get(uuid.toString()) != null ? this.progress.get(uuid.toString()) : new AdvancementProgress();
    }

    public void setProgress(final Player player, final AdvancementProgress progress) {
        if (this.progress == null) this.progress = new HashMap<>();
        this.progress.put(player.getUniqueId().toString(), progress);
    }

    public void unsetProgress(final UUID uuid) {
        if (this.progress == null) this.progress = new HashMap<>();
        this.progress.remove(uuid.toString());
    }

    public boolean isDone(final Player player) {
        return getProgress(player).isDone();
    }

    public boolean isDone(final UUID uuid) {
        return getProgress(uuid).isDone();
    }

    public boolean isGranted(final Player player) {
        return getProgress(player).isDone();
    }

}
