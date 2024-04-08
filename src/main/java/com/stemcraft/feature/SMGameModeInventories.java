package com.stemcraft.feature;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.stemcraft.STEMCraft;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import com.stemcraft.core.SMDatabase;
import com.stemcraft.core.SMFeature;
import com.stemcraft.core.SMJson;
import com.stemcraft.core.SMMessenger;
import com.stemcraft.core.config.SMConfig;
import com.stemcraft.core.event.SMEvent;
import com.stemcraft.core.util.SMXPCalculator;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;

public class SMGameModeInventories extends SMFeature {
    @Override
    protected Boolean onEnable() {
        SMDatabase.runMigration("230805142700_CreateGameModeInventoriesTable", () -> {
            SMDatabase.prepareStatement(
                "CREATE TABLE IF NOT EXISTS gamemode_inventories (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "uuid TEXT NOT NULL," +
                    "death INTEGER DEFAULT 0," +
                    "location TEXT NOT NULL," +
                    "gamemode TEXT NOT NULL," +
                    "xp INTEGER NOT NULL," +
                    "inventory TEXT NOT NULL," +
                    "armour TEXT NOT NULL," +
                    "enderchest TEXT NOT NULL," +
                    "created TIMESTAMP DEFAULT CURRENT_TIMESTAMP)")
                .executeUpdate();
        });

        SMDatabase.runMigration("230805160400_AddReasonToGameModeInventoriesTable", () -> {
            SMDatabase.prepareStatement(
                "ALTER TABLE gamemode_inventories " +
                    "ADD COLUMN reason TEXT DEFAULT ''")
                .executeUpdate();
        });

        SMDatabase.runMigration("230817181600_AddWorldToGameModeInventoriesTable", () -> {
            SMDatabase.prepareStatement(
                "ALTER TABLE gamemode_inventories " +
                    "ADD COLUMN world TEXT DEFAULT 'world'")
                .executeUpdate();

            SMDatabase.prepareStatement(
                "UPDATE gamemode_inventories SET world = 'world' WHERE world = ''").executeUpdate();
        });

        SMDatabase.runMigration("231022154400_ConvertInventoryFormat", () -> {
            PreparedStatement statement = SMDatabase.prepareStatement(
                "SELECT id,inventory,armour,enderchest FROM gamemode_inventories WHERE 1");

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Integer id = resultSet.getInt("id");
                String oldInventory = resultSet.getString("inventory");
                String oldArmour = resultSet.getString("armour");
                String oldEnderchest = resultSet.getString("enderchest");

                try {
                    ReadWriteNBT nbtInventory = NBT.parseNBT(oldInventory);
                    ReadWriteNBT nbtArmour = NBT.parseNBT(oldArmour);
                    ReadWriteNBT nbtEnderchest = NBT.parseNBT(oldEnderchest);

                    ItemStack[] itemStackInventory = NBT.itemStackArrayFromNBT(nbtInventory);
                    ItemStack[] itemStackArmour = NBT.itemStackArrayFromNBT(nbtArmour);
                    ItemStack[] itemStackEnderchest = NBT.itemStackArrayFromNBT(nbtEnderchest);

                    String jsonInventory = SMJson.toJson(itemStackInventory, ItemStack[].class);
                    String jsonArmour = SMJson.toJson(itemStackArmour, ItemStack[].class);
                    String jsonEnderchest = SMJson.toJson(itemStackEnderchest, ItemStack[].class);

                    PreparedStatement updateStatement = SMDatabase.prepareStatement(
                        "UPDATE gamemode_inventories SET inventory = ?, armour = ?, enderchest = ? WHERE id = ?");

                    updateStatement.setString(1, jsonInventory);
                    updateStatement.setString(2, jsonArmour);
                    updateStatement.setString(3, jsonEnderchest);
                    updateStatement.setInt(4, id);
                    updateStatement.executeUpdate();
                    updateStatement.close();
                } catch (Exception e) {
                    e.printStackTrace();

                    PreparedStatement updateStatement = SMDatabase.prepareStatement(
                        "DELETE FROM gamemode_inventories WHERE id = ?");

                    updateStatement.setInt(1, id);
                    updateStatement.executeUpdate();
                    updateStatement.close();
                }
            }
        });

        SMDatabase.runMigration("240109203000_AddHungerGameModeInventoriesTable", () -> {
            SMDatabase.prepareStatement(
                "ALTER TABLE gamemode_inventories " +
                    "ADD COLUMN food INTEGER DEFAULT 20")
                .executeUpdate();

            SMDatabase.prepareStatement(
                "UPDATE gamemode_inventories SET food = 20 WHERE 1").executeUpdate();
        });

