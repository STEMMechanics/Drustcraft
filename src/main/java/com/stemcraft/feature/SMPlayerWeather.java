package com.stemcraft.feature;

import java.util.HashMap;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import com.stemcraft.core.SMFeature;
import com.stemcraft.core.SMMessenger;
import com.stemcraft.core.command.SMCommand;

public class SMPlayerWeather extends SMFeature {
    @Override
    protected Boolean onEnable() {
        String[] options = {"reset", "clear", "rain", "lookup"};

        new SMCommand("playerweather")
            .alias("pweather")
            .permission("stemcraft.command.pweather")
            .tabComplete(options, "{player}")
            .action(ctx -> {
                Player targetPlayer = ctx.player;
                String option = "lookup";

                HashMap<String, String> weatherTypes = new HashMap<>();
                weatherTypes.put("clear", "CLEAR");
                weatherTypes.put("rain", "DOWNFALL");

                if (ctx.fromConsole() && ctx.args.size() == 0) {
                    ctx.returnErrorLocale("PWEATHER_USAGE");
                }

                if (ctx.args.size() > 0) {
                    ctx.checkInArrayLocale(options, ctx.args.get(0), "PWEATHER_USAGE");
                    option = ctx.args.get(0).toLowerCase();

                    targetPlayer = ctx.getArgAsPlayer(2, ctx.player);
                }

                ctx.checkNotNullLocale(targetPlayer, "CMD_PLAYER_NOT_FOUND");
                ctx.checkPermission(targetPlayer == ctx.sender, "stemcraft.pweather.other");

                if ("lookup".equals(option)) {
                    String weatherValue = "server";

                    WeatherType playerWeather = targetPlayer.getPlayerWeather();
                    if (playerWeather != null) {
                        if (playerWeather == WeatherType.CLEAR) {
                            weatherValue = "clear";
                        } else if (playerWeather == WeatherType.DOWNFALL) {
                            weatherValue = "rain";
                        } else {
                            weatherValue = "unknown";
                        }
                    }

                    if (targetPlayer == ctx.sender) {
                        ctx.returnInfoLocale("PWEATHER_GET", "type", weatherValue);
                    } else {
                        ctx.returnInfoLocale("PWEATHER_GET_FOR", "player", targetPlayer.getName(), "type",
                            weatherValue);
                    }
                } else if ("reset".equals(option)) {
                    targetPlayer.resetPlayerWeather();

                    if (targetPlayer == ctx.sender) {
                        ctx.returnInfoLocale("PWEATHER_RESET");
                    } else {
                        SMMessenger.infoLocale(ctx.sender, "PWEATHER_RESET_FOR", "player", targetPlayer.getName());
                        SMMessenger.infoLocale(targetPlayer, "PWEATHER_RESET_BY", "player", ctx.senderName());
                    }
                } else {
                    String weatherValue = weatherTypes.get(option);

                    try {
                        WeatherType weatherType = WeatherType.valueOf(weatherValue);

                        targetPlayer.setPlayerWeather(weatherType);

                        if (targetPlayer == ctx.sender) {
                            ctx.returnInfoLocale("PWEATHER_SET", "type", option);
                        } else {
                            SMMessenger.infoLocale(ctx.sender, "PWEATHER_SET_FOR", "player", targetPlayer.getName(),
                                "type", option);
                            SMMessenger.infoLocale(targetPlayer, "PWEATHER_SET_BY", "player", ctx.senderName(), "type",
                                option);
                        }
                    } catch (IllegalArgumentException e) {
                        /* Do nothing */
                    }
                }
            })
            .register();

        return true;
    }
}
