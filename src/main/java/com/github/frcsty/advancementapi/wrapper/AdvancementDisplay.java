package com.github.frcsty.advancementapi.wrapper;

import com.github.frcsty.advancementapi.AdvancementAPI;
import com.google.gson.annotations.SerializedName;
import net.minecraft.server.v1_16_R1.AdvancementFrameType;
import net.minecraft.server.v1_16_R1.EnumChatFormat;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class AdvancementDisplay {

    @SerializedName("visibility")
    String visibilityIdentifier = "VANILLA";
    @SerializedName("icon")
    private Material iconID;
    private transient ItemStack icon;
    private JSONMessage title, description;
    private AdvancementFrame frame;
    private boolean showToast;
    private boolean announceChat;
    private transient AdvancementVisibility vis;
    private String backgroundTexture;
    private float x = 0, y = 0, tabWidth = 0, tabHeight = 0;
    private transient Advancement positionOrigin;

    //ItemStack constructors

    /**
     * @param icon         Icon {@link Material}
     * @param title        Title {@link JSONMessage}
     * @param description  Description {@link JSONMessage}
     * @param frame        {@link AdvancementFrame}
     * @param showToast    Should toast messages be shown
     * @param announceChat Should advancements be announced in chat
     * @param visibility   When an advancement is visible
     */
    public AdvancementDisplay(final Material icon, final JSONMessage title, final JSONMessage description,
                              final AdvancementFrame frame, final boolean showToast, final boolean announceChat,
                              final AdvancementVisibility visibility) {
        this.icon = new ItemStack(icon);
        this.iconID = icon;
        this.title = title;
        this.description = description;
        this.frame = frame;
        this.showToast = showToast;
        this.announceChat = announceChat;
        setVisibility(visibility);

        //initialize();
    }

    /**
     * @param icon         Icon {@link Material}
     * @param title        Title {@link String}
     * @param description  Description {@link String}
     * @param frame        {@link AdvancementFrame}
     * @param showToast    Should toast messages be shown
     * @param announceChat Should advancements be announced in chat
     * @param visibility   When an advancement is visible
     */
    public AdvancementDisplay(final Material icon, String title, final String description, final AdvancementFrame frame, final boolean showToast,
                              final boolean announceChat, final AdvancementVisibility visibility) {
        this.icon = new ItemStack(icon);
        this.iconID = icon;
        if (title.contains("§")) title += "§a";
        this.title = new JSONMessage("{\"text\":\"" + title.replaceAll("\"", "\\\"") + "\"}");
        this.description = new JSONMessage("{\"text\":\"" + description.replaceAll("\"", "\\\"") + "\"}");
        this.frame = frame;
        this.showToast = showToast;
        this.announceChat = announceChat;
        setVisibility(visibility);

        //initialize();
    }

    /**
     * @param icon              Icon {@link Material}
     * @param title             Title {@link JSONMessage}
     * @param description       Description {@link JSONMessage}
     * @param frame             {@link AdvancementFrame}
     * @param backgroundTexture Background texture path
     * @param showToast         Should toast messages be shown
     * @param announceChat      Should advancements be announced in chat
     * @param visibility        When an advancement is visible
     */
    public AdvancementDisplay(final Material icon, final JSONMessage title, final JSONMessage description,
                              final AdvancementFrame frame, final String backgroundTexture,
                              final boolean showToast, final boolean announceChat, final AdvancementVisibility visibility) {
        this.icon = new ItemStack(icon);
        this.iconID = icon;
        this.title = title;
        this.description = description;
        this.frame = frame;
        this.backgroundTexture = backgroundTexture;
        this.showToast = showToast;
        this.announceChat = announceChat;
        setVisibility(visibility);

        //initialize();
    }

    /**
     * @param icon              Icon {@link Material}
     * @param title             Title {@link String}
     * @param description       Description {@link String}
     * @param frame             {@link AdvancementFrame}
     * @param backgroundTexture Background texture path
     * @param showToast         Should toast messages be shown
     * @param announceChat      Should advancements be announced in chat
     * @param visibility        When an advancement is visible
     */
    public AdvancementDisplay(final Material icon, String title, final String description, final AdvancementFrame frame,
                              final String backgroundTexture, final boolean showToast, final boolean announceChat,
                              final AdvancementVisibility visibility) {
        this.icon = new ItemStack(icon);
        this.iconID = icon;
        if (title.contains("§")) title += "§a";
        this.title = new JSONMessage("{\"text\":\"" + title.replaceAll("\"", "\\\"") + "\"}");
        this.description = new JSONMessage("{\"text\":\"" + description.replaceAll("\"", "\\\"") + "\"}");
        this.frame = frame;
        this.backgroundTexture = backgroundTexture;
        this.showToast = showToast;
        this.announceChat = announceChat;
        setVisibility(visibility);

        //initialize();
    }

    /**
     * @param icon         Icon {@link ItemStack}
     * @param title        Title {@link JSONMessage}
     * @param description  Description {@link JSONMessage}
     * @param frame        {@link AdvancementFrame}
     * @param showToast    Should toast messages be shown
     * @param announceChat Should advancements be announced in chat
     * @param visibility   When an advancement is visible
     */
    public AdvancementDisplay(final ItemStack icon, final JSONMessage title, final JSONMessage description,
                              final AdvancementFrame frame, final boolean showToast, final boolean announceChat,
                              final AdvancementVisibility visibility) {
        this.icon = icon;
        this.iconID = icon.getType();
        this.title = title;
        this.description = description;
        this.frame = frame;
        this.showToast = showToast;
        this.announceChat = announceChat;
        setVisibility(visibility);

        //initialize();
    }

    /**
     * @param icon         Icon {@link ItemStack}
     * @param title        Title {@link String}
     * @param description  Description {@link String}
     * @param frame        {@link AdvancementFrame}
     * @param showToast    Should toast messages be shown
     * @param announceChat Should advancements be announced in chat
     * @param visibility   When an advancement is visible
     */
    public AdvancementDisplay(final ItemStack icon, String title, final String description, final AdvancementFrame frame,
                              final boolean showToast, final boolean announceChat, final AdvancementVisibility visibility) {
        this.icon = icon;
        this.iconID = icon.getType();
        if (title.contains("§")) title += "§a";
        this.title = new JSONMessage("{\"text\":\"" + title.replaceAll("\"", "\\\"") + "\"}");
        this.description = new JSONMessage("{\"text\":\"" + description.replaceAll("\"", "\\\"") + "\"}");
        this.frame = frame;
        this.showToast = showToast;
        this.announceChat = announceChat;
        setVisibility(visibility);

        //initialize();
    }

    /**
     * @param icon              Icon {@link ItemStack}
     * @param title             Title {@link JSONMessage}
     * @param description       Description {@link JSONMessage}
     * @param frame             {@link AdvancementFrame}
     * @param backgroundTexture Background texture path
     * @param showToast         Should toast messages be shown
     * @param announceChat      Should advancements be announced in chat
     * @param visibility        When an advancement is visible
     */
    public AdvancementDisplay(final ItemStack icon, final JSONMessage title, final JSONMessage description, final AdvancementFrame frame,
                              final String backgroundTexture, final boolean showToast, final boolean announceChat,
                              final AdvancementVisibility visibility) {
        this.icon = icon;
        this.iconID = icon.getType();
        this.title = title;
        this.description = description;
        this.frame = frame;
        this.backgroundTexture = backgroundTexture;
        this.showToast = showToast;
        this.announceChat = announceChat;
        setVisibility(visibility);
    }

    /**
     * @param icon              Icon {@link ItemStack}
     * @param title             Title {@link String}
     * @param description       Description {@link String}
     * @param frame             {@link AdvancementFrame}
     * @param backgroundTexture Background texture path
     * @param showToast         Should toast messages be shown
     * @param announceChat      Should advancements be announced in chat
     * @param visibility        When an advancement is visible
     */
    public AdvancementDisplay(final ItemStack icon, String title, final String description, final AdvancementFrame frame,
                              final String backgroundTexture, final boolean showToast, final boolean announceChat,
                              final AdvancementVisibility visibility) {
        this.icon = icon;
        this.iconID = icon.getType();
        if (title.contains("§")) title += "§a";
        this.title = new JSONMessage("{\"text\":\"" + title.replaceAll("\"", "\\\"") + "\"}");
        this.description = new JSONMessage("{\"text\":\"" + description.replaceAll("\"", "\\\"") + "\"}");
        this.frame = frame;
        this.backgroundTexture = backgroundTexture;
        this.showToast = showToast;
        this.announceChat = announceChat;
        setVisibility(visibility);
    }

    /**
     * @return Icon {@link ItemStack}
     */
    public ItemStack getIcon() {
        if (icon == null && iconID != null) icon = new ItemStack(iconID);
        return icon;
    }

    /**
     * Changes the Icon
     *
     * @param icon New Icon Material to display
     */
    public void setIcon(final Material icon) {
        this.icon = new ItemStack(icon);
        this.iconID = icon;
    }

    /**
     * Changes the Icon
     *
     * @param icon New Icon to display
     */
    public void setIcon(final ItemStack icon) {
        this.icon = icon;
        this.iconID = icon.getType();
    }

    /**
     * @return Title {@link JSONMessage}
     */
    public JSONMessage getTitle() {
        return title;
    }

    /**
     * Changes the Title
     *
     * @param title New title {@link JSONMessage}
     */
    public void setTitle(final JSONMessage title) {
        this.title = title;
    }

    /**
     * Changes the Title
     *
     * @param title New Title {@link String}
     */
    public void setTitle(String title) {
        if (title.contains("§")) title += "§a";
        this.title = new JSONMessage("{\"text\":\"" + title.replaceAll("\"", "\\\"") + "\"}");
    }

    /**
     * @return Description {@link JSONMessage}
     */
    public JSONMessage getDescription() {
        return description;
    }

    /**
     * Changes the Description
     *
     * @param description New description {@link JSONMessage}
     */
    public void setDescription(final JSONMessage description) {
        this.description = description;
    }

    /**
     * Changes the Description
     *
     * @param description New Title {@link String}
     */
    public void setDescription(final String description) {
        this.description = new JSONMessage("{\"text\":\"" + description.replaceAll("\"", "\\\"") + "\"}");
    }

    /**
     * @return {@link AdvancementFrame}
     */
    public AdvancementFrame getFrame() {
        return frame;
    }

    /**
     * Changes the Frame
     *
     * @param frame New Frame
     */
    public void setFrame(final AdvancementFrame frame) {
        this.frame = frame;
    }

    /**
     * @return true if toasts will be shown
     */
    public boolean isToastShown() {
        return showToast;
    }

    /**
     * @return true if messages will be displayed in chat
     */
    public boolean isAnnouncedToChat() {
        return announceChat && AdvancementAPI.isAnnounceAdvancementMessages();
    }

    /**
     * @return Background texture path
     */
    @Nullable
    public String getBackgroundTexture() {
        return backgroundTexture;
    }

    /**
     * Sets the background texture
     *
     * @param backgroundTexture Background Texture path
     */
    public void setBackgroundTexture(@Nullable final String backgroundTexture) {
        this.backgroundTexture = backgroundTexture;
    }

    /**
     * Gets the relative X coordinate
     *
     * @return relative X coordinate
     */
    public float getX() {
        return x;
    }

    /**
     * Changes the relative x coordinate
     *
     * @param x relative x coordinate
     */
    public void setX(final float x) {
        this.x = x;
    }

    /**
     * Gets the relative y coordinate
     *
     * @return relative y coordinate
     */
    public float getY() {
        return y;
    }

    /**
     * Changes the relative y coordinate
     *
     * @param y relative y coordinate
     */
    public void setY(final float y) {
        this.y = y;
    }

    /**
     * Gets the absolute x coordinate
     *
     * @return absolute x coordinate
     */
    public float generateX() {
        if (getPositionOrigin() == null) {
            return x;
        } else {
            return getPositionOrigin().getDisplay().generateX() + x;
        }
    }

    /**
     * Gets the absolute y coordinate
     *
     * @return absolute y coordinate
     */
    public float generateY() {
        if (getPositionOrigin() == null) {
            return y;
        } else {
            return getPositionOrigin().getDisplay().generateY() + y;
        }
    }

    public float getTabWidth() {
        return tabWidth;
    }

    public void setTabWidth(final float tabWidth) {
        this.tabWidth = tabWidth;
    }

    public float getTabHeight() {
        return tabHeight;
    }

    public void setTabHeight(final float tabHeight) {
        this.tabHeight = tabHeight;
    }

    /**
     * Gets the {@link AdvancementVisibility}
     *
     * @return when an advancement is visible
     */
    public AdvancementVisibility getVisibility() {
        return vis != null ? vis : AdvancementVisibility.VANILLA;
    }

    /**
     * Changes the visibility
     *
     * @param visibility New Visibility
     */
    public void setVisibility(final AdvancementVisibility visibility) {
        this.vis = visibility;
        this.visibilityIdentifier = getVisibility().getName();
    }

    /**
     * @param player      Player to check
     * @param advancement Advancement to check (because {@link AdvancementDisplay} is not bound to one advancement)
     * @return true if it should be currently visible
     */
    public boolean isVisible(final Player player, final Advancement advancement) {
        final AdvancementVisibility visibility = getVisibility();
        return visibility.isVisible(player, advancement) || advancement.getProgressComponent().isGranted(player) || (visibility.isAlwaysVisibleWhenAdvancementAfterIsVisible() && advancement.isAnythingGrantedAfter(player));
    }

    /**
     * @return the advancement that marks the origin of the coordinates
     */
    public Advancement getPositionOrigin() {
        return positionOrigin;
    }

    /**
     * Changes the advancement that marks the origin of the coordinates
     *
     * @param positionOrigin New position origin
     */
    public void setPositionOrigin(final Advancement positionOrigin) {
        this.positionOrigin = positionOrigin;
    }

    /**
     * Changes if toasts should be shown
     *
     * @param showToast decides whether to show toast or not
     */
    public void setShowToast(final boolean showToast) {
        this.showToast = showToast;
    }

    /**
     * Changes if chat messages should be displayed
     *
     * @param announceChat decides whether to announce chat or not
     */
    public void setAnnounceChat(final boolean announceChat) {
        this.announceChat = announceChat;
    }

    /**
     * Changes the relative coordinates
     *
     * @param x relative x coordinate
     * @param y relative y coordinate
     */
    public void setCoordinates(final float x, final float y) {
        this.x = x;
        this.y = y;
    }

    private void initialize() {
        System.out.println("@ Enum Before Modification.");
        final AdvancementFrameType before = AdvancementFrameType.GOAL;
        System.out.println(before.c().character);

        try {
            System.out.println("@ Initializing Enum Change");
            final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            final Unsafe unsafe = (Unsafe) unsafeField.get(null);

            System.out.println("@ Getting Declared Format Field.");
            final Field chatFormatField = AdvancementFrameType.class.getDeclaredField("f");
            final long offset = unsafe.objectFieldOffset(chatFormatField);

            System.out.println("@ Setting New Enum Object.");
            unsafe.putObject(AdvancementFrameType.GOAL, offset, EnumChatFormat.AQUA);
        } catch (final Exception exception) {
            throw new RuntimeException(exception);
        }

        System.out.println("@ Enum After Modification.");
        final AdvancementFrameType after = AdvancementFrameType.GOAL;
        System.out.println(after.c().character);
    }

    public enum AdvancementFrame {

        TASK(AdvancementFrameType.TASK),
        GOAL(AdvancementFrameType.GOAL),
        CHALLENGE(AdvancementFrameType.CHALLENGE);

        private final AdvancementFrameType nms;

        AdvancementFrame(final AdvancementFrameType nms) {
            this.nms = nms;
        }

        public AdvancementFrameType getNMS() {
            return nms;
        }
    }

}