        SMDatabase.runMigration("240110075800_AddExtraGameModeInventoriesTable", () -> {
            SMDatabase.prepareStatement(
                "ALTER TABLE gamemode_inventories " +
                    "ADD COLUMN health DOUBLE DEFAULT 20.0")
                .executeUpdate();

            SMDatabase.prepareStatement(
                "ALTER TABLE gamemode_inventories " +
                    "ADD COLUMN saturation FLOAT DEFAULT 0.0")
                .executeUpdate();

            SMDatabase.prepareStatement(
                "UPDATE gamemode_inventories SET health = 20, saturation = 0 WHERE 1").executeUpdate();
        });

        SMEvent.register(PlayerGameModeChangeEvent.class, ctx -> {
            Player player = ctx.event.getPlayer();
            String newGameMode = ctx.event.getNewGameMode().toString();

            if (!this.SaveInventory(player, false, "Gamemode changed to " + newGameMode)) {
                SMMessenger.errorLocale(player, "GMI_FAILED");
                return;
            }

            if (!this.LoadLastInventory(player, newGameMode, player.getLocation().getWorld().getName())) {
                SMMessenger.errorLocale(player, "GMI_FAILED");
                return;
            }
        });

        SMEvent.register(PlayerDeathEvent.class, ctx -> {
            if (ctx.event.getEventName().equalsIgnoreCase("playerdeathevent")) {
                Player player = ctx.event.getEntity();
                this.SaveInventory(player, true, "Player death");
            }
        });

        SMEvent.register(PlayerTeleportEvent.class, ctx -> {
            Player player = ctx.event.getPlayer();

            this.SaveInventory(player, ctx.event.getFrom().getWorld().getName(), false, "Player Teleport");
            this.LoadLastInventory(player, player.getGameMode().toString(), ctx.event.getTo().getWorld().getName());
        });

        SMEvent.register(PlayerJoinEvent.class, (ctx) -> {
            PlayerJoinEvent event = (PlayerJoinEvent) ctx.event;
            Player player = event.getPlayer();

//            this.LoadLastInventory(player, player.getGameMode().toString(), player.getLocation().getWorld().getName());
        });

        SMEvent.register(PlayerQuitEvent.class, ctx -> {
            Player player = ctx.event.getPlayer();

            this.SaveInventory(player, player.getLocation().getWorld().getName(), false, "Player Quit");
        });

