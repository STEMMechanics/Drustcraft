package com.stemcraft;

import com.stemcraft.utils.SMUtilsMath;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;

@SuppressWarnings("UnusedReturnValue")
@Setter
@Getter
public class SMRegion {
    private static HashMap<Player, List<SMRegion>> playerRegionList = new HashMap<>();
    private static List<SMRegion> regionList = new ArrayList<>();

    public static final String TYPE_CUBOID = "cuboid";
    public static final String TYPE_POLY2D = "poly2d";

    @Getter
    private String name;
    private List<Location> points;
    @Getter @Setter
    private int priority;
    @Getter
    private String type;
    @Getter
    private boolean valid;
    @Getter
    private World world;

    @FunctionalInterface
    public interface PlayerEnterCallback {
        void onEnter(Player player);
    }

    @FunctionalInterface
    public interface PlayerExitCallback {
        void onExit(Player player);
    }

    @FunctionalInterface
    public interface BlockBreakCallback {
        boolean onBlockBreak(Block block, Player player);
    }

    @FunctionalInterface
    public interface BlockPlaceCallback {
        boolean onBlockPlace(Block block, Player player);
    }

    private List<PlayerEnterCallback> enterCallbackList = new ArrayList<>();
    private List<PlayerExitCallback> exitCallbackList = new ArrayList<>();
    private List<BlockBreakCallback> blockBreakCallbackList = new ArrayList<>();
    private List<BlockPlaceCallback> blockPlaceCallbackList = new ArrayList<>();

    public Location teleportEnter;
    public Location teleportExit;
    private Boolean allowDrops;
    private Boolean allowBlockBreak;
    private Boolean allowBlockPlace;

    private static final Random random = new Random();

    /**
     * Constructor
     */
    public SMRegion() {
        this.name = null;
        this.points = new ArrayList<>();
        this.priority = 0;
        this.type = SMRegion.TYPE_CUBOID;
        this.valid = false;
    }

    public SMRegion(List<Location> points) {
        this.name = null;
        this.points = points;
        this.priority = 0;

        if(points.size() == 2) {
            this.type = SMRegion.TYPE_CUBOID;
            this.valid = true;
        } else {
            this.type = SMRegion.TYPE_POLY2D;
            this.valid = points.size() >= 3;
        }
    }

    public SMRegion(String name, List<Location> points) {
        this.name = name;
        this.points = points;
        this.priority = 0;

        if(points.size() == 2) {
            this.type = SMRegion.TYPE_CUBOID;
            this.valid = true;
        } else {
            this.type = SMRegion.TYPE_POLY2D;
            this.valid = points.size() >= 3;
        }
    }

    public SMRegion(String name, List<Location> points, int priority) {
        this.name = name;
        this.points = points;
        this.priority = priority;

        if(points.size() == 2) {
            this.type = SMRegion.TYPE_CUBOID;
            this.valid = true;
        } else {
            this.type = SMRegion.TYPE_POLY2D;
            this.valid = points.size() >= 3;
        }
    }

    /**
     * Add a point to the region
     * @param location The location to add
     */
    public void addPoint(Location location) {
        if(this.type.equals(SMRegion.TYPE_POLY2D)) {
            points.add(location);
            this.valid = points.size() >= 3;
        } else {
            if(points.size() < 2) {
                points.add(location);
            } else {
                points.set(1, location);
            }

            this.valid = points.size() == 2;
        }
    }

    public void addPoints(List<Location> locations) {
        points.addAll(locations);
    }

    public void setPoint(int index, Location location) {
        points.set(index, location);
    }

    public void removePoint(int index) {
        points.remove(index);
    }

    public void clearPoints() {
        points.clear();
    }





    public void onPlayerEnter(PlayerEnterCallback callback) {
        enterCallbackList.add(callback);
    }

    public void onPlayerExit(PlayerExitCallback callback) {
        exitCallbackList.add(callback);
    }

    public void onBlockBreak(BlockBreakCallback callback) {
        blockBreakCallbackList.add(callback);
    }

    public void onBlockPlace(BlockPlaceCallback callback) {
        blockPlaceCallbackList.add(callback);
    }

