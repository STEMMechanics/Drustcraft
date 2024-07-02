package com.stemcraft;

import com.stemcraft.utils.SMUtilsSerializer;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Getter
public class SMPlayerState {
    private final Player player;
    private Location location;
    private GameMode gameMode;
    private World world;
    private float experience;
    private int totalExperience;
    private int level;
    private int foodLevel;
    private float saturation;
    private float exhaustion;
    private int fireTicks;
    private int remainingAir;
    private int maximumAir;
    private float fallDistance;
    private double absorptionAmount;

    private List<PotionEffect> effects;

    private ItemStack[] enderChest;
    private ItemStack[] mainInventory;
    private ItemStack[] armorContents;

    public SMPlayerState(Player player) {
        this.player = player;
        reset();
    }

    public void reset() {
        this.world = Bukkit.getWorlds().get(0);
        this.location = world.getSpawnLocation();
        this.gameMode = GameMode.SURVIVAL;
        this.experience = 0.0f;
        this.totalExperience = 0;
        this.level = 0;
        this.foodLevel = 20;
        this.saturation = 5.0f;
        this.exhaustion = 0.0f;
        this.fireTicks = 0;
        this.remainingAir = 0;
        this.maximumAir = 0;
        this.fallDistance = 0;
        this.absorptionAmount = 0;

        this.effects = new ArrayList<>();

        this.enderChest = new ItemStack[63];
        this.mainInventory = new ItemStack[36];
        this.armorContents = new ItemStack[4];
    }

    public void save() {
        this.location = player.getLocation();
        this.gameMode = player.getGameMode();
        this.world = player.getWorld();
        this.experience = player.getExp();
        this.totalExperience = player.getTotalExperience();
        this.level = player.getLevel();
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.exhaustion = player.getExhaustion();
        this.fireTicks = player.getFireTicks();
        this.remainingAir = player.getRemainingAir();
        this.maximumAir = player.getMaximumAir();
        this.fallDistance = player.getFallDistance();
        this.absorptionAmount = player.getAbsorptionAmount();

        this.effects = new ArrayList<>(player.getActivePotionEffects());

        this.enderChest = player.getEnderChest().getContents().clone();
        this.mainInventory = player.getInventory().getContents().clone();
        this.armorContents = player.getInventory().getArmorContents().clone();

//        try {
//            PreparedStatement statement = SMDatabase.prepareStatement(
//                    "INSERT INTO player_state (player, game_mode, world, x, y, z, yaw, pitch, experience, total_experience, level, food_level, saturation, exhaustion, fire_ticks, remaining_air, maximum_air, fall_distance, absorption_amount, effects, ender_chest, main_inventory, armor_contents) " +
//                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
//            statement.setString(1, player.getUniqueId().toString());
//            statement.setString(2, gameMode.name());
//            statement.setString(3, world.getName());
//            statement.setDouble(4, location.getX());
//            statement.setDouble(5, location.getY());
//            statement.setDouble(6, location.getZ());
//            statement.setFloat(7, location.getYaw());
//            statement.setFloat(8, location.getPitch());
//            statement.setFloat(9, experience);
//            statement.setInt(10, totalExperience);
//            statement.setInt(11, level);
//            statement.setInt(12, foodLevel);
//            statement.setFloat(13, saturation);
//            statement.setFloat(14, exhaustion);
//            statement.setInt(15, fireTicks);
//            statement.setInt(16, remainingAir);
//            statement.setInt(17, maximumAir);
//            statement.setFloat(18, fallDistance);
//            statement.setDouble(19, absorptionAmount);
//            statement.setString(20, SMUtilsSerializer.serializePotionEffects(effects));
//            statement.setString(21, SMUtilsSerializer.serializeItemStacks(enderChest));
//            statement.setString(22, SMUtilsSerializer.serializeItemStacks(mainInventory));
//            statement.setString(23, SMUtilsSerializer.serializeItemStacks(armorContents));
//
//            statement.executeUpdate();
//        } catch (Exception e) {
//            STEMCraft.error(e);
//        }

        LocalDateTime timestamp = LocalDateTime.now();
        String timestampStr = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        String path = "state/" + player.getUniqueId() + "." + timestampStr;
        SMConfig.set(path + ".game_mode", gameMode.name());
        SMConfig.set(path + ".effects", SMUtilsSerializer.serializePotionEffects(effects));
        SMConfig.set(path + ".main_inventory", SMUtilsSerializer.serializeItemStacks(mainInventory));
        SMConfig.save(path);
    }

    public void restore() {
        restore(false, false);
    }

