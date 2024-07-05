package com.stemcraft;

import java.util.ArrayList;
import java.util.List;

public class SMCommandArgs {
    private final boolean fromConsole;
    private final List<String> args;
    private final List<String> dashArgs;
    private int index = 0;
    private int length;

    public SMCommandArgs(boolean fromConsole, String[] inputArgs) {
        this.fromConsole = fromConsole;

        this.args = new ArrayList<>();
        this.dashArgs = new ArrayList<>();

        for (String arg : inputArgs) {
            if (arg.startsWith("-")) {
                dashArgs.add(arg.substring(1).toLowerCase());
            } else {
                args.add(arg);
            }
        }

        this.length = args.size();
    }
    public boolean isEmpty() {
        return length == 0;
    }

    public boolean isNotEmpty() {
        return length > 0;
    }

    public int length() {
        return length;
    }

    public boolean hasNext() {
        return index < length;
    }

    public boolean hasDashArg(String arg) {
        return dashArgs.contains(arg.toLowerCase());
    }

    public String next() {
        if(index < length) {
            return args.get(index++);
        }
        return null;
    }

    public String peek() {
        if(index < length) {
            return args.get(index);
        }
        return null;
    }

    public String last() {
        if(length > 0) {
            return args.get(length - 1);
        }
        return null;
    }

    public List<String> remaining() {
        return args.subList(index, args.size());
    }

    public List<String> all() {
        return args;
    }

    public String shift() {
        return next();
    }

    public <T> T shift(String pattern) {
        return shift(pattern, null);
    }

    public <T> T shift(String pattern, T value) {
        String currentArg = next();

        // Check if the current peek exists
        if (currentArg == null) {
            return value; // No more elements to peek
        }

        // Check if the pattern is in the format of "{x}"
        if (pattern.matches("\\{.+?}")) {
            SMTabCompletion<?> tabCompletion = STEMCraft.getTabCompletion(pattern.substring(1, pattern.length() - 1));
            if (tabCompletion != null) {
                //noinspection unchecked
                return (T) tabCompletion.resolve(currentArg);
            }
        } else if (pattern.contains("|")) {
            String[] options = pattern.split("\\|");
            for (String option : options) {
                if (currentArg.equalsIgnoreCase(option.trim())) {
                    //noinspection unchecked
                    return (T) currentArg;
                }
            }

            return null;
        }

        return value;
    }

    public <T> SMCommandArgResult<T> shiftIfConsoleOrExists(String pattern, T defaultValue) {
        return shiftIfExists(true, pattern, defaultValue);
    }

    public <T> SMCommandArgResult<T> shiftIfExists(boolean shiftIfConsole, String pattern, T defaultValue) {
        String currentArg = peek();
        if (currentArg == null) {
            return new SMCommandArgResult<>(defaultValue, "", false);
        }

        if (pattern.matches("\\{.+?}")) {
            SMTabCompletion<?> tabCompletion = STEMCraft.getTabCompletion(pattern.substring(1, pattern.length() - 1));
            if (tabCompletion != null) {
                index++;
                //noinspection unchecked
                return new SMCommandArgResult<>((T) tabCompletion.resolve(currentArg), currentArg, true);
            }
        } else if (pattern.contains("|")) {
            String[] options = pattern.split("\\|");
            for (String option : options) {
                if (currentArg.equalsIgnoreCase(option.trim())) {
                    index++;
                    //noinspection unchecked
                    return new SMCommandArgResult<>((T)currentArg, currentArg, true);
                }
            }
        }

        if(fromConsole && shiftIfConsole) {
            index++;
        }

        return new SMCommandArgResult<>(defaultValue, currentArg, false);
    }

    public <T> SMCommandArgResult<T> shiftIfExists(String pattern, T defaultValue) {
        return shiftIfExists(false, pattern, defaultValue);
    }

    public <T> SMCommandArgResult<T> shiftIfExists(String pattern) {
        return shiftIfExists(false, pattern, null);
    }

    public <T> T shiftOption(String key, String pattern, T defaultValue) {
        for (int i = 0; i < length; i++) {
            if (args.get(i).startsWith(key + ":")) {
                String value = args.get(i).substring(key.length() + 1);
                if (value.matches(pattern)) {
                    index++;
                    //noinspection unchecked
                    return (T) value;
                }
            }
        }
        return defaultValue;
    }

    public <T> T shiftOption(String key, String pattern) {
        return shiftOption(key, pattern, null);
    }

    public boolean wantsHelp() {
        String arg = peek();
        return arg != null && (arg.equalsIgnoreCase("help") || arg.equalsIgnoreCase("?"));
    }

    public <T> SMCommandArgResult<T> popIfExists(boolean popIfConsole, String pattern, T defaultValue) {
        String currentArg = last();
        if (currentArg == null) {
            return new SMCommandArgResult<>(defaultValue, "", false);
        }

        if (pattern.matches("\\{.+?}")) {
            SMTabCompletion<?> tabCompletion = STEMCraft.getTabCompletion(pattern.substring(1, pattern.length() - 1));
            if (tabCompletion != null) {
                length--;
                //noinspection unchecked
                return new SMCommandArgResult<>((T) tabCompletion.resolve(currentArg), currentArg, true);
            }
        } else if (pattern.contains("|")) {
            String[] options = pattern.split("\\|");
            for (String option : options) {
                if (currentArg.equalsIgnoreCase(option.trim())) {
                    length--;
                    //noinspection unchecked
                    return new SMCommandArgResult<>((T)currentArg, currentArg, true);
                }
            }
        }

        if(fromConsole && popIfConsole) {
            length--;
        }

        return new SMCommandArgResult<>(defaultValue, currentArg, false);
    }

    public <T> SMCommandArgResult<T> popIfExists(String pattern, T defaultValue) {
        return popIfExists(false, pattern, defaultValue);
    }

    public <T> SMCommandArgResult<T> popIfExists(String pattern) {
        return popIfExists(false, pattern, null);
    }
}
