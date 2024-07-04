/*
 * STEMCraft - a bukkit plugin
 * Copyright (C) 2011 STEMMechanics
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.stemcraft;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Logger;

import com.stemcraft.interfaces.SMCallback;
import com.stemcraft.utils.SMUtilsString;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;

@FunctionalInterface
interface InstanceHandler<T> {
    boolean handle(T instance) throws Exception;
}

public class STEMCraft extends JavaPlugin implements Listener {
    /**
     * -- GETTER --
     *  Get the plugin instance.
     */
    /*
     * Plugin instance.
     */
    @Getter
    private static STEMCraft plugin;

    /**
     * A list of required plugins.
     */
    String[] requiredPlugins = {"PlaceholderAPI", "WorldEdit"};

    /**
     * Can the plugin meet the requirements to be enabled.
     */
    private Boolean allowEnable = true;

    private static final HashMap<String, Long> runOnceMap = new HashMap<>();
    private static final HashMap<String, SMTask> runOnceMapDelay = new HashMap<>();

    private static final HashMap<Player, SMPlayer> playerList = new HashMap<>();
    private static final HashMap<String, SMTabCompletion<?>> tabCompletions = new HashMap<>();

    private static List<SMTimer> timers = new ArrayList<>();

    /**
     * The display version. Can be set in config. Defaults to plugin version.
     */
    private static String displayVersion = null;

    public static void scheduleSyncDelayedTask(Object o) {

    }

    /**
     * On Bukkit Plugin load
     */
    @Override
    public void onLoad() {
        // Set plugin instance
        plugin = this;

        // Set messenger prefixes
        SMMessenger.setPrefix(SMMessenger.MessageType.INFO, SMConfig.getString("config.message.prefix.info"));
        SMMessenger.setPrefix(SMMessenger.MessageType.SUCCESS, SMConfig.getString("config.message.prefix.success"));
        SMMessenger.setPrefix(SMMessenger.MessageType.WARNING, SMConfig.getString("config.message.prefix.warn"));
        SMMessenger.setPrefix(SMMessenger.MessageType.ERROR, SMConfig.getString("config.message.prefix.error"));
        SMMessenger.setPrefix(SMMessenger.MessageType.ANNOUNCEMENT, SMConfig.getString("config.message.prefix.announce"));

        // Check required plugins are installed
        for (String pluginName : this.requiredPlugins) {
            if (Bukkit.getPluginManager().getPlugin(pluginName) == null) {
                getLogger().severe(pluginName + " is not installed! STEMCraft requires " + pluginName);
                this.allowEnable = false;
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
        }

        getLogger().info("STEMCraft " + getVersion() + " loaded");
    }

    /**
     * On Bukkit Plugin Enable
     */
    @Override
    public void onEnable() {
        // Can we be enabled?
        if (!this.allowEnable) {
            getLogger().severe("STEMCraft was not enabled because a dependency was missing");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Check required plugins are enabled
        for (String pluginName : this.requiredPlugins) {
            if (!Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
                getLogger().severe(pluginName + " is not enabled! This plugin requires " + pluginName);
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
        }

        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        if (!SMDatabase.isConnected()) {
            SMDatabase.connect();
        }

        loadPackageClasses("migrations", SMDatabaseMigration.class, instance -> {
            String name = instance.getClass().getSimpleName();
            SMDatabase.runMigration(name, instance::migrate);

            return true;
        });

        SMConfig.getKeys("config.worlds").forEach(worldName -> {
            if(SMConfig.getBoolean("config.worlds." + worldName + ".load", false)) {
                if(SMWorld.exists(worldName)) {
                    getLogger().info("Loading world '" + worldName + "'");
                    SMWorld.load(worldName);
                    if(SMWorld.isLoaded(worldName)) {
                        getLogger().info("World '" + worldName + "' loaded");
                    } else {
                        getLogger().warning("Could not load world '" + worldName + "'");
                    }
                } else {
                    getLogger().warning("Could not load world '" + worldName + "' as it does not exist");
                }
            }
        });

        loadPackageClasses("tabcompletions", SMTabCompletion.class, instance -> {
            tabCompletions.put(instance.getName(), instance);
            return true;
        });

        loadPackageClasses("listeners", Listener.class, instance -> {
            Bukkit.getPluginManager().registerEvents((Listener) instance, this);
            return true;
        });

        loadPackageClasses("commands", SMCommand.class, instance -> {
            instance.register();
            return true;
        });

        loadPackageClasses("timers", SMTimer.class, instance -> {
            timers.add(instance);
            return true;
        });

        SMSkipNight.initialize();
        SMRegion.loadRegions();
        SMWebServer.start();

        STEMCraft.runTimer(20, 1, () -> {
            for (SMTimer timer : timers) {
                timer.processTimer();
            }
        });
    }

    /**
     * On Bukkit Plugin Disable
     */
    @Override
    public void onDisable() {
        for(Player player : plugin.getServer().getOnlinePlayers()) {
            SMPlayer.saveState(player);
        }

        // Disconnect from Database
        if (SMDatabase.isConnected()) {
            SMDatabase.disconnect();
        }
    }

    /**
     * When receiving the Plugin Disable Event
     */
    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() == this) {
            onDisable();
        }
    }

    public static SMPlayer getPlayer(Player player) {
        if (!playerList.containsKey(player)) {
            playerList.put(player, new SMPlayer(player));
        }

        return playerList.get(player);
    }

    /**
     * JarFileProcessor callback interface
     */
    @FunctionalInterface
    public interface JarFileProcessor {
        void process(JarEntry jarFile);
    }

    /**
     * Iterate plugin files using the callback
     */
    public static void iteratePluginFiles(String path, JarFileProcessor callback) {
        try {
            File pluginFile = new File(STEMCraft.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (pluginFile.getPath().endsWith(".jar")) {
                try (JarInputStream jarInputStream = new JarInputStream(new FileInputStream(pluginFile))) {
                    JarEntry jarEntry;
                    while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                        if (path != null && !path.isEmpty()) {
                            String className = jarEntry.getName();
                            if (!className.startsWith(path)) {
                                continue;
                            }
                        }

                        callback.process(jarEntry);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the plugin version.
     */
    public static String getVersion() {
        return getPlugin().getDescription().getVersion();
    }

    /**
     * Get the plugin display version. Can be set in config.
     * 
     * @return The display version of the plugin.
     */
    public static String getDisplayVersion() {
        if (displayVersion == null) {
            displayVersion = SMConfig.getString("config.display-version");
            if (displayVersion == null) {
                displayVersion = getVersion();
            }
        }

        return displayVersion;
    }

    /**
     * Load plugin package classes from path.
     *
     * @param path The path to load classes from.
     * @param baseClass The base class to filter load.
     * @param handler The instance handler. Return boolean to filter instances.
     * @return A map of class names to instances.
     */
    public static <T> Map<String, T> loadPackageClasses(String path, Class<T> baseClass, InstanceHandler<T> handler) {
        Map<String, T> objectList = new HashMap<>();

        iteratePluginFiles("com/stemcraft/" + path, jar -> {
            String className = jar.getName();

            if (className.endsWith(".class")) {
                try {
                    String formattedClassName = className
                            .substring(0, className.length() - 6)
                            .replaceAll("/", ".");

                    Class<?> classItem = Class.forName(formattedClassName);

                    if (baseClass.isAssignableFrom(classItem)) {
                        Constructor<?> constructor = classItem.getDeclaredConstructor();
                        T classInstance = baseClass.cast(constructor.newInstance());

                        if (handler == null || handler.handle(classInstance)) {
                            objectList.put(classItem.getSimpleName(), classInstance);
                        }
                    }
                } catch (Exception e) {
                    STEMCraft.error(e);
                }
            }
        });

        return objectList;
    }

    /**
     * Send an info message to the console.
     */
    public static void info(String message) {
        Bukkit.getConsoleSender().sendMessage("[" + getNamed() + "] " + SMUtilsString.colorize(message));
    }

    /**
     * Send a warn message to the console.
     */
    public static void warning(String message) {
        Bukkit.getConsoleSender().sendMessage(SMUtilsString.colorize(message));
    }

    /**
     * Send a severe message to the console.
     */
    public static void severe(String message) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.COLOR_CHAR + "c" + SMUtilsString.colorize(message));
    }

    public static void error(@NonNull Throwable throwable, final String... messages) {
        if (throwable instanceof InvocationTargetException && throwable.getCause() != null)
            throwable = throwable.getCause();

        final List<String> lines = new ArrayList<>();

        lines.add(getNamed() + " " + getVersion() + " encountered a series error");
        if(messages != null) {
            lines.addAll(Arrays.asList(messages));
        }

        do {
            lines.add(throwable == null ? "Unknown error" : throwable.getClass().getSimpleName() + " " + SMUtilsString.getOrDefault(throwable.getMessage(), throwable.getLocalizedMessage(), "(Unknown cause)"));

            int count = 0;
            for(StackTraceElement element : throwable.getStackTrace()) {
                count++;
                final String trace = element.toString();
                if(trace.contains("sun.reflect"))
                    continue;
                if(count > 6 && trace.startsWith("net.minecraft.server"))
                    break;

                lines.add("    at " + trace);
            }
        } while((throwable = throwable.getCause()) != null);

        severe(String.join("\n", lines));
    }


    /**
     * Run a task after 1 tick has passed.
     *
     * @param runnable The task to run.
     * @return The task.
     */
    public static SMTask runLater(final Runnable runnable) {
        return runLater(1, runnable);
    }

    /**
     * Run a task after a delay.
     *
     * @param delayTicks The delay in ticks.
     * @param runnable The task to run.
     * @return The task.
     */
    public static SMTask runLater(final int delayTicks, final Runnable runnable) {
        if (!getPlugin().isEnabled()) {
            runnable.run();
            return null;
        } else {
            try {
                return SMTask.fromBukkit(Bukkit.getScheduler().runTaskLater(getPlugin(), runnable, delayTicks));
            } catch (final NoSuchMethodError err) {
                return SMTask.fromBukkit(
                        Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), runnable, delayTicks), false);
            }
        }
    }

    public static SMTask runAsync(final Runnable runnable) {
        return runLaterAsync(0, runnable);
    }

    @SuppressWarnings("deprecation")
    public static SMTask runLaterAsync(final int delayTicks, final Runnable runnable) {
        if (!getPlugin().isEnabled()) {
            runnable.run();
            return null;
        }

        try {

            return SMTask.fromBukkit(
                Bukkit.getScheduler().runTaskLaterAsynchronously(getPlugin(), runnable, delayTicks));

        } catch (final NoSuchMethodError err) {
            return SMTask.fromBukkit(
                Bukkit.getScheduler().scheduleAsyncDelayedTask(getPlugin(), runnable, delayTicks), true);
        }
    }

    public static SMTask runTimer(final int repeatTicks, final Runnable runnable) {
        return runTimer(0, repeatTicks, runnable);
    }

    /**
     * Runs the given task after the given delay with a fixed delay between repetitions, even if the plugin is disabled
     * for some reason.
     *
     * @param delayTicks the delay (in ticks) to wait before running the task.
     * @param repeatTicks the delay (in ticks) between repetitions of the task.
     * @param runnable the task to be run.
     * @return the {@link SMTask} representing the scheduled task, or {@code null}.
     */
    public static SMTask runTimer(final int delayTicks, final int repeatTicks, final Runnable runnable) {
        if (runIfDisabled(runnable))
            return null;

        try {
            return SMTask.fromBukkit(
                Bukkit.getScheduler().runTaskTimer(getPlugin(), runnable, delayTicks, repeatTicks));

        } catch (final NoSuchMethodError err) {
            return SMTask.fromBukkit(Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), runnable,
                delayTicks, repeatTicks), false);
        }
    }

    /**
     * Runs the given task asynchronously on the next tick with a fixed delay between repetitions, even if the plugin is
     * disabled for some reason.
     *
     * @param repeatTicks the delay (in ticks) between repetitions of the task.
     * @param task the task to be run.
     * @return the {@link SMTask} representing the scheduled task, or {@code null}.
     */
    public static SMTask runTimerAsync(final int repeatTicks, final Runnable task) {
        return runTimerAsync(0, repeatTicks, task);
    }

    /**
     * Runs the given task after the given delay with a fixed delay between repetitions, even if the plugin is disabled
     * for some reason.
     *
     * @param delayTicks the delay (in ticks) to wait before running the task.
     * @param repeatTicks the delay (in ticks) between repetitions of the task.
     * @param runnable the task to be run.
     * @return the {@link SMTask} representing the scheduled task, or {@code null}.
     */
    @SuppressWarnings("deprecation")
    public static SMTask runTimerAsync(final int delayTicks, final int repeatTicks, final Runnable runnable) {
        if (runIfDisabled(runnable))
            return null;

        try {
            return SMTask.fromBukkit(Bukkit.getScheduler()
                .runTaskTimerAsynchronously(getPlugin(), runnable, delayTicks, repeatTicks));
        } catch (final NoSuchMethodError err) {
            return SMTask.fromBukkit(Bukkit.getScheduler().scheduleAsyncRepeatingTask(getPlugin(), runnable,
                delayTicks, repeatTicks), true);
        }
    }

    /**
     * Runs the specified task if the plugin is disabled.
     * <p>
     * In case the plugin is disabled, this method will return {@code true} and the task will be run. Otherwise, we
     * return {@code false} and the task is run correctly in Bukkit's scheduler.
     * <p>
     * This is a fail-safe for critical save-on-exit operations in case the plugin malfunctions or is improperly
     * reloaded using a plugin manager such as PlugMan.
     *
     * @param runnable the task to be run.
     * @return {@code true} if the task was run, or {@code false} if the plugin is enabled.
     */
    private static boolean runIfDisabled(final Runnable runnable) {
        if (!getPlugin().isEnabled()) {
            runnable.run();

            return true;
        }

        return false;
    }

    /**
     * Run a callback, cancelling any other requests with the same id within the blockingTime.
     */
    public static void runOnce(final String id, final long blockingTime, final SMCallback callback) {
        long currentMs = System.currentTimeMillis();

        if (!runOnceMap.containsKey(id) || runOnceMap.get(id) < currentMs) {
            runOnceMap.put(id, currentMs + blockingTime);
            callback.run();
        }
    }

    /**
     * Run a callback once after a delay. Calls with the same id will cancel within the delay will cancel the original
     * callback.
     */
    public static SMTask runOnceDelay(final String id, final long delayTicks, final SMCallback callback) {
        if (runOnceMapDelay.containsKey(id)) {
            runOnceMapDelay.get(id).cancel();
        }

        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                runOnceMapDelay.remove(id);
                callback.run();
            }
        };

        SMTask task = SMTask.fromBukkit(runnable.runTaskLater(getPlugin(), delayTicks));
        runOnceMapDelay.put(id, task);

        return task;
    }

    /**
     * Run a callback once after a delay. Calls with the same id will cancel within the delay will cancel the original
     * callback.
     */
    public static void cancelRunOnceDelay(final String id) {
        if (runOnceMapDelay.containsKey(id)) {
            runOnceMapDelay.get(id).cancel();
        }
    }

    /**
     * Get the plugin name
     */
    public static String getNamed() {
        return getPlugin().getDescription().getName();
    }

    public static String locale(String id) {
        return id;
    }

    /**
     * Return the server's command map
     * @return The server's command map
     */
    public static CommandMap getCommandMap() {
        try {
            Server server = STEMCraft.getPlugin().getServer();
            final Field bukkitCommandMap = server.getClass().getDeclaredField("commandMap");

            bukkitCommandMap.setAccessible(true);
            return (CommandMap) bukkitCommandMap.get(server);
        } catch (Exception e) {
            STEMCraft.error(e);
        }

        return null;
    }

    public static SMTabCompletion<?> getTabCompletion(String name) {
        return tabCompletions.get(name);
    }
}