    private void handlePlayerEnter(Player player) {
        enterCallbackList.forEach(cb -> {
            cb.onEnter(player);
        });
    }

    private void handlePlayerExit(Player player) {
        exitCallbackList.forEach(cb -> {
            cb.onExit(player);
        });
    }

    public boolean handleBlockBreak(Block block, Player player) {
        for(BlockBreakCallback cb : blockBreakCallbackList) {
            if(cb.onBlockBreak(block, player)) {
                return true;
            }
        }

        return !allowBlockBreak;
    }

    public boolean handleBlockPlace(Block block, Player player) {
        for(BlockPlaceCallback cb : blockPlaceCallbackList) {
            if(cb.onBlockPlace(block, player)) {
                return true;
            }
        }

        return !allowBlockPlace;
    }

    public boolean setName(String newName) {
        String oldName = name;

        if(newName != null && newName.isEmpty()) {
            String formattedNewName = newName.toLowerCase();

            for(SMRegion region : regionList) {
                if(region.getName().equalsIgnoreCase(formattedNewName)) {
                    return false;
                }
            }

            name = formattedNewName;
        }

        if(oldName != null) {
            SMConfig.remove("regions." + oldName);
        }

        SMConfig.save("regions");
        return true;
    }

    /**
     * Delete the region
     */
    public void delete() {
        if(name != null) {
            SMConfig.remove("regions." + name);
            SMConfig.save("regions");
        }

        playerRegionList.forEach((player, list) -> list.removeIf(item -> item.getName().equals(name)));
        regionList.remove(this);
    }

    /**
     * Save a region to disk
     */
    public void save(String path) {
        if(name == null || name.isEmpty() || points == null || points.isEmpty() || points.size() < 2) {
            return;
        }

        if(path == null) {
            path = "regions." + name;
        }

        SMConfig.remove(path);

        SMConfig.set(path + ".world", Objects.requireNonNull(points.get(0).getWorld()).getName());
        SMConfig.set(path + ".priority", priority);

        if(points.size() == 2) {
            SMConfig.set(path + ".type", SMRegion.TYPE_CUBOID);

            Map<String, Integer> minPoint = Map.of(
                    "x", points.get(0).getBlockX(),
                    "y", points.get(0).getBlockY(),
                    "z", points.get(0).getBlockZ()
            );

            Map<String, Integer> maxPoint = Map.of(
                    "x", points.get(1).getBlockX(),
                    "y", points.get(1).getBlockY(),
                    "z", points.get(1).getBlockZ()
            );

            SMConfig.set(path + ".min", minPoint);
            SMConfig.set(path + ".max", maxPoint);
        } else {
            SMConfig.set(path + ".type", SMRegion.TYPE_POLY2D);
            List<Map<String, Integer>> pointList = new ArrayList<>();
            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;

            for (Location location : points) {
                Map<String, Integer> point = Map.of(
                        "x", location.getBlockX(),
                        "z", location.getBlockZ()
                );
                pointList.add(point);

                int y = location.getBlockY();
                STEMCraft.info(String.valueOf(y));
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }

            SMConfig.set(path + ".points", pointList);
            SMConfig.set(path + ".min-y", minY);
            SMConfig.set(path + ".max-y", maxY);
        }

        if(teleportEnter != null && teleportEnter.getWorld() != null) {
            SMConfig.set(path + ".flags.teleport-enter", teleportEnter.getWorld().getName() + "," + teleportEnter.getX() + "," + teleportEnter.getY() + "," + teleportEnter.getZ());
        }

        if(teleportExit != null && teleportExit.getWorld() != null) {
            SMConfig.set(path + ".flags.teleport-exit", teleportExit.getWorld().getName() + "," + teleportExit.getX() + "," + teleportExit.getY() + "," + teleportExit.getZ());
        }

        if(allowDrops != null) {
            SMConfig.set(path + ".flags.allow-drops", allowDrops);
        }

        SMConfig.save(path);
    }

    public void save() {
        save(null);
    }