        return true;
    }

    public Boolean LoadLastInventory(Player player) {
        String gameMode = player.getGameMode().toString();
        String world = player.getLocation().getWorld().getName();

        return this.LoadLastInventory(player, gameMode, world, false);
    }

    public Boolean LoadLastInventory(Player player, String gameMode, String world) {
        return this.LoadLastInventory(player, gameMode, world, false);
    }

    public Boolean LoadLastInventory(Player player, String gameMode, String world, Boolean death) {
        Boolean success = true;
        Boolean foundInventory = false;
        SMXPCalculator xpc = new SMXPCalculator(player);
        int xp = 0;
        String uuid = player.getUniqueId().toString();
        String inventoryContents = "";
        String armourContents = "";
        String enderChestContents = "";
        int food = 20;
        double health = 20.0d;
        float saturation = 0.0f;

        world = getInventoryWorld(world);

        // Load player state
        try {
            PreparedStatement statement = SMDatabase.prepareStatement(
                "SELECT * FROM gamemode_inventories WHERE uuid = ? AND death = ? AND gamemode = ? AND world = ? ORDER BY created DESC");
            statement.setString(1, uuid);
            statement.setInt(2, 0);
            statement.setString(3, gameMode);
            statement.setString(4, world);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                xp = resultSet.getInt("xp");
                inventoryContents = resultSet.getString("inventory");
                armourContents = resultSet.getString("armour");
                enderChestContents = resultSet.getString("enderchest");
                food = resultSet.getInt("food");
                health = resultSet.getDouble("health");
                saturation = resultSet.getFloat("saturation");

                foundInventory = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            SMMessenger.errorLocale(player, "GMI_FAILED");
            success = false;
        }

        // Clear current potions
        player.getActivePotionEffects().forEach((effect) -> {
            player.removePotionEffect(effect.getType());
        });

        PlayerInventory playerInventory = player.getInventory();
        if (foundInventory) {
            playerInventory.setContents(SMJson.fromJson(ItemStack[].class, inventoryContents));
            playerInventory.setArmorContents(SMJson.fromJson(ItemStack[].class, armourContents));
            player.getEnderChest().setContents(SMJson.fromJson(ItemStack[].class, enderChestContents));
            xpc.setExp(xp);
            player.setFoodLevel(food);
            player.setHealth(health);
            player.setSaturation(saturation);
        } else {
            playerInventory.clear();
            playerInventory.setBoots(null);
            playerInventory.setChestplate(null);
            playerInventory.setLeggings(null);
            playerInventory.setHelmet(null);
            player.getEnderChest().clear();
            xpc.setExp(0);
            player.setFoodLevel(20);
            player.setHealth(20.0d);
            player.setSaturation(0.0f);
        }

        return success;
    }

    public Boolean SaveInventory(Player player) {
        return SaveInventory(player, player.getLocation().getWorld().getName(), false, "");
    }

    public Boolean SaveInventory(Player player, String reason) {
        return SaveInventory(player, player.getLocation().getWorld().getName(), false, reason);
    }

    public Boolean SaveInventory(Player player, Boolean death, String reason) {
        return SaveInventory(player, player.getLocation().getWorld().getName(), death, reason);
    }

    private Boolean SaveInventory(Player player, String world, Boolean death, String reason) {
        Boolean success = true;
        SMXPCalculator xpc = new SMXPCalculator(player);
        String uuid = player.getUniqueId().toString();
        String currentGameMode = player.getGameMode().toString();
        int xp = xpc.getCurrentExp();
        String inventoryContents = SMJson.toJson(player.getInventory().getContents(), ItemStack[].class);
        String armourContents = SMJson.toJson(player.getInventory().getArmorContents(), ItemStack[].class);
        String enderChestContents = SMJson.toJson(player.getEnderChest().getContents(), ItemStack[].class);
        String location = SMJson.toJson(player.getLocation(), Location.class);
        int food = player.getFoodLevel();
        double health = player.getHealth();
        float saturation = player.getSaturation();

        world = getInventoryWorld(world);

        // Save player state
        try {
            PreparedStatement statement = SMDatabase.prepareStatement(
                "INSERT INTO gamemode_inventories (uuid, death, location, gamemode, xp, inventory, armour, enderchest, reason, world, food, health, saturation) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            statement.setString(1, uuid);
            statement.setInt(2, death == true ? 1 : 0);
            statement.setString(3, location);
            statement.setString(4, currentGameMode);
            statement.setInt(5, xp);
            statement.setString(6, inventoryContents);
            statement.setString(7, armourContents);
            statement.setString(8, enderChestContents);
            statement.setString(9, reason);
            statement.setString(10, world);
            statement.setInt(11, food);
            statement.setDouble(12, health);
            statement.setFloat(13, saturation);
            statement.executeUpdate();
            statement.close();

            RemoveOldInventories(player, player.getGameMode(), world);
        } catch (Exception e) {
            e.printStackTrace();
            SMMessenger.errorLocale(player, "GMI_FAILED");
            success = false;
        }

        return success;
    }

    private void RemoveOldInventories(Player player, GameMode gameMode, String world) {
        String uuidString = player.getUniqueId().toString();
        String gameModeString = gameMode.toString();
        Integer maxRows = SMConfig.main().getInt("gamemode-inventories.max-count", 50);

        String sql =
            "WITH RankedRows AS ( " +
                "SELECT *, " +
                "ROW_NUMBER() OVER (ORDER BY created DESC) as rn " +
                "FROM gamemode_inventories " +
                "WHERE uuid = ? " +
                "AND gamemode = ? " +
                "AND world = ? " +
                ") " +
                "DELETE FROM gamemode_inventories " +
                "WHERE id IN ( " +
                "SELECT id " +
                "FROM RankedRows " +
                "WHERE rn > ? " +
                ");";

        try {
            PreparedStatement statement = SMDatabase.prepareStatement(sql);
            statement.setString(1, uuidString);
            statement.setString(2, gameModeString);
            statement.setString(3, world);
            statement.setInt(4, maxRows);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
            SMMessenger.errorLocale(player, "GMI_FAILED");
        }
    }

    /**
     * Returns the world name containing the current world inventory.
     * 
     * @param name
     * @return
     */
    private static String getInventoryWorld(String name) {
        return SMConfig.main().getString("gamemode-inventories.linked-worlds." + name, name);
    }
}
