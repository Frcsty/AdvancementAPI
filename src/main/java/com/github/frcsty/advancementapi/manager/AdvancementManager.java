package com.github.frcsty.advancementapi.manager;

import com.github.frcsty.advancementapi.AdvancementAPI;
import com.github.frcsty.advancementapi.event.AdvancementGrantEvent;
import com.github.frcsty.advancementapi.event.AdvancementRevokeEvent;
import com.github.frcsty.advancementapi.event.CriteriaGrantEvent;
import com.github.frcsty.advancementapi.event.CriteriaProgressChangeEvent;
import com.github.frcsty.advancementapi.event.offline.OfflineAdvancementGrantEvent;
import com.github.frcsty.advancementapi.event.offline.OfflineAdvancementRevokeEvent;
import com.github.frcsty.advancementapi.event.offline.OfflineCriteriaGrantEvent;
import com.github.frcsty.advancementapi.event.offline.OfflineCriteriaProgressChangeEvent;
import com.github.frcsty.advancementapi.exception.UnloadProgressFailedException;
import com.github.frcsty.advancementapi.wrapper.Advancement;
import com.github.frcsty.advancementapi.wrapper.NameKey;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.v1_16_R1.*;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;

public final class AdvancementManager {

    private static Map<String, AdvancementManager> accessible = new HashMap<>();
    private static Map<NameKey, Float> smallestY = new HashMap<>();
    private static Map<NameKey, Float> smallestX = new HashMap<>();
    private static Gson gson;
    private static Type progressListType;
    private boolean hiddenBoolean = false;
    private boolean announceAdvancementMessages = true;
    private List<Player> players;
    private List<Advancement> advancements = new ArrayList<>();

    private AdvancementManager() {
        players = new ArrayList<>();
    }

    /**
     * Gets an accessible Advancement Manager by it's Name
     *
     * @param name
     * @return
     */
    public static AdvancementManager getAccessibleManager(final String name) {
        return accessible.getOrDefault(name.toLowerCase(), null);
    }

    public static Collection<AdvancementManager> getAccessibleManagers() {
        return accessible.values();
    }

    private static float getSmallestY(final NameKey key) {
        return smallestY.containsKey(key) ? smallestY.get(key) : 0;
    }

    private static float getSmallestX(final NameKey key) {
        return smallestX.containsKey(key) ? smallestX.get(key) : 0;
    }

    /**
     * Creates a new instance of an advancement manager
     *
     * @param players All players that should be in the new manager from the start, can be changed at any time
     * @return the generated advancement manager
     */
    public static AdvancementManager getNewAdvancementManager(final Player... players) {
        final AdvancementManager manager = new AdvancementManager();
        for (final Player player : players) {
            manager.addPlayer(player);
        }
        return manager;
    }

    private static void check() {
        if (gson == null) {
            gson = new Gson();
        }
        if (progressListType == null) {
            progressListType = new TypeToken<Map<String, List<String>>>() {
                private static final long serialVersionUID = 5832697137241815078L;
            }.getType();
        }
    }

    /**
     * @return All players that have been added to the manager
     */
    public List<Player> getPlayers() {
        players.removeIf(player -> player == null || !player.isOnline());
        return players;
    }

    /**
     * Adds a player to the manager
     *
     * @param player Player to add
     */
    public void addPlayer(final Player player) {
        Validate.notNull(player);
        addPlayer(player, null);
    }

    private void addPlayer(final Player player, final NameKey tab) {
        if (!players.contains(player)) {
            players.add(player);
        }

        final Collection<net.minecraft.server.v1_16_R1.Advancement> advs = new ArrayList<>();
        final Set<MinecraftKey> remove = new HashSet<>();
        final Map<MinecraftKey, AdvancementProgress> prgs = new HashMap<>();

        for (final Advancement advancement : advancements) {
            final boolean isTab = tab != null && advancement.getTab().isSimilar(tab);
            if (isTab) {
                remove.add(advancement.getName().getMinecraftKey());
            }

            if (tab == null || isTab) {
                //Criteria
                checkAwarded(player, advancement);
                final com.github.frcsty.advancementapi.wrapper.AdvancementDisplay display = advancement.getDisplay();
                final boolean showToast = display.isToastShown() && getCriteriaProgress(player, advancement) < advancement.getSavedCriteria().size();
                final net.minecraft.server.v1_16_R1.ItemStack icon = CraftItemStack.asNMSCopy(display.getIcon());
                MinecraftKey backgroundTexture = null;
                final boolean hasBackgroundTexture = display.getBackgroundTexture() != null;

                if (hasBackgroundTexture) {
                    backgroundTexture = new MinecraftKey(display.getBackgroundTexture());
                }

                final boolean hidden = !display.isVisible(player, advancement);
                advancement.saveHiddenStatus(player, hidden);

                if (!hidden || hiddenBoolean) {
                    final net.minecraft.server.v1_16_R1.AdvancementDisplay advDisplay = new AdvancementDisplay(icon, display.getTitle().getBaseComponent(), display.getDescription().getBaseComponent(), backgroundTexture, display.getFrame().getNMS(), showToast, display.isAnnouncedToChat(), hidden ? hiddenBoolean : false);
                    advDisplay.a(display.generateX() - getSmallestX(advancement.getTab()), display.generateY() - getSmallestY(advancement.getTab()));

                    final AdvancementRewards advRewards = new AdvancementRewards(0, new MinecraftKey[0], new MinecraftKey[0], null);
                    Map<String, Criterion> advCriteria = new HashMap<>();
                    String[][] advRequirements;

                    if (advancement.getSavedCriteria() == null) {
                        for (int i = 0; i < advancement.getCriteria(); i++) {
                            advCriteria.put("criterion." + i, new Criterion(new CriterionInstance() {
                                @Override
                                public JsonObject a(final LootSerializationContext context) {
                                    return null;
                                }

                                @Override
                                public MinecraftKey a() {
                                    return new MinecraftKey("minecraft", "impossible");
                                }
                            }));
                        }
                        advancement.saveCriteria(advCriteria);
                    } else {
                        advCriteria = advancement.getSavedCriteria();
                    }

                    if (advancement.getSavedCriteriaRequirements() == null) {
                        final List<String[]> fixedRequirements = new ArrayList<>();
                        for (final String name : advCriteria.keySet()) {
                            fixedRequirements.add(new String[]{name});
                        }
                        advRequirements = Arrays.stream(fixedRequirements.toArray()).toArray(String[][]::new);
                        advancement.saveCriteriaRequirements(advRequirements);
                    } else {
                        advRequirements = advancement.getSavedCriteriaRequirements();
                    }

                    final net.minecraft.server.v1_16_R1.Advancement adv = new net.minecraft.server.v1_16_R1.Advancement(advancement.getName().getMinecraftKey(), advancement.getParent() == null ? null : advancement.getParent().getSavedAdvancement(), advDisplay, advRewards, advCriteria, advRequirements);

                    advs.add(adv);

                    final net.minecraft.server.v1_16_R1.AdvancementProgress advPrg = advancement.getProgress(player);
                    advPrg.a(advancement.getSavedCriteria(), advancement.getSavedCriteriaRequirements());

                    for (final String criterion : advancement.getAwardedCriteria().get(player.getUniqueId().toString())) {
                        final CriterionProgress critPrg = advPrg.getCriterionProgress(criterion);
                        critPrg.b();
                    }

                    advancement.setProgress(player, advPrg);
                    prgs.put(advancement.getName().getMinecraftKey(), advPrg);
                }
            }

        }

        //Packet
        final PacketPlayOutAdvancements packet = new PacketPlayOutAdvancements(false, advs, remove, prgs);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }

