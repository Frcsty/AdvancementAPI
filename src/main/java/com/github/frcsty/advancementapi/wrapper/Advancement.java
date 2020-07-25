package com.github.frcsty.advancementapi.wrapper;

import com.github.frcsty.advancementapi.AdvancementAPI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import net.minecraft.server.v1_16_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Warning;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class Advancement {

    private static Map<String, Advancement> advancementMap = new HashMap<>();

    private transient NameKey name;
    @SerializedName("name")
    private String nameKey;
    private AdvancementDisplay display;
    private transient Advancement parent;
    @SerializedName("parent")
    private String parentKey;
    private transient Set<Advancement> children = new HashSet<>();
    @SerializedName("criteriaAmount")
    private int criteria = 1;
    private transient AdvancementReward reward;
    private transient Map<String, Set<String>> awardedCriteria = new HashMap<>();
    private transient Map<String, AdvancementProgress> progress = new HashMap<>();
    private transient Map<String, Criterion> savedCriteria = null;
    @SerializedName("criteria")
    private Set<String> savedCriterionNames = null;
    @SerializedName("criteriaRequirements")
    private String[][] savedCriteriaRequirements = null;
    private transient net.minecraft.server.v1_16_R1.Advancement savedAdvancement = null;
    private transient Map<String, Boolean> savedHiddenStatus;

    /**
     * @param parent  Parent advancement, used for drawing lines between different advancements
     * @param name    Unique Name
     * @param display
     */
    public Advancement(@Nullable final Advancement parent, final NameKey name, final AdvancementDisplay display) {
        this.parent = parent;
        if (this.parent != null) this.parent.addChildren(this);
        this.parentKey = parent == null ? null : parent.getName().toString();
        this.name = name;
        this.nameKey = name.toString();
        this.display = display;
    }

    /**
     * Generates an Advancement
     *
     * @param json JSON representation of {@link Advancement} instance
     * @return Generated {@link Advancement}
     */
    public static Advancement fromJSON(final String json) {
        Gson gson = new GsonBuilder().setLenient().create();
        Advancement created = gson.fromJson(json, Advancement.class);
        created.loadAfterGSON();
        return created;
    }

    /**
     * Generates an Advancement
     *
     * @param json JSON representation of {@link Advancement} instance
     * @return Generated {@link Advancement}
     */
    public static Advancement fromJSON(final JsonElement json) {
        Gson gson = new GsonBuilder().setLenient().create();
        Advancement created = gson.fromJson(json, Advancement.class);
        created.loadAfterGSON();
        return created;
    }

    private void loadAfterGSON() {
        this.children = new HashSet<>();
        this.name = new NameKey(nameKey);
        advancementMap.put(nameKey, this);
        this.parent = advancementMap.get(parentKey);
        if (this.parent != null) this.parent.addChildren(this);

        this.display.setVisibility(AdvancementVisibility.parseVisibility(this.display.visibilityIdentifier));
    }

    private void addChildren(final Advancement adv) {
        children.add(adv);
    }

    /**
     * @return JSON representation of current {@link Advancement} instance
     */
    public String getAdvancementJSON() {
        final Gson gson = new Gson();
        return gson.toJson(this);
    }

    /**
     * @return The parent Advancement
     */
    @Nullable
    public Advancement getParent() {
        return parent;
    }

    /**
     * @return Required Criteria Amount
     */
    public int getCriteria() {
        return criteria;
    }

    /**
     * Sets the Required Criteria Amount
     *
     * @param criteria
     */
    public void setCriteria(final int criteria) {
        this.criteria = criteria;
        final Map<String, Criterion> advCriteria = new HashMap<>();
        final String[][] advRequirements;

        for (int i = 0; i < getCriteria(); i++) {
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
        saveCriteria(advCriteria);

        final List<String[]> fixedRequirements = new ArrayList<>();
        for (final String name : advCriteria.keySet()) {
            fixedRequirements.add(new String[]{name});
        }
        advRequirements = Arrays.stream(fixedRequirements.toArray()).toArray(String[][]::new);
        saveCriteriaRequirements(advRequirements);
    }

    /**
     * @return Unique Name
     */
    public NameKey getName() {
        return name;
    }

    public AdvancementDisplay getDisplay() {
        return display;
    }

    /**
     * @return Currently set Reward
     */
    public AdvancementReward getReward() {
        return reward;
    }


    //Advancement Row

    /**
     * Sets the Reward for completing the Advancement
     *
     * @param reward
     */
    public void setReward(@Nullable final AdvancementReward reward) {
        this.reward = reward;
    }

    /**
     * Displays an Advancement Message to every Player saying Player has completed said advancement<br>
     * Note that this doesn't grant the advancement
     *
     * @param player Player who has recieved the advancement
     */
    public void displayMessageToEverybody(final Player player) {
        final IChatBaseComponent message = getMessage(player);
        final PacketPlayOutChat packet = new PacketPlayOutChat(message, ChatMessageType.CHAT, AdvancementAPI.CHAT_MESSAGE_UUID);
        for (final Player online : Bukkit.getOnlinePlayers()) {
            ((CraftPlayer) online).getHandle().playerConnection.sendPacket(packet);
        }
    }

    /**
     * @param player Player who has recieved the advancement
     * @return
     */
    public IChatBaseComponent getMessage(final Player player) {
        final String translation = "chat.type.advancement." + display.getFrame().name().toLowerCase();
        final IChatBaseComponent title = IChatBaseComponent.ChatSerializer.a(display.getTitle().getJson());
        final IChatBaseComponent description = IChatBaseComponent.ChatSerializer.a(display.getDescription().getJson());

        final ChatModifier tm = title.getChatModifier();
        final AdvancementDisplay.AdvancementFrame frame = getDisplay().getFrame();
        final EnumChatFormat typeColor = frame == AdvancementDisplay.AdvancementFrame.CHALLENGE ? EnumChatFormat.DARK_PURPLE : EnumChatFormat.GREEN;
        final String color = tm.getColor() == null ? typeColor.name().toLowerCase() : tm.getColor().name;

        return IChatBaseComponent.ChatSerializer.a("{"
                + "\"translate\":\"" + translation + "\","
                + "\"with\":"
                + "["
                + "\"" + player.getDisplayName() + "\","
                + "{"
                + "\"text\":\"[" + title.getText() + "]\",\"color\":\"" + color + "\",\"bold\":" + tm.isBold() + ",\"italic\":" + tm.isItalic() + ", \"strikethrough\":" + tm.isStrikethrough() + ",\"underlined\":" + tm.isUnderlined() + ",\"obfuscated\":" + tm.isRandom() + ","
                + "\"hoverEvent\":"
                + "{"
                + "\"action\":\"show_text\","
                + "\"value\":[\"\", {\"text\":\"" + title.getText() + "\",\"color\":\"" + color + "\",\"bold\":" + tm.isBold() + ",\"italic\":" + tm.isItalic() + ", \"strikethrough\":" + tm.isStrikethrough() + ",\"underlined\":" + tm.isUnderlined() + ",\"obfuscated\":" + tm.isRandom() + "}, {\"text\":\"\\n\"}, {\"text\":\"" + description.getText() + "\"}]"
                + "}"
                + "}"
                + "]"
                + "}");

    }

    /**
     * Sends a Toast Message regardless if the Player has it in one of their Advancement Managers or not
     *
     * @param player Player who should see the Toast Message
     */
    public void displayToast(final Player player) {
        final MinecraftKey notName = new MinecraftKey("advancement-api", "notification");
        final AdvancementDisplay display = getDisplay();
        final AdvancementRewards advRewards = new AdvancementRewards(0, new MinecraftKey[0], new MinecraftKey[0], null);
        final ItemStack icon = CraftItemStack.asNMSCopy(display.getIcon());

        MinecraftKey backgroundTexture = null;
        boolean hasBackgroundTexture = display.getBackgroundTexture() != null;

        if (hasBackgroundTexture) {
            backgroundTexture = new MinecraftKey(display.getBackgroundTexture());
        }

        final Map<String, Criterion> advCriteria = new HashMap<>();
        final String[][] advRequirements;
        advCriteria.put("for_free", new Criterion(new CriterionInstance() {
            @Override
            public JsonObject a(final LootSerializationContext context) {
                return null;
            }

            @Override
            public MinecraftKey a() {
                return new MinecraftKey("minecraft", "impossible");
            }
        }));
        final List<String[]> fixedRequirements = new ArrayList<>();

        fixedRequirements.add(new String[]{"for_free"});

        advRequirements = Arrays.stream(fixedRequirements.toArray()).toArray(String[][]::new);

        final net.minecraft.server.v1_16_R1.AdvancementDisplay saveDisplay = new net.minecraft.server.v1_16_R1.AdvancementDisplay(icon, display.getTitle().getBaseComponent(), display.getDescription().getBaseComponent(), backgroundTexture, display.getFrame().getNMS(), true, display.isAnnouncedToChat(), true);
        final net.minecraft.server.v1_16_R1.Advancement saveAdv = new net.minecraft.server.v1_16_R1.Advancement(notName, getParent() == null ? null : getParent().getSavedAdvancement(), saveDisplay, advRewards, advCriteria, advRequirements);
        final Map<MinecraftKey, AdvancementProgress> progress = new HashMap<>();
        final AdvancementProgress advPrg = new AdvancementProgress();
        advPrg.a(advCriteria, advRequirements);
        advPrg.getCriterionProgress("for_free").b();
        progress.put(notName, advPrg);

        final Set<MinecraftKey> keys = new HashSet<>();
        PacketPlayOutAdvancements packet = new PacketPlayOutAdvancements(false, Collections.singletonList(saveAdv), keys, progress);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);

        keys.add(notName);
        progress.clear();
        packet = new PacketPlayOutAdvancements(false, new ArrayList<>(), keys, progress);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }

    /**
     * @return All direct children
     */
    public Set<Advancement> getChildren() {
        return children;
    }

    /**
     * @return Root {@link Advancement}
     */
    public Advancement getRootAdvancement() {
        if (parent == null) {
            return this;
        } else {
            return parent.getRootAdvancement();
        }
    }

    /**
     * @return Unique Name of Advancement Tab
     */
    public NameKey getTab() {
        return getRootAdvancement().getName();
    }

//	public boolean isAnythingGrantedUntil(AdvancementManager manager, Player player) {
//		for(Advancement until : getRowUntil()) {
//			if(manager.getCriteriaProgress(player, until) >= getCriteria()) return true;
//		}
//		return false;
//	}
//
//	public boolean isAnythingGrantedAfter(AdvancementManager manager, Player player) {
//		for(Advancement after : getRowAfter()) {
//			if(manager.getCriteriaProgress(player, after) >= getCriteria()) return true;
//		}
//		return false;
//	}


    //Saved

    /**
     * @return All parents and children
     */
    public List<Advancement> getRow() {
        final List<Advancement> row = new ArrayList<>();
        row.add(this);
        if (getParent() != null) {
            for (Advancement untilRow : getParent().getRowUntil()) {
                if (!row.contains(untilRow)) row.add(untilRow);
            }
            Collections.reverse(row);
        }
        for (Advancement child : getChildren()) {
            for (Advancement afterRow : child.getRowAfter()) {
                if (!row.contains(afterRow)) row.add(afterRow);
            }
        }
        return row;
    }

    /**
     * @return All parents
     */
    public List<Advancement> getRowUntil() {
        final List<Advancement> row = new ArrayList<>();
        row.add(this);
        if (getParent() != null) {
            for (Advancement untilRow : getParent().getRowUntil()) {
                if (!row.contains(untilRow)) row.add(untilRow);
            }
        }
        return row;
    }

    /**
     * @return All children
     */
    public List<Advancement> getRowAfter() {
        final List<Advancement> row = new ArrayList<>();
        row.add(this);
        for (Advancement child : getChildren()) {
            for (Advancement afterRow : child.getRowAfter()) {
                if (!row.contains(afterRow)) row.add(afterRow);
            }
        }
        return row;
    }

    /**
     * @param player Player to check
     * @return true if any parent is granted
     */
    public boolean isAnythingGrantedUntil(final Player player) {
        for (final Advancement until : getRowUntil()) {
            if (until.isGranted(player)) return true;
        }
        return false;
    }

    /**
     * @param player Player to check
     * @return true if any child is granted
     */
    public boolean isAnythingGrantedAfter(final Player player) {
        for (final Advancement after : getRowAfter()) {
            if (after.isGranted(player)) return true;
        }
        return false;
    }

    @Warning(reason = "Only use if you know what you are doing!")
    public void saveHiddenStatus(final Player player, final boolean hidden) {
        if (savedHiddenStatus == null) savedHiddenStatus = new HashMap<>();
        savedHiddenStatus.put(player.getUniqueId().toString(), hidden);
    }

    public boolean getHiddenStatus(final Player player) {
        if (savedHiddenStatus == null) savedHiddenStatus = new HashMap<>();
        if (!savedHiddenStatus.containsKey(player.getUniqueId().toString()))
            savedHiddenStatus.put(player.getUniqueId().toString(), getDisplay().isVisible(player, this));
        return savedHiddenStatus.get(player.getUniqueId().toString());
    }


    @Warning(reason = "Only use if you know what you are doing!")
    public void saveCriteria(final Map<String, Criterion> save) {
        savedCriteria = save;
        savedCriterionNames = save.keySet();
    }

    public Map<String, Criterion> getSavedCriteria() {
        return savedCriteria;
    }

    @Warning(reason = "Only use if you know what you are doing!")
    public void saveCriteriaRequirements(final String[][] save) {
        savedCriteriaRequirements = save;
    }

    public String[][] getSavedCriteriaRequirements() {
        return savedCriteriaRequirements;
    }

    @Warning(reason = "Unsafe")
    public void saveAdvancement(final net.minecraft.server.v1_16_R1.Advancement save) {
        savedAdvancement = save;
    }

    public net.minecraft.server.v1_16_R1.Advancement getSavedAdvancement() {
        return savedAdvancement;
    }


    //Player Actions

    public Map<String, Set<String>> getAwardedCriteria() {
        if (awardedCriteria == null) awardedCriteria = new HashMap<>();
        return awardedCriteria;
    }

    @Warning(reason = "Unsafe")
    public void setAwardedCriteria(final Map<String, Set<String>> awardedCriteria) {
        this.awardedCriteria = awardedCriteria;
    }

    public AdvancementProgress getProgress(final Player player) {
        if (this.progress == null) progress = new HashMap<>();
        return this.progress.containsKey(player.getUniqueId().toString()) ? this.progress.get(player.getUniqueId().toString()) : new AdvancementProgress();
    }

    public AdvancementProgress getProgress(final UUID uuid) {
        if (this.progress == null) progress = new HashMap<>();
        return this.progress.containsKey(uuid.toString()) ? this.progress.get(uuid.toString()) : new AdvancementProgress();
    }

    @Warning(reason = "Only use if you know what you are doing!")
    public void setProgress(final Player player, final AdvancementProgress progress) {
        if (this.progress == null) this.progress = new HashMap<>();
        this.progress.put(player.getUniqueId().toString(), progress);
    }

    @Warning(reason = "Only use if you know what you are doing!")
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

    /**
     * @param player Player to check
     * @return true if advancement is granted
     */
    public boolean isGranted(final Player player) {
        return getProgress(player).isDone();
    }


    /**
     * @param key Key to check
     * @return true if {@link Advancement} name and key share the same namespace and name
     */
    public boolean hasName(final NameKey key) {
        return key.getNamespace().equalsIgnoreCase(name.getNamespace()) && key.getKey().equalsIgnoreCase(name.getKey());
    }

    @Override
    public String toString() {
        return "Advancement " + getAdvancementJSON() + "";
    }

}