    /**
     * Create a new region
     * @param name The unique name for the region
     * @param points The boundary points of the region
     * @return The region object or null
     */
    public static SMRegion create(String name, List<Location> points) {
        if(name != null) {
            for(SMRegion region : regionList) {
                if (region.getName().equalsIgnoreCase(name)) {
                    return null;
                }
            }
        }

        SMRegion region = new SMRegion();
        region.name = name;
        region.points = points;

        for(Location loc : points) {
            STEMCraft.info(String.valueOf(loc.getBlockY()));
        }

        regionList.add(region);

        if(name != null) {
            region.save();
        }

        return region;
    }

    /**
     * Get an existing region object by name
     * @param name The name of the region
     * @return The region object or null
     */
    public static SMRegion get(String name) {
        for(SMRegion region : regionList) {
            if(region.getName().equalsIgnoreCase(name)) {
                return region;
            }
        }

        return null;
    }

    /**
     * Returns is a region with the name exists
     * @param name The name to check
     * @return If the region exists
     */
    public static boolean exists(String name) {
        return get(name) != null;
    }

    /**
     * Get a region name list
     * @return The region names
     */
    public static List<String> getRegionNames() {
        List<String> list = new ArrayList<>();

        for(SMRegion region : regionList) {
            String name = region.getName();
            if(name != null) {
                list.add(name);
            }
        }

        return list;
    }

    /**
     * Get a region list
     * @return The region list
     */
    public static List<SMRegion> getRegions() {
        return regionList;
    }





    public static boolean updatePlayerRegions(Player player) {
        return updatePlayerRegions(player, player.getLocation());
    }

