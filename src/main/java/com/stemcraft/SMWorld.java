package com.stemcraft;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;

public class SMWorld {
    private final String name;
    private World world;
    private static HashMap<String, SMWorld> worldMap;

    public SMWorld(String name) {
        this.name = name;
        this.world = null;

        for(World world : Bukkit.getWorlds()) {
            if(world.getName().equals(name)) {
                this.world = world;
                break;
            }
        }
    }

    public SMWorld(World world) {
        this.name = world.getName();
        this.world = world;
    }

    public World getBase() {
        return world;
    }

    public boolean isLoaded() {
        return (world != null);
    }

    public static boolean isLoaded(String name) {
        for(World world : Bukkit.getWorlds()) {
            if(world.getName().equals(name)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isLoadable(String name) {
        for(World world : Bukkit.getWorlds()) {
            if(world.getName().equals(name)) {
                return true;
            }
        }

        File worldFolder = getWorldFolder(name);
        if (!worldFolder.isDirectory()) {
            return false;
        }

        if (new File(worldFolder, "level.dat").exists()) {
            return true;
        }

        File regionFolder = getWorldRegionFolder(name);
        if (regionFolder != null) {
            for (String fileName : regionFolder.list()) {
                if (fileName.toLowerCase(Locale.ENGLISH).endsWith(".mca")) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean exists() {
        if (world == null) {
            return isLoadable(name);
        }

        return true;
    }

    public boolean getAutoLoad() {
        return SMConfig.getBoolean("config.worlds." + this.name + ".load", false);
    }

    public void setAutoLoad(boolean value) {
        SMConfig.set("config.worlds." + this.name + ".load", value);
        SMConfig.save("config");
    }

    public String getGameMode() {
        return SMConfig.getString("config.worlds." + this.name + ".gamemode", "");
    }

    public void setGameMode(String value) {
        String path = "config.worlds." + this.name + ".gamemode";

        if(value.isEmpty()) {
            SMConfig.remove(path);
        } else {
            SMConfig.set(path, value);
        }

        SMConfig.save("config");
    }

    public Location getSpawn() {
        return this.getBase().getSpawnLocation();
    }

    public void setSpawn(Location location) {
        this.getBase().setSpawnLocation(location);
    }

    /** Static Methods **/

    public static boolean exists(String name) {
        for(World world : Bukkit.getWorlds()) {
            if(world.getName().equals(name)) {
                return true;
            }
        }

        return isLoadable(name);
    }

    public static Collection<String> list() {
        String[] subDirs = Bukkit.getWorldContainer().list();
        Collection<String> list = new ArrayList<String>(subDirs.length);
        for (String name : subDirs) {
            if (isLoadable(name)) {
                list.add(name);
            }
        }
        return list;
    }

    public void load() {
        if(world == null && exists()) {
            create();
        }
    }

    public static void load(String name) {
        SMWorld world = new SMWorld(name);
        world.load();
    }

    public void create() {
        if(world == null) {
            ChunkGenerator cgen = null;
            WorldCreator c = new WorldCreator(name);
            c.generatorSettings("{}");
            c.generator(cgen);
            world = c.createWorld();
        }
    }

    public void unload() {
        if(world != null) {
            if(Bukkit.getWorlds().get(0) != world) {
                for (Player player : world.getPlayers()) {
                    SMMessenger.send(SMMessenger.MessageType.WARNING, player, "World '{name}' is being unloaded, teleporting to main world", "name", name);
                    SMUtils.delayedTeleport(player, Bukkit.getWorlds().get(0).getSpawnLocation());
                }

                STEMCraft.runLater(1, () -> {
                    Bukkit.unloadWorld(world, true);
                });

                world = null;
            }
        }
    }

    public void remove(CommandSender sender) {
        if(world != null) {
            if(Bukkit.getWorlds().get(0) != world) {
                for (Player player : world.getPlayers()) {
                    SMMessenger.send(SMMessenger.MessageType.WARNING, player, "World is being removed, teleporting to main world");
                    SMUtils.delayedTeleport(player, Bukkit.getWorlds().get(0).getSpawnLocation());
                }

                STEMCraft.runLater(1, () -> {
                    Bukkit.unloadWorld(world, false);
                });

                SMMessenger.send(SMMessenger.MessageType.INFO, sender, "World removed");
                world = null;
            } else {
                SMMessenger.send(SMMessenger.MessageType.ERROR, sender, "You cannot remove the main world");
            }
        }
    }

    private static File getWorldFolder(String worldName) {
        return new File(Bukkit.getWorldContainer(), worldName);
    }

    private static File getWorldRegionFolder(String worldName) {
        File mainFolder = getWorldFolder(worldName);

        // Overworld
        File tmp = new File(mainFolder, "region");
        if (tmp.exists()) {
            return tmp;
        }

        // Nether
        tmp = new File(mainFolder, "DIM-1" + File.separator + "region");
        if (tmp.exists()) {
            return tmp;
        }

        // The End
        tmp = new File(mainFolder, "DIM1" + File.separator + "region");
        if (tmp.exists()) {
            return tmp;
        }

        // Unknown???
        return null;
    }
}
