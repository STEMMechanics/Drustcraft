package com.stemcraft;

import org.bukkit.World;
import org.bukkit.command.CommandSender;

public class SMWorldManager {
    public static void onEnable(STEMCraft plugin) {

    }

    public static void onDisable(STEMCraft plugin) {

    }

//    public static World create(String worldName, long seed, CommandSender sender) {
//
//    }


//    public static World createWorld(String worldname, long seed, CommandSender sender) {
//        SMWorldConfig worldConfig = SMConfigManager.get("worlds." + worldname);
//
//        final boolean load = wc.isInitialized();
//        String chunkGeneratorName = wc.getChunkGeneratorName();
//        StringBuilder msg = new StringBuilder();
//        if (load) {
//            msg.append("Loading");
//        } else {
//            msg.append("Generating");
//        }
//        msg.append(" world '").append(worldname).append("'");
//        if (seed == 0) {
//            if (chunkGeneratorName != null) {
//                msg.append(" using chunk generator: '").append(chunkGeneratorName).append("'");
//            }
//        } else {
//            msg.append(" using seed ").append(seed);
//            if (chunkGeneratorName != null) {
//                msg.append(" and chunk generator: '").append(chunkGeneratorName).append("'");
//            }
//        }
//        MyWorlds.plugin.log(Level.INFO, msg.toString());
//
//        World w = null;
//        int i = 0;
//        ChunkGenerator cgen = null;
//        try {
//            if (chunkGeneratorName != null) {
//                cgen = getGenerator(worldname, chunkGeneratorName);
//            }
//        } catch (Throwable t) {
//            MyWorlds.plugin.getLogger().log(Level.SEVERE, "Failed to initialize generator " + chunkGeneratorName, t);
//            if (sender != null) {
//                sender.sendMessage(ChatColor.RED + "Failed to initialize generator " + chunkGeneratorName + ": " +
//                        t.getMessage());
//            }
//            return null;
//        }
//        if (cgen == null) {
//            if (chunkGeneratorName != null && chunkGeneratorName.indexOf(':') != 0) {
//                msg.setLength(0);
//                msg.append("World '").append(worldname);
//                msg.append("' could not be created because the chunk generator '");
//                msg.append(chunkGeneratorName).append("' was not found!");
//                MyWorlds.plugin.log(Level.SEVERE, msg.toString());
//                if (sender != null) {
//                    sender.sendMessage(ChatColor.RED + msg.toString());
//                }
//                return null;
//            }
//        }
//        Throwable failReason = null;
//        try {
//            WorldCreator c = new WorldCreator(worldname);
//            wc.worldmode.apply(c);
//            if (seed != 0) {
//                c.seed(seed);
//            }
//
//            // Parse args from chunkgenerator name
//            String options = "";
//            if (chunkGeneratorName != null) {
//                int chunkGenArgsStart = chunkGeneratorName.indexOf(':');
//                if (chunkGenArgsStart != -1) {
//                    options = chunkGeneratorName.substring(chunkGenArgsStart + 1);
//                }
//            }
//
//            // Parse custom structures/nostructures option from options
//            if (!options.isEmpty()) {
//                GeneratorStructuresParser parser = new GeneratorStructuresParser();
//                options = parser.process(options);
//                if (parser.hasNoStructures) {
//                    c.generateStructures(false);
//                } else if (parser.hasStructures) {
//                    c.generateStructures(true);
//                }
//            }
//
//            // Vanilla flat world settings
//            if (!options.isEmpty() && cgen == null) {
//                c.generatorSettings(options);
//            } else {
//                c.generatorSettings("{}");
//            }
//
//            c.generator(cgen);
//            w = c.createWorld();
//        } catch (Throwable t) {
//            failReason = t;
//        }
//        if (w == null) {
//            if (failReason != null) {
//                MyWorlds.plugin.getLogger().log(Level.SEVERE, "World creation failed after " + i + " retries!", failReason);
//                if (sender != null) {
//                    sender.sendMessage(ChatColor.RED + "Failed to create world: " + failReason.getMessage());
//                }
//            } else {
//                MyWorlds.plugin.log(Level.SEVERE, "World creation failed after " + i + " retries!");
//            }
//        } else if (i == 1) {
//            MyWorlds.plugin.log(Level.INFO, "World creation succeeded after 1 retry!");
//        } else if (i > 0) {
//            MyWorlds.plugin.log(Level.INFO, "World creation succeeded after " + i + " retries!");
//        }
//
//        // Force a save of world configurations so this persists
//        // The just-loaded world will have the configuration applied to it thanks to the WorldLoadEvent
//        WorldConfigStore.saveAll();
//
//        return w;
//    }

}