    /**
     * Removes a player from the manager
     *
     * @param player Player to remove
     */
    public void removePlayer(final Player player) {
        players.remove(player);

        final Collection<net.minecraft.server.v1_16_R1.Advancement> advs = new ArrayList<>();
        final Set<MinecraftKey> remove = new HashSet<>();
        final Map<MinecraftKey, net.minecraft.server.v1_16_R1.AdvancementProgress> prgs = new HashMap<>();

        for (final Advancement advancement : advancements) {
            remove.add(advancement.getName().getMinecraftKey());
        }

        //Packet
        final PacketPlayOutAdvancements packet = new PacketPlayOutAdvancements(false, advs, remove, prgs);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }

    /**
     * Adds advancements or updates one advancement
     *
     * @param advancementsAdded An array of all advancements that should be added<br>If you want to update the display of an advancement, the array must have a length of 1
     */
    public void addAdvancement(final Advancement... advancementsAdded) {
        final Map<Player, Collection<net.minecraft.server.v1_16_R1.Advancement>> advancementsList = new HashMap<>();
        final Set<MinecraftKey> remove = new HashSet<>();
        final Map<Player, Map<MinecraftKey, net.minecraft.server.v1_16_R1.AdvancementProgress>> progressList = new HashMap<>();
        final Set<NameKey> updatedTabs = new HashSet<>();

        for (final Advancement adv : advancementsAdded) {
            float smallestY = getSmallestY(adv.getTab());
            final float y = adv.getDisplay().generateY();
            if (y < smallestY) {
                smallestY = y;
                updatedTabs.add(adv.getTab());
                AdvancementManager.smallestY.put(adv.getTab(), smallestY);
            }

            float smallestX = getSmallestX(adv.getTab());
            final float x = adv.getDisplay().generateY();
            if (x < smallestX) {
                smallestX = x;
                updatedTabs.add(adv.getTab());
                AdvancementManager.smallestX.put(adv.getTab(), smallestX);
            }
        }

        for (final NameKey key : updatedTabs) {
            for (final Player player : players) {
                update(player, key);
            }
        }

        for (final Advancement advancement : advancementsAdded) {
            if (advancements.contains(advancement)) {
                remove.add(advancement.getName().getMinecraftKey());
            } else {
                advancements.add(advancement);
            }
            final com.github.frcsty.advancementapi.wrapper.AdvancementDisplay display = advancement.getDisplay();
            final AdvancementRewards advRewards = new AdvancementRewards(0, new MinecraftKey[0], new MinecraftKey[0], null);
            final net.minecraft.server.v1_16_R1.ItemStack icon = CraftItemStack.asNMSCopy(display.getIcon());

            MinecraftKey backgroundTexture = null;
            final boolean hasBackgroundTexture = display.getBackgroundTexture() != null;

            if (hasBackgroundTexture) {
                backgroundTexture = new MinecraftKey(display.getBackgroundTexture());
            }

            Map<String, Criterion> advCriteria = new HashMap<>();
            String[][] advRequirements;

            if (advancement.getSavedCriteria() == null) {
                for (int i = 0; i < advancement.getCriteria(); i++) {
                    advCriteria.put("criterion." + i, new Criterion(new CriterionInstance() {
                        @Override
                        public JsonObject a(final LootSerializationContext context) {
                            return null;
                        }

                        @Override
                        public MinecraftKey a() {
                            return new MinecraftKey("minecraft", "impossible");
                        }
                    }));
                }
                advancement.saveCriteria(advCriteria);
            } else {
                advCriteria = advancement.getSavedCriteria();
            }

            if (advancement.getSavedCriteriaRequirements() == null) {
                final List<String[]> fixedRequirements = new ArrayList<>();
                for (final String name : advCriteria.keySet()) {
                    fixedRequirements.add(new String[]{name});
                }
                advRequirements = Arrays.stream(fixedRequirements.toArray()).toArray(String[][]::new);
                advancement.saveCriteriaRequirements(advRequirements);
            } else {
                advRequirements = advancement.getSavedCriteriaRequirements();
            }

            final net.minecraft.server.v1_16_R1.AdvancementDisplay saveDisplay = new AdvancementDisplay(icon, display.getTitle().getBaseComponent(), display.getDescription().getBaseComponent(), backgroundTexture, display.getFrame().getNMS(), display.isToastShown(), display.isAnnouncedToChat(), true);
            saveDisplay.a(display.generateX() - getSmallestY(advancement.getTab()), display.generateY() - getSmallestX(advancement.getTab()));
            final net.minecraft.server.v1_16_R1.Advancement saveAdv = new net.minecraft.server.v1_16_R1.Advancement(advancement.getName().getMinecraftKey(), advancement.getParent() == null ? null : advancement.getParent().getSavedAdvancement(), saveDisplay, advRewards, advCriteria, advRequirements);

            advancement.saveAdvancement(saveAdv);

            for (final Player player : getPlayers()) {
                final Map<MinecraftKey, net.minecraft.server.v1_16_R1.AdvancementProgress> prgs = progressList.containsKey(player) ? progressList.get(player) : new HashMap<>();
                checkAwarded(player, advancement);

                final boolean showToast = display.isToastShown() && getCriteriaProgress(player, advancement) < advancement.getSavedCriteria().size();
                final Collection<net.minecraft.server.v1_16_R1.Advancement> advs = advancementsList.containsKey(player) ? advancementsList.get(player) : new ArrayList<>();
                final boolean hidden = !display.isVisible(player, advancement);
                advancement.saveHiddenStatus(player, hidden);

                if (!hidden || hiddenBoolean) {
                    final net.minecraft.server.v1_16_R1.AdvancementDisplay advDisplay = new AdvancementDisplay(icon, display.getTitle().getBaseComponent(), display.getDescription().getBaseComponent(), backgroundTexture, display.getFrame().getNMS(), showToast, display.isAnnouncedToChat(), hidden ? hiddenBoolean : false);
                    advDisplay.a(display.generateX() - getSmallestX(advancement.getTab()), display.generateY() - getSmallestY(advancement.getTab()));
                    final net.minecraft.server.v1_16_R1.Advancement adv = new net.minecraft.server.v1_16_R1.Advancement(advancement.getName().getMinecraftKey(), advancement.getParent() == null ? null : advancement.getParent().getSavedAdvancement(), advDisplay, advRewards, advCriteria, advRequirements);

                    advs.add(adv);

                    advancementsList.put(player, advs);
                    final net.minecraft.server.v1_16_R1.AdvancementProgress advPrg = advancement.getProgress(player);
                    advPrg.a(advCriteria, advRequirements);

                    for (final String criterion : advancement.getAwardedCriteria().get(player.getUniqueId().toString())) {
                        final CriterionProgress critPrg = advPrg.getCriterionProgress(criterion);
                        critPrg.b();
                    }

                    advancement.setProgress(player, advPrg);

                    prgs.put(advancement.getName().getMinecraftKey(), advPrg);

                    progressList.put(player, prgs);
                }
            }
        }

        for (final Player player : getPlayers()) {
            //Packet
            final PacketPlayOutAdvancements packet = new PacketPlayOutAdvancements(false, advancementsList.get(player), remove, progressList.get(player));
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
        }
    }

    /**
     * Removes an advancement from the manager
     *
     * @param advancementsRemoved An array of advancements that should be removed
     */
    public void removeAdvancement(final Advancement... advancementsRemoved) {
        final Collection<net.minecraft.server.v1_16_R1.Advancement> advs = new ArrayList<>();
        final Set<MinecraftKey> remove = new HashSet<>();
        final Map<MinecraftKey, AdvancementProgress> prgs = new HashMap<>();

        for (final Advancement advancement : advancementsRemoved) {
            if (advancements.contains(advancement)) {
                advancements.remove(advancement);

                remove.add(advancement.getName().getMinecraftKey());
            }
        }

        for (final Player player : getPlayers()) {
            //Packet
            final PacketPlayOutAdvancements packet = new PacketPlayOutAdvancements(false, advs, remove, prgs);
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
        }
    }

