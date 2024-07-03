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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.stemcraft.SMConfig.remove;

@Getter
public class SMPlayerState {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private final Player player;
    private LocalDateTime timestamp;
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

    public SMPlayerState(String playerUuid) {
        this.player = Bukkit.getPlayer(playerUuid);
        reset();
    }

    /**
     * Reset the player state to defaults
     */
    public void reset() {
        this.timestamp = LocalDateTime.now();
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

    /**
     * Save the player state to disk
     */
    public void save() {
        if(this.player == null) {
            return;
        }

        this.timestamp = LocalDateTime.now();
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

        String timestampStr = timestamp.format(formatter);

        String path = "state/" + player.getUniqueId() + "." + timestampStr;

        SMConfig.set(path + ".game-mode", gameMode.name());
        SMConfig.set(path + ".location.world", world.getName());
        SMConfig.set(path + ".location.x", location.getX());
        SMConfig.set(path + ".location.y", location.getY());
        SMConfig.set(path + ".location.z", location.getZ());
        SMConfig.set(path + ".location.yaw", location.getYaw());
        SMConfig.set(path + ".location.pitch", location.getPitch());

        if(experience != 0.0f) SMConfig.set(path + ".experience", experience);
        if(totalExperience != 0) SMConfig.set(path + ".total-experience", totalExperience);
        if(level != 0) SMConfig.set(path + ".level", level);
        if(foodLevel != 20) SMConfig.set(path + ".food", foodLevel);
        if(saturation != 5.0f) SMConfig.set(path + ".saturation", saturation);
        if(exhaustion != 0.0f) SMConfig.set(path + ".exhaustion", exhaustion);
        if(fireTicks != 0) SMConfig.set(path + ".fire-ticks", fireTicks);
        if(remainingAir != 0) SMConfig.set(path + ".remaining-air", remainingAir);
        if(maximumAir != 0) SMConfig.set(path + ".maximum-air", maximumAir);
        if(fallDistance != 0.0f) SMConfig.set(path + ".fall-distance", fallDistance);
        if(absorptionAmount != 0.0d) SMConfig.set(path + ".absorption", absorptionAmount);

        if(!effects.isEmpty()) SMConfig.set(path + ".effects", SMUtilsSerializer.serializePotionEffects(effects));
        SMConfig.set(path + ".main-inventory", SMUtilsSerializer.serializeItemStacks(mainInventory));
        SMConfig.set(path + ".ender-chest", SMUtilsSerializer.serializeItemStacks(enderChest));
        SMConfig.set(path + ".armor-contents", SMUtilsSerializer.serializeItemStacks(armorContents));

        SMConfig.save(path);
    }

    /**
     * Restore the player to this state, ignoring the states location and ggme mode
     */
    public void restore() {
        restore(false, false);
    }

    /**
     * Restore the player to this state
     * @param teleport Teleport the player to the state location
     * @param setGameMode Change the player game mode to the state game mode
     */
    public void restore(boolean teleport, boolean setGameMode) {
        if(teleport) {
            SMPlayer.teleport(player, location);
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

    /**
     * Retrieve a list of saved player states from latest to oldest
     * @param player The player to match
     * @param world Return states saved when located in this world. Use NULL for any world
     * @param gameMode Return states saved when in this game mode. Use NULL for all game modes
     * @return A list of matching player states
     */
    public static List<SMPlayerState> find(Player player, World world, GameMode gameMode) {
        List<SMPlayerState> states = new ArrayList<>();

        String pathPrefix = "state/" + player.getUniqueId();
        List<String> keys = SMConfig.getKeys(pathPrefix);
        sortStateKeys(keys);

        keys.forEach(key -> {
            SMPlayerState state = new SMPlayerState(player);
            String path = pathPrefix + "." + key;
            GameMode keyGameMode = null;

            if(gameMode != null) {
                if(!gameMode.name().equalsIgnoreCase(SMConfig.getString(path + ".game-mode"))) {
                    return;
                }
            }

            if(world != null) {
                if(!world.getName().equalsIgnoreCase(SMConfig.getString(path + ".location.world"))) {
                    return;
                }
            }

            state.timestamp = LocalDateTime.parse(key, formatter);

            state.gameMode = GameMode.valueOf(SMConfig.getString(path + ".game-mode", state.gameMode.name()));
            state.world = Bukkit.getWorld(SMConfig.getString(path + ".location.world", state.world.getName()));
            state.location = new Location(
                state.world,
                SMConfig.getDouble(path + ".location.x", state.location.getX()),
                SMConfig.getDouble(path + ".location.y", state.location.getY()),
                SMConfig.getDouble(path + ".location.z", state.location.getZ()),
                SMConfig.getFloat(path + ".location.yaw", state.location.getYaw()),
                SMConfig.getFloat(path + ".location.pitch", state.location.getPitch())
            );

            state.experience = SMConfig.getFloat(path + ".experience", state.experience);
            state.totalExperience = SMConfig.getInt(path + ".total-experience", state.totalExperience);
            state.level = SMConfig.getInt(path + ".level", state.level);
            state.foodLevel = SMConfig.getInt(path + ".food", state.foodLevel);
            state.saturation = SMConfig.getFloat(path + ".saturation", state.saturation);
            state.exhaustion = SMConfig.getFloat(path + ".exhaustion", state.exhaustion);
            state.fireTicks = SMConfig.getInt(path + ".fire-ticks", state.fireTicks);
            state.remainingAir = SMConfig.getInt(path + ".remaining-air", state.remainingAir);
            state.maximumAir = SMConfig.getInt(path + ".maximum-air", state.maximumAir);
            state.fallDistance = SMConfig.getFloat(path + ".fall-distance", state.fallDistance);
            state.absorptionAmount = SMConfig.getDouble(path + ".absorption", state.absorptionAmount);

            state.effects = SMUtilsSerializer.deserializePotionEffects(SMConfig.getString(path + ".effects"));
            state.mainInventory = SMUtilsSerializer.deserializeItemStacks(SMConfig.getString(path + ".main-inventory"));
            state.enderChest = SMUtilsSerializer.deserializeItemStacks(SMConfig.getString(path + ".ender-chest"));
            state.armorContents = SMUtilsSerializer.deserializeItemStacks(SMConfig.getString(path + ".armor-contents"));

            states.add(state);
        });

        return states;
    }

    /**
     * Sort formatted date keys from latest to oldest
     * @param keys A list of keys to sort
     */
    private static void sortStateKeys(List<String> keys) {
        Comparator<String> timestampComparator = (key1, key2) -> {
            LocalDateTime dateTime1 = LocalDateTime.parse(key1, formatter);
            LocalDateTime dateTime2 = LocalDateTime.parse(key2, formatter);
            return dateTime2.compareTo(dateTime1); // Compare in reverse order (latest to earliest)
        };

        keys.sort(timestampComparator);
    }

    /**
     * Cleanup the player state data by removing states beyond the storage limits
     *
     * @param player The player to clean up
     * @param gameMode The game mode to clean up
     */
    private static void cleanup(Player player, GameMode gameMode) {
        int maxStatesPerGameMode = 10;
        String pathPrefix = "state/" + player.getUniqueId();
        List<String> keys = SMConfig.getKeys(pathPrefix);
        String gameModeStr = gameMode.name();
        List<String> foundKeys = new ArrayList<>();

        sortStateKeys(keys);
        keys.forEach(key -> {
            String path = pathPrefix + "." + key;
            if(SMConfig.getString(path + ".game-mode").equalsIgnoreCase(gameModeStr)) {
                foundKeys.add(key);
            }
        });

        if (foundKeys.size() > maxStatesPerGameMode) {
            List<String> keysToRemove = foundKeys.subList(maxStatesPerGameMode, foundKeys.size());
            keysToRemove.forEach(key -> remove(pathPrefix + "." + key));
        }

        SMConfig.save(pathPrefix);
    }
}
