package com.github.frcsty.advancementapi;

import com.github.frcsty.advancementapi.manager.AdvancementManager;
import com.github.frcsty.advancementapi.wrapper.Advancement;
import com.github.frcsty.advancementapi.wrapper.AdvancementPacketReceiver;
import com.github.frcsty.advancementapi.wrapper.NameKey;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.server.v1_16_R1.PacketPlayOutAdvancements;
import net.minecraft.server.v1_16_R1.PacketPlayOutSelectAdvancementTab;
import org.bukkit.Bukkit;
import org.bukkit.Warning;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileReader;
import java.util.*;

public final class AdvancementAPI extends JavaPlugin implements Listener {

    public static final UUID CHAT_MESSAGE_UUID = new UUID(0, 0);

    private static AdvancementAPI instance;
    private static AdvancementPacketReceiver packetReciever;
    private static ArrayList<Player> initiatedPlayers = new ArrayList<>();
    private static ArrayList<AdvancementManager> managers = new ArrayList<>();
    private static boolean announceAdvancementMessages = true;
    private static Map<String, NameKey> openedTabs = new HashMap<>();
    private static boolean useUUID;
    private AdvancementManager fileAdvancementManager;

    public AdvancementAPI() {
        if (instance == null) {
            instance = this;
        }
    }

    /**
     * Creates a new instance of an advancement manager
     *
     * @param players All players that should be in the new manager from the start, can be changed at any time
     * @return the generated advancement manager
     */
    public static AdvancementManager getNewAdvancementManager(final Player... players) {
        return AdvancementManager.getNewAdvancementManager(players);
    }

    /**
     * Clears the active tab
     *
     * @param player The player whose Tab should be cleared
     */
    public static void clearActiveTab(final Player player) {
        setActiveTab(player, null, true);
    }

    /**
     * Sets the active tab
     *
     * @param player          The player whose Tab should be changed
     * @param rootAdvancement The name of the tab to change to
     */
    public static void setActiveTab(final Player player, final String rootAdvancement) {
        setActiveTab(player, new NameKey(rootAdvancement));
    }

    /**
     * Sets the active tab
     *
     * @param player          The player whose Tab should be changed
     * @param rootAdvancement The name of the tab to change to
     */
    public static void setActiveTab(final Player player, final NameKey rootAdvancement) {
        setActiveTab(player, rootAdvancement, true);
    }

    public static void setActiveTab(final Player player, final NameKey rootAdvancement, final boolean update) {
        if (update) {
            final PacketPlayOutSelectAdvancementTab packet = new PacketPlayOutSelectAdvancementTab(rootAdvancement == null ? null : rootAdvancement.getMinecraftKey());
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
        }
        openedTabs.put(player.getName(), rootAdvancement);
    }

    /**
     * @param player Player to check
     * @return The active Tab
     */
    public static NameKey getActiveTab(final Player player) {
        return openedTabs.get(player.getName());
    }

    @Warning(reason = "Unsafe")
    public static List<Player> getInitiatedPlayers() {
        return initiatedPlayers;
    }

    public static AdvancementAPI getInstance() {
        return instance;
    }

    /**
     * @return <b>true</b> if advancement messages should be shown by default<br><b>false</b> if all advancement messages will be hidden
     */
    public static boolean isAnnounceAdvancementMessages() {
        return announceAdvancementMessages;
    }

    /**
     * Changes if advancement messages should be shown by default
     *
     * @param announceAdvancementMessages
     */
    public static void setAnnounceAdvancementMessages(final boolean announceAdvancementMessages) {
        AdvancementAPI.announceAdvancementMessages = announceAdvancementMessages;
    }

    /**
     * @return <b>true</b> if Player Progress is saved by their UUID<br><b>false</b> if Player Progress is saved by their Name (not recommended)<br><b>Saving and Loading Progress via UUID will might not work as expected with this Setting!!<b>
     */
    public static boolean isUseUUID() {
        return useUUID;
    }

    @Override
    public void onLoad() {
        instance = this;
        fileAdvancementManager = AdvancementManager.getNewAdvancementManager();
    }

    @Override
    public void onEnable() {
        packetReciever = new AdvancementPacketReceiver();

        //Registering Players
        new BukkitRunnable() {
            @Override
            public void run() {
                final String path = instance.getDataFolder().getAbsolutePath() + File.separator + "advancements" + File.separator + "main" + File.separator;
                final File saveLocation = new File(path);
                loadAdvancements(saveLocation);

                for (final Player player : Bukkit.getOnlinePlayers()) {
                    fileAdvancementManager.addPlayer(player);
                    packetReciever.initPlayer(player);
                    initiatedPlayers.add(player);
                }
            }
        }.runTaskLater(this, 5L);
        //Registering Events
        Bukkit.getPluginManager().registerEvents(this, this);

        reloadConfig();
        final FileConfiguration config = getConfig();
        config.addDefault("useUUID", true);
        saveConfig();
        useUUID = config.getBoolean("useUUID");
    }

    private void loadAdvancements(final File location) {
        if (location.mkdirs()) return;
        final File[] files = location.listFiles();

        if (files == null) return;
        Arrays.sort(files);
        for (final File file : files) {
            if (file.isDirectory()) {
                loadAdvancements(file);
            } else if (file.getName().endsWith(".json")) {
                try {
                    final FileReader os = new FileReader(file);
                    final JsonParser parser = new JsonParser();
                    final JsonElement element = parser.parse(os);
                    os.close();

                    final Advancement add = Advancement.fromJSON(element);
                    fileAdvancementManager.addAdvancement(add);
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDisable() {
        for (final AdvancementManager manager : managers) {
            for (final Advancement advancement : manager.getAdvancements()) {
                manager.removeAdvancement(advancement);
            }
        }
        final PacketPlayOutAdvancements packet = new PacketPlayOutAdvancements(true, new ArrayList<>(), new HashSet<>(), new HashMap<>());
        for (final Player p : Bukkit.getOnlinePlayers()) {
            packetReciever.close(p, packetReciever.getHandlers().get(p.getName()));
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
        }
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                fileAdvancementManager.addPlayer(player);
                initiatedPlayers.add(player);
            }
        }.runTaskLater(this, 5L);
        packetReciever.initPlayer(player);
    }

    @EventHandler
    public void quit(final PlayerQuitEvent event) {
        packetReciever.close(event.getPlayer(), packetReciever.getHandlers().get(event.getPlayer().getName()));
        initiatedPlayers.remove(event.getPlayer());
    }

}