    /**
     * Updates/Refreshes the player
     *
     * @param player Player to update
     */
    public void update(final Player player) {
        if (players.contains(player)) {
            final NameKey rootAdvancement = AdvancementAPI.getActiveTab(player);
            AdvancementAPI.clearActiveTab(player);
            addPlayer(player);
            new BukkitRunnable() {
                @Override
                public void run() {
                    AdvancementAPI.setActiveTab(player, rootAdvancement);
                }
            }.runTaskLater(AdvancementAPI.getInstance(), 5L);
        }
    }

    /**
     * Updates/Refreshes the player
     *
     * @param player Player to update
     * @param tab    Tab to update
     */
    private void update(final Player player, final NameKey tab) {
        if (players.contains(player)) {
            final NameKey rootAdvancement = AdvancementAPI.getActiveTab(player);
            AdvancementAPI.clearActiveTab(player);
            addPlayer(player, tab);
            new BukkitRunnable() {
                @Override
                public void run() {
                    AdvancementAPI.setActiveTab(player, rootAdvancement);
                }
            }.runTaskLater(AdvancementAPI.getInstance(), 5L);
        }
    }

    /**
     * Updates advancement progress for a player
     *
     * @param player              Player to update
     * @param advancementsUpdated An array of advancement to update progress
     */
    private void updateProgress(final Player player, final Advancement... advancementsUpdated) {
        updateProgress(player, false, true, advancementsUpdated);
    }

    private void updateProgress(final Player player,
                                final boolean alreadyGranted,
                                final boolean fireEvent,
                                final Advancement... advancementsUpdated) {
        if (players.contains(player)) {
            final Collection<net.minecraft.server.v1_16_R1.Advancement> advs = new ArrayList<>();
            final Set<MinecraftKey> remove = new HashSet<>();
            final Map<MinecraftKey, AdvancementProgress> prgs = new HashMap<>();

            for (final Advancement advancement : advancementsUpdated) {
                if (advancements.contains(advancement)) {
                    checkAwarded(player, advancement);

                    final net.minecraft.server.v1_16_R1.AdvancementProgress advPrg = advancement.getProgress(player);
                    final boolean hidden = advancement.getHiddenStatus(player);


                    advPrg.a(advancement.getSavedCriteria(), advancement.getSavedCriteriaRequirements());

                    final Map<String, Set<String>> awardedCriteria = advancement.getAwardedCriteria();
                    final Set<String> awarded = awardedCriteria.get(player.getUniqueId().toString());

                    for (final String criterion : advancement.getSavedCriteria().keySet()) {
                        if (awarded.contains(criterion)) {
                            final CriterionProgress critPrg = advPrg.getCriterionProgress(criterion);
                            critPrg.b();
                        } else {
                            final CriterionProgress critPrg = advPrg.getCriterionProgress(criterion);
                            critPrg.c();
                        }
                    }

                    advancement.setProgress(player, advPrg);
                    prgs.put(advancement.getName().getMinecraftKey(), advPrg);

                    if (hidden && advPrg.isDone()) {
                        final com.github.frcsty.advancementapi.wrapper.AdvancementDisplay display = advancement.getDisplay();
                        final AdvancementRewards advRewards = new AdvancementRewards(0, new MinecraftKey[0], new MinecraftKey[0], null);
                        final net.minecraft.server.v1_16_R1.ItemStack icon = CraftItemStack.asNMSCopy(display.getIcon());

                        MinecraftKey backgroundTexture = null;
                        final boolean hasBackgroundTexture = display.getBackgroundTexture() != null;

                        if (hasBackgroundTexture) {
                            backgroundTexture = new MinecraftKey(display.getBackgroundTexture());
                        }

                        Map<String, Criterion> advCriteria = new HashMap<>();
                        String[][] advRequirements;

                        if (advancement.getSavedCriteria() == null) {
                            for (int i = 0; i < advancement.getCriteria(); i++) {
                                advCriteria.put("criterion." + i, new Criterion(new CriterionInstance() {
                                    @Override
                                    public JsonObject a(final LootSerializationContext context) {
                                        return null;
                                    }

                                    @Override
                                    public MinecraftKey a() {
                                        return new MinecraftKey("minecraft", "impossible");
                                    }
                                }));
                            }
                            advancement.saveCriteria(advCriteria);
                        } else {
                            advCriteria = advancement.getSavedCriteria();
                        }

                        if (advancement.getSavedCriteriaRequirements() == null) {
                            final List<String[]> fixedRequirements = new ArrayList<>();
                            for (final String name : advCriteria.keySet()) {
                                fixedRequirements.add(new String[]{name});
                            }
                            advRequirements = Arrays.stream(fixedRequirements.toArray()).toArray(String[][]::new);
                            advancement.saveCriteriaRequirements(advRequirements);
                        } else {
                            advRequirements = advancement.getSavedCriteriaRequirements();
                        }

                        final net.minecraft.server.v1_16_R1.AdvancementDisplay advDisplay = new AdvancementDisplay(icon, display.getTitle().getBaseComponent(), display.getDescription().getBaseComponent(), backgroundTexture, display.getFrame().getNMS(), display.isToastShown(), display.isAnnouncedToChat(), hidden ? hiddenBoolean : false);
                        advDisplay.a(display.generateX() - getSmallestX(advancement.getTab()), display.generateY() - getSmallestY(advancement.getTab()));

                        final net.minecraft.server.v1_16_R1.Advancement adv = new net.minecraft.server.v1_16_R1.Advancement(advancement.getName().getMinecraftKey(), advancement.getParent() == null ? null : advancement.getParent().getSavedAdvancement(), advDisplay, advRewards, advCriteria, advRequirements);

                        advs.add(adv);
                    }


                    if (!alreadyGranted) {
                        if (advPrg.isDone()) {
                            grantAdvancement(player, advancement, true, false, fireEvent);
                        }
                    }
                }
            }

            final PacketPlayOutAdvancements packet = new PacketPlayOutAdvancements(false, advs, remove, prgs);
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
        }
    }

    /**
     * Updates all possibly affected visibilities for all parents and childs
     *
     * @param player Player to update
     * @param from   Advancement to check from
     */
    private void updateAllPossiblyAffectedVisibilities(final Player player, final Advancement from) {
        final List<Advancement> updated = from.getRow();
        for (final Advancement adv : updated) {
            updateVisibility(player, adv);
        }
    }

