package com.stemcraft.configtypes;

import com.stemcraft.SMConfig;
import com.stemcraft.interfaces.SMConfigType;
import com.stemcraft.SMRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SMConfigTypeRegion implements SMConfigType<SMRegion> {

    public void register() {
        SMConfig.registerType(SMRegion.class, this);
    }

    public SMRegion load(String path) {
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
        region.setName(SMConfig.getString(path + ".name", ""));
        region.setPoints(points);

        return region;
    }

    public void save(String path, SMRegion value) {
        SMConfig.set(path + ".world", value.getWorld().getName());
        SMConfig.set(path + ".name", value.getName());

        List<Location> points = value.getPoints();
        if(value.getType().equalsIgnoreCase(SMRegion.TYPE_CUBOID)) {
            Location minLocation = points.get(0);
            Location maxLocation = points.get(1);

            SMConfig.set(path + ".type", SMRegion.TYPE_CUBOID);
            SMConfig.set(path + ".min.x", minLocation.getBlockX());
            SMConfig.set(path + ".min.y", minLocation.getBlockY());
            SMConfig.set(path + ".min.z", minLocation.getBlockZ());
            SMConfig.set(path + ".max.x", maxLocation.getBlockX());
            SMConfig.set(path + ".max.y", maxLocation.getBlockY());
            SMConfig.set(path + ".max.z", maxLocation.getBlockZ());
        } else if(value.getType().equalsIgnoreCase(SMRegion.TYPE_POLY2D)) {
            List<Map<String, Integer>> pointList = new ArrayList<>();
            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;

            for(Location point : points) {
                int x = point.getBlockX();
                int z = point.getBlockZ();
                pointList.add(Map.of("x", x, "z", z));

                if(point.getBlockY() < minY) {
                    minY = point.getBlockY();
                }

                if(point.getBlockY() > maxY) {
                    maxY = point.getBlockY();
                }
            }

            SMConfig.set(path + ".type", SMRegion.TYPE_POLY2D);
            SMConfig.set(path + ".points", pointList);
            SMConfig.set(path + ".min-y", minY);
            SMConfig.set(path + ".max-y", maxY);
        }
    }
}
