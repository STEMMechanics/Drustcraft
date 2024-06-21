package com.stemcraft.tabcompletions;

import com.stemcraft.SMMessenger;
import com.stemcraft.SMTabCompletion;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SMTabCompletionSpeed extends SMTabCompletion<Float> {

    public SMTabCompletionSpeed() {
        super("speed");
    }

    @Override
    public List<String> completions(String arg) {
        String[] speed = {"1", "1.5", "1.75", "2"};
        return Arrays.asList(speed);
    }

    @Override
    public Float resolve(String arg) {
        try {
            float speed = Float.parseFloat(arg);

            if (speed < 0.1f) {
                return 0.1f;
            }

            return Math.min(speed, 10.0f);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