    public void restore(boolean teleport, boolean setGameMode) {
        if(teleport) {
            player.teleport(location);
        }
        if (setGameMode) {
            player.setGameMode(gameMode);
        }
        player.setExp(experience);
        player.setTotalExperience(totalExperience);
        player.setLevel(level);
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setExhaustion(exhaustion);
        player.setFireTicks(fireTicks);
        player.setRemainingAir(remainingAir);
        player.setMaximumAir(maximumAir);
        player.setFallDistance(fallDistance);
        player.setAbsorptionAmount(absorptionAmount);

        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        for (PotionEffect effect : effects) {
            player.addPotionEffect(new PotionEffect(effect.getType(), effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.hasIcon()));
        }

        player.getEnderChest().setContents(enderChest);
        player.getInventory().setContents(mainInventory);
        player.getInventory().setArmorContents(armorContents);
    }

    public static List<SMPlayerState> find(Player player, World world, GameMode gameMode) {
        List<SMPlayerState> states = new ArrayList<>();

        String path = "state/" + player.getUniqueId();
        List<String> keys = SMConfig.getKeys(path);
        sortStateKeys(keys);

        keys.forEach(key -> {
            SMPlayerState state = new SMPlayerState(player);
            String keyPath = path + "." + key;
            GameMode keyGameMode = null;

            if(SMConfig.contains(keyPath + ".game_mode")) {
                keyGameMode = GameMode.valueOf(SMConfig.getString(keyPath + ".game_mode"));
            }

            if(keyGameMode == gameMode) {
                state.gameMode = GameMode.valueOf(SMConfig.getString(keyPath + ".game_mode"));
                state.effects = SMUtilsSerializer.deserializePotionEffects(SMConfig.getString(keyPath + ".effects"));
                state.mainInventory = SMUtilsSerializer.deserializeItemStacks(SMConfig.getString(keyPath + ".main_inventory"));

                states.add(state);
            }
        });

//        try {
//            PreparedStatement statement = SMDatabase.prepareStatement(
//                    "SELECT * FROM player_state WHERE player = ? AND world = ? AND game_mode = ? ORDER BY created DESC");
//            statement.setString(1, player.getUniqueId().toString());
//            statement.setString(2, world.getName());
//            statement.setString(3, gameMode.name());
//
//            ResultSet resultSet = statement.executeQuery();
//            while (resultSet.next()) {
//                SMPlayerState state = new SMPlayerState(player);
//                state.gameMode = GameMode.valueOf(resultSet.getString("game_mode"));
//                state.location.setX(resultSet.getDouble("x"));
//                state.location.setY(resultSet.getDouble("y"));
//                state.location.setZ(resultSet.getDouble("z"));
//                state.location.setYaw(resultSet.getFloat("yaw"));
//                state.location.setPitch(resultSet.getFloat("pitch"));
//
//                state.experience = resultSet.getFloat("experience");
//                state.totalExperience = resultSet.getInt("total_experience");
//                state.level = resultSet.getInt("level");
//                state.foodLevel = resultSet.getInt("food_level");
//                state.saturation = resultSet.getFloat("saturation");
//                state.exhaustion = resultSet.getFloat("exhaustion");
//                state.fireTicks = resultSet.getInt("fire_ticks");
//                state.remainingAir = resultSet.getInt("remaining_air");
//                state.maximumAir = resultSet.getInt("maximum_air");
//                state.fallDistance = resultSet.getFloat("fall_distance");
//                state.absorptionAmount = resultSet.getDouble("absorption_amount");
//
//                state.effects = SMUtilsSerializer.deserializePotionEffects(resultSet.getString("effects"));
//
//                state.enderChest = SMUtilsSerializer.deserializeItemStacks(resultSet.getString("ender_chest"));
//                state.mainInventory = SMUtilsSerializer.deserializeItemStacks(resultSet.getString("main_inventory"));
//                state.armorContents = SMUtilsSerializer.deserializeItemStacks(resultSet.getString("armor_contents"));
//
//                states.add(state);
//            }
//        } catch (Exception e) {
//            STEMCraft.error(e);
//        }

        return states;
    }

    private static void sortStateKeys(List<String> keys) {
        Comparator<String> timestampComparator = (key1, key2) -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
            LocalDateTime dateTime1 = LocalDateTime.parse(key1, formatter);
            LocalDateTime dateTime2 = LocalDateTime.parse(key2, formatter);
            return dateTime2.compareTo(dateTime1); // Compare in reverse order (latest to earliest)
        };

        keys.sort(timestampComparator);
    }
}
