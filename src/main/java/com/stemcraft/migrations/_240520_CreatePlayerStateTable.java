package com.stemcraft.migrations;

import com.stemcraft.SMDatabase;
import com.stemcraft.SMDatabaseMigration;
import com.stemcraft.STEMCraft;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class _240520_CreatePlayerStateTable extends SMDatabaseMigration {

    @Override
    public void migrate() {
        String sql = "CREATE TABLE IF NOT EXISTS player_state (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "player TEXT NOT NULL," +
                "game_mode TEXT NOT NULL," +
                "x DOUBLE NOT NULL," +
                "y DOUBLE NOT NULL," +
                "z DOUBLE NOT NULL," +
                "yaw FLOAT NOT NULL," +
                "pitch FLOAT NOT NULL," +
                "world TEXT NOT NULL," +
                "experience FLOAT NOT NULL," +
                "total_experience INTEGER NOT NULL," +
                "level INTEGER NOT NULL," +
                "food_level INTEGER NOT NULL," +
                "saturation FLOAT NOT NULL," +
                "exhaustion FLOAT NOT NULL," +
                "fire_ticks INTEGER NOT NULL," +
                "remaining_air INTEGER NOT NULL," +
                "maximum_air INTEGER NOT NULL," +
                "fall_distance FLOAT NOT NULL," +
                "absorption_amount DOUBLE NOT NULL," +
                "effects TEXT NOT NULL," +
                "main_inventory TEXT NOT NULL," +
                "armor_contents TEXT NOT NULL," +
                "ender_chest TEXT NOT NULL," +
                "created TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";
        try {
            PreparedStatement stmt = SMDatabase.prepareStatement(sql);
            stmt.executeUpdate();
        } catch (SQLException e) {
            STEMCraft.error(e);
        }
    }
}