    /**
     * Reload the regions players are inside. This will adjust any flags
     */
    public static void reloadPlayerRegions() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            playerRegionList.put(player, findRegions(player.getLocation()));
        }
    }

    /**
     * Sort the passed list by priority
     * @param list The list to sort
     */

    public static SMRegion loadFromConfig(String path, String name) {
        String worldName = SMConfig.getString(path + ".world", "");
        if(worldName.isEmpty()) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if(world == null) {
            return null;
        }

        List<Location> points = new ArrayList<>();

        String type = SMConfig.getString(path + ".type", "");
        if(type.equalsIgnoreCase(SMRegion.TYPE_CUBOID)) {
            Map<String, Object> minPointData = SMConfig.getMap(path + ".min");
            Map<String, Object> maxPointData = SMConfig.getMap(path + ".max");

            Object minXObj = minPointData.get("x");
            Object minYObj = minPointData.get("y");
            Object minZObj = minPointData.get("z");
            Object maxXObj = maxPointData.get("x");
            Object maxYObj = maxPointData.get("y");
            Object maxZObj = maxPointData.get("z");

            if (!(minXObj instanceof Integer) || !(minYObj instanceof Integer) || !(minZObj instanceof Integer) ||
                    !(maxXObj instanceof Integer) || !(maxYObj instanceof Integer) || !(maxZObj instanceof Integer)) {
                return null;
            }

            int minX = (int) minXObj;
            int minY = (int) minYObj;
            int minZ = (int) minZObj;
            int maxX = (int) maxXObj;
            int maxY = (int) maxYObj;
            int maxZ = (int) maxZObj;

            Location minLocation = new Location(world, minX, minY, minZ);
            Location maxLocation = new Location(world, maxX, maxY, maxZ);

            points.add(minLocation);
            points.add(maxLocation);
        } else if(type.equalsIgnoreCase(SMRegion.TYPE_POLY2D)) {
            List<Map<?, ?>> pointList = SMConfig.getMapList(path + ".points");
            int minY = SMConfig.getInt(path + ".min-y");
            int maxY = SMConfig.getInt(path + ".max-y");

            if (pointList == null || pointList.isEmpty()) {
                return null;
            }

            Map<?, ?> firstPointData = pointList.get(0);
            Object firstXObj = firstPointData.get("x");
            Object firstZObj = firstPointData.get("z");

            if (!(firstXObj instanceof Integer) || !(firstZObj instanceof Integer)) {
                return null;
            }

            int firstX = (int) firstXObj;
            int firstZ = (int) firstZObj;
            points.add(new Location(world, firstX, maxY, firstZ));

            for (int i = 1; i < pointList.size(); i++) {
                Map<?, ?> pointData = pointList.get(i);
                Object xObj = pointData.get("x");
                Object zObj = pointData.get("z");

                if (!(xObj instanceof Integer) || !(zObj instanceof Integer)) {
                    return null;
                }

                int x = (int) xObj;
                int z = (int) zObj;
                points.add(new Location(world, x, minY, z));
            }
        } else {
            return null;
        }

        SMRegion region = new SMRegion();
        if(name != null) {
            region.name = name.toLowerCase();
        } else {
            region.name = null;
        }

        region.points = points;

        if(SMConfig.contains(path + ".flags.teleport-enter")) {
            String[] values = SMConfig.getString(path + ".flags.teleport-enter", "").split(",");
            if(values.length >= 4) {
                try {
                    World flagWorld = Bukkit.getWorld(values[0]);
                    double x = Double.parseDouble(values[1]);
                    double y = Double.parseDouble(values[2]);
                    double z = Double.parseDouble(values[3]);
                    if(flagWorld != null) {
                        region.teleportEnter = new Location(flagWorld, x, y, z);
                    }
                } catch (NumberFormatException e) {
                    // Skip this point if any value can't be parsed as a double
                }
            }
        }

        regionList.add(region);
        return region;
    }

    public static SMRegion loadFromConfig(String path) {
        return loadFromConfig(path, null);
    }

    public static void loadRegions() {
        regionList.clear();

        List<String> names = SMConfig.getKeys("regions");
        names.forEach(name -> {
            loadFromConfig("regions" + name, name);
        });
    }

    /**
     * Returns a random location within the region bounds.
     * @return A random Location within the region, or null if the region is invalid.
     */
    public Location randomLocation() {
        if (points.size() < 2) {
            return null;
        }

        World world = points.get(0).getWorld();
        if (world == null) {
            return null;
        }

        if (points.size() == 2) {
            // Cuboid region
            Location min = points.get(0);
            Location max = points.get(1);

            int x = randomBetween(min.getBlockX(), max.getBlockX());
            int y = randomBetween(min.getBlockY(), max.getBlockY());
            int z = randomBetween(min.getBlockZ(), max.getBlockZ());

            return new Location(world, x, y, z);
        } else {
            // Poly2D region
            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;
            for (Location point : points) {
                minY = Math.min(minY, point.getBlockY());
                maxY = Math.max(maxY, point.getBlockY());
            }

            Location randomLoc;
            do {
                double minX = points.stream().mapToDouble(Location::getX).min().orElse(0);
                double maxX = points.stream().mapToDouble(Location::getX).max().orElse(0);
                double minZ = points.stream().mapToDouble(Location::getZ).min().orElse(0);
                double maxZ = points.stream().mapToDouble(Location::getZ).max().orElse(0);

                double x = randomBetween(minX, maxX);
                double y = randomBetween(minY, maxY);
                double z = randomBetween(minZ, maxZ);

                randomLoc = new Location(world, x, y, z);
            } while (!SMUtilsMath.insideRegion(randomLoc, points));

            return randomLoc;
        }
    }

    /**
     * Returns a random safe location within the region bounds suitable for spawning.
     * @return A random safe Location within the region, or null if no safe location is found after multiple attempts.
     */
    public Location randomSpawnLocation() {
        final int MAX_ATTEMPTS = 50;
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            Location randomLoc = randomLocation();
            if (randomLoc == null) {
                return null;
            }

            Location safeLocation = findSafeY(randomLoc);
            if (safeLocation != null) {
                return safeLocation;
            }
        }
        return null; // Couldn't find a safe location after MAX_ATTEMPTS
    }

    private Location findSafeY(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();

        for (int y = world.getMaxHeight(); y >= world.getMinHeight(); y--) {
            Block block = world.getBlockAt(x, y, z);
            Block above = world.getBlockAt(x, y + 1, z);
            Block below = world.getBlockAt(x, y - 1, z);

            if (block.getType() == Material.AIR &&
                    above.getType() == Material.AIR &&
                    below.getType().isSolid() &&
                    !below.isLiquid()) {
                return new Location(world, x + 0.5, y, z + 0.5);
            }
        }

        return null; // No safe Y found
    }

    private static int randomBetween(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    private static double randomBetween(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }


    // TODO: Implement and order properly
    public boolean getAllowDrops() {
        return allowDrops;
    }
}