    /**
     * Updates the visibility
     *
     * @param player      Player to update
     * @param advancement Advancement to update
     */
    private void updateVisibility(final Player player, final Advancement advancement) {
        if (players.contains(player)) {
            final Collection<net.minecraft.server.v1_16_R1.Advancement> advs = new ArrayList<>();
            final Set<MinecraftKey> remove = new HashSet<>();
            final Map<MinecraftKey, AdvancementProgress> prgs = new HashMap<>();

            if (advancements.contains(advancement)) {
                checkAwarded(player, advancement);

                final com.github.frcsty.advancementapi.wrapper.AdvancementDisplay display = advancement.getDisplay();
                final boolean hidden = !display.isVisible(player, advancement);

                if (hidden == advancement.getHiddenStatus(player)) {
                    return;
                }

                advancement.saveHiddenStatus(player, hidden);

                if (!hidden || hiddenBoolean) {
                    remove.add(advancement.getName().getMinecraftKey());

                    final AdvancementRewards advRewards = new AdvancementRewards(0, new MinecraftKey[0], new MinecraftKey[0], null);
                    final ItemStack icon = CraftItemStack.asNMSCopy(display.getIcon());

                    MinecraftKey backgroundTexture = null;
                    final boolean hasBackgroundTexture = display.getBackgroundTexture() != null;

                    if (hasBackgroundTexture) {
                        backgroundTexture = new MinecraftKey(display.getBackgroundTexture());
                    }

                    Map<String, Criterion> advCriteria = new HashMap<>();
                    String[][] advRequirements;

                    if (advancement.getSavedCriteria() == null) {
                        for (int i = 0; i < advancement.getCriteria(); i++) {
                            advCriteria.put("criterion." + i, new Criterion(new CriterionInstance() {
                                @Override
                                public JsonObject a(final LootSerializationContext context) {
                                    return null;
                                }

                                @Override
                                public MinecraftKey a() {
                                    return new MinecraftKey("minecraft", "impossible");
                                }
                            }));
                        }
                        advancement.saveCriteria(advCriteria);
                    } else {
                        advCriteria = advancement.getSavedCriteria();
                    }

                    if (advancement.getSavedCriteriaRequirements() == null) {
                        final List<String[]> fixedRequirements = new ArrayList<>();
                        for (final String name : advCriteria.keySet()) {
                            fixedRequirements.add(new String[]{name});
                        }
                        advRequirements = Arrays.stream(fixedRequirements.toArray()).toArray(String[][]::new);
                        advancement.saveCriteriaRequirements(advRequirements);
                    } else {
                        advRequirements = advancement.getSavedCriteriaRequirements();
                    }

                    final boolean showToast = display.isToastShown();
                    final net.minecraft.server.v1_16_R1.AdvancementDisplay advDisplay = new net.minecraft.server.v1_16_R1.AdvancementDisplay(icon, display.getTitle().getBaseComponent(), display.getDescription().getBaseComponent(), backgroundTexture, display.getFrame().getNMS(), showToast, display.isAnnouncedToChat(), hidden ? hiddenBoolean : false);
                    advDisplay.a(display.generateX() - getSmallestX(advancement.getTab()), display.generateY() - getSmallestY(advancement.getTab()));

                    final net.minecraft.server.v1_16_R1.Advancement adv = new net.minecraft.server.v1_16_R1.Advancement(advancement.getName().getMinecraftKey(), advancement.getParent() == null ? null : advancement.getParent().getSavedAdvancement(), advDisplay, advRewards, advCriteria, advRequirements);

                    advs.add(adv);

                    final net.minecraft.server.v1_16_R1.AdvancementProgress advPrg = advancement.getProgress(player);
                    advPrg.a(advCriteria, advRequirements);

                    for (final String criterion : advancement.getAwardedCriteria().get(player.getUniqueId().toString())) {
                        final CriterionProgress critPrg = advPrg.getCriterionProgress(criterion);
                        critPrg.b();
                    }

                    advancement.setProgress(player, advPrg);

                    prgs.put(advancement.getName().getMinecraftKey(), advPrg);
                }
            }

            //Packet
            final PacketPlayOutAdvancements packet = new PacketPlayOutAdvancements(false, advs, remove, prgs);
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
        }
    }

    /**
     * @return A list of all advancements in the manager
     */
    public List<Advancement> getAdvancements() {
        return advancements;
    }

    /**
     * @param namespace Namespace to check
     * @return A list of all advancements in the manager with a specified namespace
     */
    private List<Advancement> getAdvancements(final String namespace) {
        final List<Advancement> advs = getAdvancements();
        advs.removeIf(advancement -> !advancement.getName().getNamespace().equalsIgnoreCase(namespace));
        return advs;
    }

    /**
     * @param name Name to check
     * @return An advancement matching the given name or null if it doesn't exist in the AdvancementManager
     */
    public Advancement getAdvancement(final NameKey name) {
        for (final Advancement advancement : advancements) {
            if (advancement.hasName(name)) {
                return advancement;
            }
        }
        return null;
    }

    /**
     * Displays a message to all players in the manager<br>
     * Note that this doesn't grant the advancement
     *
     * @param player      Player which has received an advancement
     * @param advancement Advancement Player has received
     */
    private void displayMessage(final Player player, final Advancement advancement) {
        final IChatBaseComponent message = advancement.getMessage(player);
        final PacketPlayOutChat packet = new PacketPlayOutChat(message, ChatMessageType.CHAT, AdvancementAPI.CHAT_MESSAGE_UUID);
        for (final Player receivers : getPlayers()) {
            ((CraftPlayer) receivers).getHandle().playerConnection.sendPacket(packet);
        }
    }

    /**
     * @return true if advancement messages will be shown by default in this manager<br>false if advancement messages will never be shown in this manager
     */
    private boolean isAnnounceAdvancementMessages() {
        return announceAdvancementMessages;
    }

    /**
     * Changes if advancement messages will be shown by default in this manager
     *
     * @param announceAdvancementMessages
     */
    public void setAnnounceAdvancementMessages(final boolean announceAdvancementMessages) {
        this.announceAdvancementMessages = announceAdvancementMessages;
    }

    /**
     * Makes the AdvancementManager accessible
     *
     * @param name Unique Name, case insensitive
     */
    public void makeAccessible(String name) {
        name = name.toLowerCase();
        if (name.equals("file")) {
            throw new RuntimeException("There is already an AdvancementManager with Name '" + name + "'!");
        }
        if (accessible.containsKey(name)) {
            throw new RuntimeException("There is already an AdvancementManager with Name '" + name + "'!");
        } else if (accessible.containsValue(this)) {
            throw new RuntimeException("AdvancementManager is already accessible with a different Name!");
        }
        accessible.put(name, this);
    }

    /**
     * Resets Accessibility-Status and Name
     */
    public void resetAccessible() {
        final Iterator<String> it = accessible.keySet().iterator();
        while (it.hasNext()) {
            String name = it.next();
            if (accessible.get(name).equals(this)) {
                it.remove();
                break;
            }
        }
    }

    /**
     * Returns the Unique Name if AdvancementManager is accessible
     *
     * @return Name or null if not accessible
     */
    public String getName() {
        for (final String name : accessible.keySet()) {
            if (accessible.get(name).equals(this)) return name;
        }
        return null;
    }

    private void checkAwarded(final Player player, final Advancement advancement) {
        Map<String, Criterion> advCriteria = new HashMap<>();
        String[][] advRequirements;

        if (advancement.getSavedCriteria() == null) {
            for (int i = 0; i < advancement.getCriteria(); i++) {
                advCriteria.put("criterion." + i, new Criterion(new CriterionInstance() {
                    @Override
                    public JsonObject a(final LootSerializationContext context) {
                        return null;
                    }

                    @Override
                    public MinecraftKey a() {
                        return new MinecraftKey("minecraft", "impossible");
                    }
                }));
            }
            advancement.saveCriteria(advCriteria);
        } else {
            advCriteria = advancement.getSavedCriteria();
        }

        if (advancement.getSavedCriteriaRequirements() == null) {
            final List<String[]> fixedRequirements = new ArrayList<>();
            for (final String name : advCriteria.keySet()) {
                fixedRequirements.add(new String[]{name});
            }
            advRequirements = Arrays.stream(fixedRequirements.toArray()).toArray(String[][]::new);
            advancement.saveCriteriaRequirements(advRequirements);
        } else {
            advRequirements = advancement.getSavedCriteriaRequirements();
        }

        final Map<String, Set<String>> awardedCriteria = advancement.getAwardedCriteria();
        if (!awardedCriteria.containsKey(player.getUniqueId().toString())) {
            awardedCriteria.put(player.getUniqueId().toString(), new HashSet<>());
        }
    }

