package com.stemcraft.utils;

import org.bukkit.World;

public class SMUtilsWorld {
    public static String getOverworldName(String name) {
        if (name.endsWith("_nether")) {
            return name.substring(0, name.length() - "_nether".length());
        }

        if (name.endsWith("_the_end")) {
            return name.substring(0, name.length() - "_the_end".length());
        }

        return name;
    }

    public static String toRealTime(World world) {
        long time = world.getTime();

        // Adjust the time to start at 6:00 AM instead of midnight
        long adjustedTime = (time + 6000) % 24000;

        // Convert the adjusted time to hours and minutes
        int hours = (int) (adjustedTime / 1000);
        int minutes = (int) ((adjustedTime % 1000) / 16.6667);

        // Convert to 12-hour time format with AM/PM
        String am_pm = (hours < 12) ? "AM" : "PM";
        hours = hours % 12;
        hours = (hours == 0) ? 12 : hours; // Convert hour 0 to 12

        return String.format("%d:%02d %s", hours, minutes, am_pm);
    }
}