    private void checkAwarded(final UUID uuid, final Advancement advancement) {
        Map<String, Criterion> advCriteria = new HashMap<>();
        String[][] advRequirements;

        if (advancement.getSavedCriteria() == null) {
            for (int i = 0; i < advancement.getCriteria(); i++) {
                advCriteria.put("criterion." + i, new Criterion(new CriterionInstance() {
                    @Override
                    public JsonObject a(final LootSerializationContext context) {
                        return null;
                    }

                    @Override
                    public MinecraftKey a() {
                        return new MinecraftKey("minecraft", "impossible");
                    }
                }));
            }
            advancement.saveCriteria(advCriteria);
        } else {
            advCriteria = advancement.getSavedCriteria();
        }

        if (advancement.getSavedCriteriaRequirements() == null) {
            final List<String[]> fixedRequirements = new ArrayList<>();
            for (final String name : advCriteria.keySet()) {
                fixedRequirements.add(new String[]{name});
            }
            advRequirements = Arrays.stream(fixedRequirements.toArray()).toArray(String[][]::new);
            advancement.saveCriteriaRequirements(advRequirements);
        } else {
            advRequirements = advancement.getSavedCriteriaRequirements();
        }

        final Map<String, Set<String>> awardedCriteria = advancement.getAwardedCriteria();
        if (!awardedCriteria.containsKey(uuid.toString())) {
            awardedCriteria.put(uuid.toString(), new HashSet<>());
        }
    }

    private boolean isOnline(final UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid).isOnline();
    }

    /**
     * Grants an advancement
     *
     * @param player      Reciever
     * @param advancement Advancement to grant
     */
    public void grantAdvancement(final Player player, final Advancement advancement) {
        grantAdvancement(player, advancement, false, true, true);
    }

    private void grantAdvancement(final Player player, final Advancement advancement, final boolean alreadyGranted,
                                  final boolean updateProgress, final boolean fireEvent) {
        checkAwarded(player, advancement);
        final Map<String, Set<String>> awardedCriteria = advancement.getAwardedCriteria();
        final Set<String> awarded = awardedCriteria.get(player.getUniqueId().toString());
        awarded.addAll(advancement.getSavedCriteria().keySet());
        awardedCriteria.put(player.getUniqueId().toString(), awarded);
        advancement.setAwardedCriteria(awardedCriteria);

        if (fireEvent) {
            final boolean announceChat = advancement.getDisplay().isAnnouncedToChat() && AdvancementAPI.getInitiatedPlayers().contains(player) && AdvancementAPI.isAnnounceAdvancementMessages() && isAnnounceAdvancementMessages();
            final AdvancementGrantEvent event = new AdvancementGrantEvent(this, advancement, player, announceChat);

            Bukkit.getPluginManager().callEvent(event);
            if (advancement.getReward() != null) advancement.getReward().onGrant(player);
            if (event.isDisplayMessage()) {
                displayMessage(player, advancement);
            }
        }

        if (updateProgress) {
            updateProgress(player, alreadyGranted, false, advancement);
            updateAllPossiblyAffectedVisibilities(player, advancement);
        }
    }

    /**
     * Grants an advancement, also works with offline players
     *
     * @param uuid        Receiver UUID
     * @param advancement Advancement to grant
     */
    public void grantAdvancement(final UUID uuid, final Advancement advancement) {
        if (isOnline(uuid)) {
            grantAdvancement(Bukkit.getPlayer(uuid), advancement);
        } else {
            checkAwarded(uuid, advancement);

            final Map<String, Set<String>> awardedCriteria = advancement.getAwardedCriteria();
            final Set<String> awarded = awardedCriteria.get(uuid.toString());
            awarded.addAll(advancement.getSavedCriteria().keySet());
            awardedCriteria.put(uuid.toString(), awarded);
            advancement.setAwardedCriteria(awardedCriteria);

            final OfflineAdvancementGrantEvent event = new OfflineAdvancementGrantEvent(this, advancement, uuid);
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    /**
     * Revokes an advancement
     *
     * @param player      Receiver
     * @param advancement Advancement to revoke
     */
    public void revokeAdvancement(final Player player, final Advancement advancement) {
        checkAwarded(player, advancement);

        advancement.setAwardedCriteria(new HashMap<>());

        updateProgress(player, advancement);
        updateAllPossiblyAffectedVisibilities(player, advancement);

        final AdvancementRevokeEvent event = new AdvancementRevokeEvent(this, advancement, player);
        Bukkit.getPluginManager().callEvent(event);
    }

    /**
     * Revokes an advancement, also works with offline players
     *
     * @param uuid        Receiver UUID
     * @param advancement Advancement to revoke
     */
    public void revokeAdvancement(final UUID uuid, final Advancement advancement) {
        if (isOnline(uuid)) {
            revokeAdvancement(Bukkit.getPlayer(uuid), advancement);
        } else {
            checkAwarded(uuid, advancement);

            advancement.setAwardedCriteria(new HashMap<>());
            final OfflineAdvancementRevokeEvent event = new OfflineAdvancementRevokeEvent(this, advancement, uuid);
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    /**
     * Grants criteria for an advancement
     *
     * @param player      Receiver
     * @param advancement
     * @param criteria    Array of criteria to grant
     */
    public void grantCriteria(final Player player, final Advancement advancement, final String... criteria) {
        checkAwarded(player, advancement);
        final Map<String, Set<String>> awardedCriteria = advancement.getAwardedCriteria();
        final Set<String> awarded = awardedCriteria.get(player.getUniqueId().toString());
        awarded.addAll(Arrays.asList(criteria));
        awardedCriteria.put(player.getUniqueId().toString(), awarded);
        advancement.setAwardedCriteria(awardedCriteria);

        updateProgress(player, false, true, advancement);
        updateAllPossiblyAffectedVisibilities(player, advancement);

        final CriteriaGrantEvent event = new CriteriaGrantEvent(this, advancement, criteria, player);
        Bukkit.getPluginManager().callEvent(event);
    }

    /**
     * Grans criteria for an advancement, also works with offline players
     *
     * @param uuid
     * @param advancement
     * @param criteria
     */
    private void grantCriteria(final UUID uuid, final Advancement advancement, final String... criteria) {
        if (isOnline(uuid)) {
            grantCriteria(Bukkit.getPlayer(uuid), advancement, criteria);
        } else {
            checkAwarded(uuid, advancement);

            final Map<String, Set<String>> awardedCriteria = advancement.getAwardedCriteria();
            final Set<String> awarded = awardedCriteria.get(uuid.toString());
            awarded.addAll(Arrays.asList(criteria));
            awardedCriteria.put(uuid.toString(), awarded);
            advancement.setAwardedCriteria(awardedCriteria);

            final OfflineCriteriaGrantEvent event = new OfflineCriteriaGrantEvent(this, advancement, criteria, uuid);
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    /**
     * Revokes criteria for an advancement
     *
     * @param player      Receiver
     * @param advancement
     * @param criteria    Array of criteria to revoke
     */
    public void revokeCriteria(final Player player, final Advancement advancement, final String... criteria) {
        checkAwarded(player, advancement);
        final Map<String, Set<String>> awardedCriteria = advancement.getAwardedCriteria();
        final net.minecraft.server.v1_16_R1.AdvancementProgress advPrg = advancement.getProgress(player);
        if (advPrg.isDone()) {
            final AdvancementRevokeEvent event = new AdvancementRevokeEvent(this, advancement, player);
            Bukkit.getPluginManager().callEvent(event);
        }

        final Set<String> awarded = awardedCriteria.get(player.getUniqueId().toString());
        awarded.removeAll(Arrays.asList(criteria));
        awardedCriteria.put(player.getUniqueId().toString(), awarded);
        advancement.setAwardedCriteria(awardedCriteria);

        updateProgress(player, advancement);
        updateAllPossiblyAffectedVisibilities(player, advancement);

        final CriteriaGrantEvent event = new CriteriaGrantEvent(this, advancement, criteria, player);
        Bukkit.getPluginManager().callEvent(event);
    }

    /**
     * Revokes criteria for an advancement, also works with offline players
     *
     * @param uuid        Receiver UUID
     * @param advancement
     * @param criteria    Array of criteria to revoke
     */
    public void revokeCriteria(final UUID uuid, final Advancement advancement, final String... criteria) {
        if (isOnline(uuid)) {
            revokeCriteria(Bukkit.getPlayer(uuid), advancement, criteria);
        } else {
            checkAwarded(uuid, advancement);
            final Map<String, Set<String>> awardedCriteria = advancement.getAwardedCriteria();
            final net.minecraft.server.v1_16_R1.AdvancementProgress advPrg = advancement.getProgress(uuid);
            if (advPrg.isDone()) {
                final OfflineAdvancementRevokeEvent event = new OfflineAdvancementRevokeEvent(this, advancement, uuid);
                Bukkit.getPluginManager().callEvent(event);
            }

            final Set<String> awarded = awardedCriteria.get(uuid.toString());
            awarded.removeAll(Arrays.asList(criteria));
            awardedCriteria.put(uuid.toString(), awarded);
            advancement.setAwardedCriteria(awardedCriteria);

            final OfflineCriteriaGrantEvent event = new OfflineCriteriaGrantEvent(this, advancement, criteria, uuid);
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    /**
     * Sets the criteria progress for an advancement<br>
     * Might not work as expected when using features for experts<br>
     * Is the only method triggering CriteriaProgressChangeEvent
     *
     * @param player      Receiver
     * @param advancement
     * @param progress
     */
    private void setCriteriaProgress(final Player player, final Advancement advancement, int progress) {
        checkAwarded(player, advancement);
        final Map<String, Set<String>> awardedCriteria = advancement.getAwardedCriteria();
        final Set<String> awarded = awardedCriteria.get(player.getUniqueId().toString());

        final CriteriaProgressChangeEvent event = new CriteriaProgressChangeEvent(this, advancement, player, awarded.size(), progress);
        Bukkit.getPluginManager().callEvent(event);
        progress = event.getProgress();

        int difference = Math.abs(awarded.size() - progress);

        if (awarded.size() > progress) {
            //Count down
            int i = 0;
            for (final String criterion : advancement.getSavedCriteria().keySet()) {
                if (i >= difference) break;
                if (awarded.contains(criterion)) {
                    awarded.remove(criterion);
                    i++;
                }
            }
        } else if (awarded.size() < progress) {
            //Count up
            int i = 0;
            for (final String criterion : advancement.getSavedCriteria().keySet()) {
                if (i >= difference) break;
                if (!awarded.contains(criterion)) {
                    awarded.add(criterion);
                    i++;
                }
            }
        }

        awardedCriteria.put(player.getUniqueId().toString(), awarded);
        advancement.setAwardedCriteria(awardedCriteria);

        updateProgress(player, false, true, advancement);
        updateAllPossiblyAffectedVisibilities(player, advancement);
    }

    /**
     * Sets the criteria progress for an advancement, also works for offline players<br>
     * Might not work as expected when using features for experts<br>
     * Is the only method triggering CriteriaProgressChangeEvent
     *
     * @param uuid        Receiver UUID
     * @param advancement
     * @param progress
     */
    public void setCriteriaProgress(final UUID uuid, final Advancement advancement, int progress) {
        if (isOnline(uuid)) {
            setCriteriaProgress(Bukkit.getPlayer(uuid), advancement, progress);
        } else {
            checkAwarded(uuid, advancement);
            final Map<String, Set<String>> awardedCriteria = advancement.getAwardedCriteria();
            final Set<String> awarded = awardedCriteria.get(uuid.toString());

            final OfflineCriteriaProgressChangeEvent event = new OfflineCriteriaProgressChangeEvent(this, advancement, uuid, awarded.size(), progress);
            Bukkit.getPluginManager().callEvent(event);
            progress = event.getProgress();

            int difference = Math.abs(awarded.size() - progress);

            if (awarded.size() > progress) {
                //Count down
                int i = 0;
                for (final String criterion : advancement.getSavedCriteria().keySet()) {
                    if (i >= difference) break;
                    if (awarded.contains(criterion)) {
                        awarded.remove(criterion);
                        i++;
                    }
                }
            } else if (awarded.size() < progress) {
                //Count up
                int i = 0;
                for (final String criterion : advancement.getSavedCriteria().keySet()) {
                    if (i >= difference) break;
                    if (!awarded.contains(criterion)) {
                        awarded.add(criterion);
                        i++;
                    }
                }
            }

            awardedCriteria.put(uuid.toString(), awarded);
            advancement.setAwardedCriteria(awardedCriteria);
        }
    }

    /**
     * @param player
     * @param advancement
     * @return The criteria progress
     */
    private int getCriteriaProgress(final Player player, final Advancement advancement) {
        checkAwarded(player, advancement);
        final Map<String, Set<String>> awardedCriteria = advancement.getAwardedCriteria();

        return awardedCriteria.get(player.getUniqueId().toString()).size();
    }

    /**
     * @param uuid
     * @param advancement
     * @return The criteria progress
     */
    public int getCriteriaProgress(final UUID uuid, final Advancement advancement) {
        checkAwarded(uuid, advancement);
        final Map<String, Set<String>> awardedCriteria = advancement.getAwardedCriteria();

        return awardedCriteria.get(uuid.toString()).size();
    }

    private String getSavePath(final Player player, final String namespace) {
        return getSaveDirectory(namespace) + (AdvancementAPI.isUseUUID() ? player.getUniqueId() : player.getName()) + ".json";
    }

    private String getSaveDirectory(final String namespace) {
        return AdvancementAPI.getInstance().getDataFolder().getAbsolutePath() + File.separator + "saved_data" + File.separator + namespace + File.separator;
    }

    //Online Save/Load

    private File getSaveFile(final Player player, final String namespace) {
        final File file = new File(getSaveDirectory(namespace));
        file.mkdirs();
        return new File(getSavePath(player, namespace));
    }

    private String getSavePath(final UUID uuid, final String namespace) {
        return getSaveDirectory(namespace) + uuid + ".json";
    }

    private File getSaveFile(final UUID uuid, final String namespace) {
        final File file = new File(getSaveDirectory(namespace));
        file.mkdirs();
        return new File(getSavePath(uuid, namespace));
    }

    //Load Progress

    /**
     * @param player Player to check
     * @return A JSON String representation of the progress for a player
     */
    public String getProgressJSON(final Player player) {
        final Map<String, List<String>> prg = new HashMap<>();

        for (final Advancement advancement : getAdvancements()) {
            final String nameKey = advancement.getName().toString();
            final List<String> progress = prg.containsKey(nameKey) ? prg.get(nameKey) : new ArrayList<>();
            final net.minecraft.server.v1_16_R1.AdvancementProgress advPrg = advancement.getProgress(player);
            for (final String criterion : advancement.getSavedCriteria().keySet()) {
                final CriterionProgress critPrg = advPrg.getCriterionProgress(criterion);
                if (critPrg != null && critPrg.a()) {
                    progress.add(criterion);
                }
            }
            prg.put(nameKey, progress);
        }

        check();
        return gson.toJson(prg);
    }

    /**
     * @param player    Player to check
     * @param namespace Namespace to check
     * @return A JSON String representation of the progress for a player in a specified namespace
     */
    private String getProgressJSON(final Player player, final String namespace) {
        final Map<String, List<String>> prg = new HashMap<>();

        for (final Advancement advancement : getAdvancements()) {
            final String anotherNamespace = advancement.getName().getNamespace();

            if (namespace.equalsIgnoreCase(anotherNamespace)) {
                final String nameKey = advancement.getName().toString();
                final List<String> progress = prg.containsKey(nameKey) ? prg.get(nameKey) : new ArrayList<>();
                final net.minecraft.server.v1_16_R1.AdvancementProgress advPrg = advancement.getProgress(player);
                for (final String criterion : advancement.getSavedCriteria().keySet()) {
                    final CriterionProgress critPrg = advPrg.getCriterionProgress(criterion);
                    if (critPrg != null && critPrg.a()) {
                        progress.add(criterion);
                    }
                }
                prg.put(nameKey, progress);
            }
        }

        check();
        return gson.toJson(prg);
    }

    /**
     * Saves the progress
     *
     * @param player    Player to check
     * @param namespace Namespace to check
     */
    public void saveProgress(final Player player, final String namespace) {
        final File saveFile = getSaveFile(player, namespace);
        final String json = getProgressJSON(player, namespace);

        try {
            if (!saveFile.exists()) {
                saveFile.createNewFile();
            }
            final FileWriter w = new FileWriter(saveFile);
            w.write(json);
            w.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the progress
     *
     * @param player    Player to check
     * @param namespace Namespace to check
     */
    public void loadProgress(final Player player, final String namespace) {
        final File saveFile = getSaveFile(player, namespace);

        if (saveFile.exists() && saveFile.isFile()) {
            final Map<String, List<String>> prg = getProgress(player, namespace);

            for (final Advancement advancement : advancements) {
                if (advancement.getName().getNamespace().equalsIgnoreCase(namespace)) {
                    checkAwarded(player, advancement);

                    final String nameKey = advancement.getName().toString();

                    if (prg.containsKey(nameKey)) {
                        final List<String> loaded = prg.get(nameKey);

                        grantCriteria(player, advancement, loaded.toArray(new String[loaded.size()]));

                    }
                }
            }
        }
    }

    /**
     * Loads the progress
     *
     * @param player             Player to check
     * @param advancementsLoaded Array of advancements to check, all advancements which arent in the same namespace as the first one will be ignored
     */
    public void loadProgress(final Player player, final Advancement... advancementsLoaded) {
        if (advancementsLoaded.length == 0) return;
        final List<Advancement> advancements = Arrays.asList(advancementsLoaded);
        final String namespace = advancements.get(0).getName().getNamespace();
        final File saveFile = getSaveFile(player, namespace);

        if (saveFile.exists() && saveFile.isFile()) {
            final Map<String, List<String>> prg = getProgress(player, namespace);

            for (final Advancement advancement : advancements) {
                if (advancement.getName().getNamespace().equalsIgnoreCase(namespace)) {
                    checkAwarded(player, advancement);

                    final String nameKey = advancement.getName().toString();

                    if (prg.containsKey(nameKey)) {
                        final List<String> loaded = prg.get(nameKey);

                        grantCriteria(player, advancement, loaded.toArray(new String[loaded.size()]));

                    }
                }
            }
        }
    }

    /**
     * Loads the progress with a custom JSON String
     *
     * @param player             Player to check
     * @param json               JSON String to load from
     * @param advancementsLoaded Array of advancements to check
     */
    public void loadCustomProgress(final Player player, final String json, final Advancement... advancementsLoaded) {
        if (advancementsLoaded.length == 0) return;
        final List<Advancement> advancements = Arrays.asList(advancementsLoaded);
        final Map<String, List<String>> prg = getCustomProgress(json);

        for (final Advancement advancement : advancements) {
            checkAwarded(player, advancement);

            final String nameKey = advancement.getName().toString();
            if (prg.containsKey(nameKey)) {
                final List<String> loaded = prg.get(nameKey);

                grantCriteria(player, advancement, loaded.toArray(new String[loaded.size()]));

            }
        }
    }

    /**
     * Loads the progress with a custom JSON String
     *
     * @param player Player to check
     * @param json   JSON String to load from
     */
    public void loadCustomProgress(final Player player, final String json) {
        final Map<String, List<String>> prg = getCustomProgress(json);

        for (final Advancement advancement : advancements) {
            checkAwarded(player, advancement);

            final String nameKey = advancement.getName().toString();

            if (prg.containsKey(nameKey)) {
                final List<String> loaded = prg.get(nameKey);

                grantCriteria(player, advancement, loaded.toArray(new String[loaded.size()]));

            }
        }
    }

    //Offline Save/Load

    /**
     * Loads the progress with a custom JSON String
     *
     * @param player    Player to check
     * @param json      JSON String to load from
     * @param namespace Namespace to check
     */
    public void loadCustomProgress(final Player player, final String json, final String namespace) {
        final Map<String, List<String>> prg = getCustomProgress(json);

        for (final Advancement advancement : advancements) {
            if (advancement.getName().getNamespace().equalsIgnoreCase(namespace)) {
                checkAwarded(player, advancement);

                final String nameKey = advancement.getName().toString();

                if (prg.containsKey(nameKey)) {
                    final List<String> loaded = prg.get(nameKey);

                    grantCriteria(player, advancement, loaded.toArray(new String[loaded.size()]));

                }
            }
        }
    }

    private Map<String, List<String>> getProgress(final Player player, final String namespace) {
        final File saveFile = getSaveFile(player, namespace);

        try {
            final FileReader os = new FileReader(saveFile);
            final JsonParser parser = new JsonParser();
            final JsonElement element = parser.parse(os);
            os.close();

            check();
            return gson.fromJson(element, progressListType);
        } catch (final Exception ex) {
            ex.printStackTrace();
            return new HashMap<>();
        }
    }

    private Map<String, List<String>> getCustomProgress(final String json) {
        check();
        return gson.fromJson(json, progressListType);
    }

    //Load Progress

    /**
     * @param uuid Player UUID to check
     * @return A JSON String representation of the progress for a player
     */
    public String getProgressJSON(final UUID uuid) {
        final Map<String, List<String>> prg = new HashMap<>();

        for (final Advancement advancement : getAdvancements()) {
            final String nameKey = advancement.getName().toString();

            final List<String> progress = prg.containsKey(nameKey) ? prg.get(nameKey) : new ArrayList<>();
            final net.minecraft.server.v1_16_R1.AdvancementProgress advPrg = advancement.getProgress(uuid);
            for (final String criterion : advancement.getSavedCriteria().keySet()) {
                final CriterionProgress critPrg = advPrg.getCriterionProgress(criterion);
                if (critPrg != null && critPrg.a()) {
                    progress.add(criterion);
                }
            }
            prg.put(nameKey, progress);
        }

        check();
        return gson.toJson(prg);
    }

    /**
     * @param uuid      Player UUID to check
     * @param namespace Namespace to check
     * @return A JSON String representation of the progress for a player in a specified namespace
     */
    private String getProgressJSON(final UUID uuid, final String namespace) {
        final Map<String, List<String>> prg = new HashMap<>();

        for (final Advancement advancement : getAdvancements()) {
            final String anotherNamespace = advancement.getName().getNamespace();

            if (namespace.equalsIgnoreCase(anotherNamespace)) {
                final String nameKey = advancement.getName().toString();
                final List<String> progress = prg.containsKey(nameKey) ? prg.get(nameKey) : new ArrayList<>();
                final net.minecraft.server.v1_16_R1.AdvancementProgress advPrg = advancement.getProgress(uuid);
                for (final String criterion : advancement.getSavedCriteria().keySet()) {
                    final CriterionProgress critPrg = advPrg.getCriterionProgress(criterion);
                    if (critPrg != null && critPrg.a()) {
                        progress.add(criterion);
                    }
                }
                prg.put(nameKey, progress);
            }
        }

        check();
        return gson.toJson(prg);
    }

    /**
     * Saves the progress
     *
     * @param uuid      Player UUID to check
     * @param namespace Namespace to check
     */
    public void saveProgress(final UUID uuid, final String namespace) {
        final File saveFile = getSaveFile(uuid, namespace);
        final String json = getProgressJSON(uuid, namespace);

        try {
            if (!saveFile.exists()) {
                saveFile.createNewFile();
            }
            final FileWriter w = new FileWriter(saveFile);
            w.write(json);
            w.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the progress<br>
     * <b>Recommended to only load progress for online players!</b>
     *
     * @param uuid      Player UUID to check
     * @param namespace Namespace to check
     */
    @Deprecated
    public void loadProgress(final UUID uuid, final String namespace) {
        final File saveFile = getSaveFile(uuid, namespace);

        if (saveFile.exists() && saveFile.isFile()) {
            final Map<String, List<String>> prg = getProgress(uuid, namespace);

            for (final Advancement advancement : advancements) {
                if (advancement.getName().getNamespace().equalsIgnoreCase(namespace)) {
                    checkAwarded(uuid, advancement);

                    final String nameKey = advancement.getName().toString();
                    if (prg.containsKey(nameKey)) {
                        final List<String> loaded = prg.get(nameKey);

                        grantCriteria(uuid, advancement, loaded.toArray(new String[loaded.size()]));

                    }
                }
            }
        }
    }

    /**
     * Loads the progress<br>
     * <b>Recommended to only load progress for online players!</b>
     *
     * @param uuid               Player UUID to check
     * @param advancementsLoaded Array of advancements to check, all advancements which arent in the same namespace as the first one will be ignored
     */
    @Deprecated
    public void loadProgress(final UUID uuid, final Advancement... advancementsLoaded) {
        if (advancementsLoaded.length == 0) return;
        final List<Advancement> advancements = Arrays.asList(advancementsLoaded);
        final String namespace = advancements.get(0).getName().getNamespace();
        final File saveFile = getSaveFile(uuid, namespace);

        if (saveFile.exists() && saveFile.isFile()) {
            final Map<String, List<String>> prg = getProgress(uuid, namespace);

            for (final Advancement advancement : advancements) {
                if (advancement.getName().getNamespace().equalsIgnoreCase(namespace)) {
                    checkAwarded(uuid, advancement);

                    final String nameKey = advancement.getName().toString();
                    if (prg.containsKey(nameKey)) {
                        final List<String> loaded = prg.get(nameKey);

                        grantCriteria(uuid, advancement, loaded.toArray(new String[loaded.size()]));

                    }
                }
            }
        }
    }

    //Unload Progress

    /**
     * Loads the progress with a custom JSON String<br>
     * <b>Recommended to only load progress for online players!</b>
     *
     * @param uuid               Player UUID to check
     * @param json               JSON String to load from
     * @param advancementsLoaded Array of advancements to check
     */
    @Deprecated
    public void loadCustomProgress(final UUID uuid, final String json, final Advancement... advancementsLoaded) {
        if (advancementsLoaded.length == 0) return;
        final List<Advancement> advancements = Arrays.asList(advancementsLoaded);
        final Map<String, List<String>> prg = getCustomProgress(json);

        for (final Advancement advancement : advancements) {
            checkAwarded(uuid, advancement);

            final String nameKey = advancement.getName().toString();

            if (prg.containsKey(nameKey)) {
                final List<String> loaded = prg.get(nameKey);
                grantCriteria(uuid, advancement, loaded.toArray(new String[loaded.size()]));

            }
        }
    }

    /**
     * Loads the progress with a custom JSON String<br>
     * <b>Recommended to only load progress for online players!</b>
     *
     * @param uuid Player UUID to check
     * @param json JSON String to load from
     */
    @Deprecated
    public void loadCustomProgress(final UUID uuid, final String json) {
        final Map<String, List<String>> prg = getCustomProgress(json);

        for (final Advancement advancement : advancements) {
            checkAwarded(uuid, advancement);

            final String nameKey = advancement.getName().toString();

            if (prg.containsKey(nameKey)) {
                final List<String> loaded = prg.get(nameKey);

                grantCriteria(uuid, advancement, loaded.toArray(new String[loaded.size()]));

            }
        }
    }

    /**
     * Loads the progress with a custom JSON String<br>
     * <b>Recommended to only load progress for online players!</b>
     *
     * @param uuid      Player UUID to check
     * @param json      JSON String to load from
     * @param namespace Namespace to check
     */
    @Deprecated
    public void loadCustomProgress(final UUID uuid, final String json, final String namespace) {
        final Map<String, List<String>> prg = getCustomProgress(json);

        for (final Advancement advancement : advancements) {
            if (advancement.getName().getNamespace().equalsIgnoreCase(namespace)) {
                checkAwarded(uuid, advancement);

                final String nameKey = advancement.getName().toString();

                if (prg.containsKey(nameKey)) {
                    final List<String> loaded = prg.get(nameKey);

                    grantCriteria(uuid, advancement, loaded.toArray(new String[loaded.size()]));

                }
            }
        }
    }

    /**
     * Unloads the progress for all advancements in the manager<br>
     * <b>Does not work for Online Players!</b>
     *
     * @param uuid Affected Player UUID
     */
    public void unloadProgress(final UUID uuid) {
        if (isOnline(uuid)) {
            throw new UnloadProgressFailedException(uuid);
        } else {
            for (final Advancement advancement : getAdvancements()) {
                advancement.unsetProgress(uuid);
            }
        }
    }

    /**
     * Unloads the progress for all advancements in the manager with a specified namespace<br>
     * <b>Does not work for Online Players!</b>
     *
     * @param uuid      Affected Player UUID
     * @param namespace Specific Namespace
     */
    public void unloadProgress(final UUID uuid, final String namespace) {
        if (isOnline(uuid)) {
            throw new UnloadProgressFailedException(uuid);
        } else {
            for (final Advancement advancement : getAdvancements(namespace)) {
                advancement.unsetProgress(uuid);
            }
        }
    }

    /**
     * Unloads the progress for the given advancements<br>
     * <b>Does not work for Online Players!</b>
     *
     * @param uuid         Affected Player UUID
     * @param advancements Specific Advancements
     */
    public void unloadProgress(final UUID uuid, final Advancement... advancements) {
        if (isOnline(uuid)) {
            throw new UnloadProgressFailedException(uuid);
        } else {
            for (final com.github.frcsty.advancementapi.wrapper.Advancement advancement : advancements) {
                advancement.unsetProgress(uuid);
            }
        }
    }

    private Map<String, List<String>> getProgress(final UUID uuid, final String namespace) {
        final File saveFile = getSaveFile(uuid, namespace);

        try {
            final FileReader os = new FileReader(saveFile);
            final JsonParser parser = new JsonParser();
            final JsonElement element = parser.parse(os);
            os.close();

            check();
            return gson.fromJson(element, progressListType);
        } catch (final Exception ex) {
            ex.printStackTrace();
            return new HashMap<>();
        }
    }

}
