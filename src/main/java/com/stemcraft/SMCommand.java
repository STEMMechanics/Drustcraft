package com.stemcraft;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Consumer;

import lombok.Setter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class SMCommand implements TabCompleter {
    /**
     * Command label.
     */
    @Getter
    private String label;

    /**
     * The command description
     */
    private String description = "";

    /**
     * The list of command aliases.
     */
    private final List<String> aliases = new ArrayList<>();

    /**
     * The permission required for this command.
     */
    private String permission = "";

    /**
     * Command tab completion data
     */
    private final List<String[]> tabCompletionList = new ArrayList<>();

    /**
     * The custom execution handler
     */
    @Setter
    private Consumer<SMCommandContext> executionHandler;

    /**
     * Constructor
     * 
     * @param label The command label
     */
    public SMCommand(String label) {
        this.label = label;
    }

    /**
     * Set the command description.
     * @param description The command description
     */
    public void description(String description) {
        this.description = description;
    }

    /**
     * Add aliases to the command.
     * @param aliases The command aliases
     */
    public void alias(String... aliases) {
        this.aliases.addAll(Arrays.asList(aliases));
    }

    /**
     * Set the permission required for the command.
     * @param permission The permission required for the command
     */
    public void permission(String permission) {
        this.permission = permission;
    }

    /**
     * Add tab completion data to the command.
     * @param completion The tab completion data
     */
    public void tabCompletion(String... completion) {
        this.tabCompletionList.add(completion);
    }

    /**
     * Register the command on the server
     */
    public void register() {
        CommandMap commandMap = STEMCraft.getCommandMap();
        if (commandMap != null) {
            PluginCommand pluginCommand = null;

            try {
                Constructor<PluginCommand> c = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
                c.setAccessible(true);

                pluginCommand = c.newInstance(label, STEMCraft.getPlugin());
            } catch (Exception e) {
                STEMCraft.error(e);
            }

            if (pluginCommand != null) {
                pluginCommand.setTabCompleter(this);

                if (!this.aliases.isEmpty()) {
                    pluginCommand.setAliases(aliases);
                }

                pluginCommand.setExecutor((sender, command, label, args) -> {
                    SMCommandContext context = new SMCommandContext(this, sender, label, args);

                    if (!Objects.equals(permission, "") && !sender.hasPermission(permission)) {
                        context.errorNoPermission();
                    } else {
                        this.execute(context);
                    }

                    return true;
                });

                if (!aliases.isEmpty()) {
                    pluginCommand.setAliases(aliases);
                }

                if(!description.isEmpty()) {
                    pluginCommand.setDescription(description);
                } else {
                    pluginCommand.setDescription("stemcraft command: " + label);
                }

                commandMap.register(label, "stemcraft", pluginCommand);
            }
        }
    }

    /**
     * Return the usage string of the command
     * @return The usage string
     */
    public String usage() {
        return "/" + label;
    }

    /**
     * Execute the command
     * @param ctx The command context
     */
    public void execute(SMCommandContext ctx) {
        if (executionHandler != null) {
            executionHandler.accept(ctx);
        } else {
            ctx.error("Command not implemented");
        }
    }

    private static class TabCompleteValueOption {
        String option;
        String value;

        TabCompleteValueOption(String option, String value) {
            this.option = option;
            this.value = value;
        }
    }

    private static class TabCompleteArgParser {
        List<String> optionArgsAvailable = new ArrayList<>();
        Map<String, List<String>> valueOptionArgsAvailable = new HashMap<>();
        List<String> optionArgsUsed = new ArrayList<>();
        List<String> valueOptionArgsUsed = new ArrayList<>();
        Integer argIndex = 0;
        String[] args;

        public TabCompleteArgParser(String[] args) {
            this.args = args;
        }

        public static String getStringAsOption(String arg) {
            if (arg.startsWith("-")) {
                return arg.toLowerCase();
            }

            return null;
        }

        public void addOption(String option) {
            optionArgsAvailable.add(option);
        }

        public static TabCompleteValueOption getStringAsValueOption(String arg) {
            if (arg.matches("^[a-zA-Z0-9-_]:.*")) {
                String option = arg.substring(0, arg.indexOf(':')).toLowerCase();
                String value = arg.substring(arg.indexOf(':') + 1);

                return new TabCompleteValueOption(option, value);
            }

            return null;
        }

        public void addValueOption(TabCompleteValueOption option) {
            valueOptionArgsAvailable.put(option.option, parseValue(option.value));
        }

        public static List<String> parseValue(String value) {
            List<String> list = new ArrayList<>();

            if (value.startsWith("{") && value.endsWith("}")) {
                String placeholder = value.substring(1, value.length() - 1);
                SMTabCompletion<?> smTabCompletion = STEMCraft.getTabCompletion(placeholder);
                if(smTabCompletion != null) {
                    list.addAll(smTabCompletion.completions(""));
                }
            } else {
                list.add(value);
            }

            return list;
        }


        public Boolean hasRemainingArgs() {
            return argIndex < args.length - 1;
        }

        public void next() {
            nextMatches(null);
        }

        public Boolean nextMatches(String tabCompletionItem) {
            for (; argIndex < args.length; argIndex++) {
                String arg = args[argIndex];

                String option = getStringAsOption(arg);
                if (option != null) {
                    optionArgsUsed.add(option);
                    optionArgsAvailable.remove(option);
                    continue;
                }

                TabCompleteValueOption valueOption = getStringAsValueOption(arg);
                if (valueOption != null) {
                    valueOptionArgsUsed.add(valueOption.option);
                    valueOptionArgsAvailable.remove(valueOption.option);
                    continue;
                }

                if (tabCompletionItem == null) {
                    argIndex++;
                    return true;
                }

                List<String> values = parseValue(tabCompletionItem);
                if (values.contains(arg)) {
                    argIndex++;
                    return true;
                }

                return false;
            }

            // To get here we are out of args to parse
            return null;
        }

        public void processRemainingArgs() {
            while (hasRemainingArgs()) {
                next();
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        List<String> tabCompletionResults = new ArrayList<>();
        List<String> optionArgsAvailable = new ArrayList<>();
        Map<String, List<String>> valueOptionArgsAvailable = new HashMap<>();
        String[] fullArgs = new String[args.length - 1];

        System.arraycopy(args, 0, fullArgs, 0, args.length - 1);

        // iterate each tab completion list
        tabCompletionList.forEach(list -> {
            boolean matches = true;
            int listIndex;

            // Copy the elements except the last one
            TabCompleteArgParser argParser = new TabCompleteArgParser(fullArgs);

            // iterate each tab completion list item
            for (listIndex = 0; listIndex < list.length; listIndex++) {
                String listItem = list[listIndex];

                // list item is an option
                String option = TabCompleteArgParser.getStringAsOption(listItem);
                if (option != null) {
                    argParser.addOption(option);
                    continue;
                }

                // list item is a value option
                TabCompleteValueOption valueOption = TabCompleteArgParser.getStringAsValueOption(listItem);
                if (valueOption != null) {
                    argParser.addValueOption(valueOption);
                    continue;
                }

                // list item is a string or placeholder
                Boolean nextMatches = argParser.nextMatches(listItem);
                if (nextMatches == null) {
                    tabCompletionResults.addAll(TabCompleteArgParser.parseValue(listItem));
                    break;
                } else if (!nextMatches) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                // parse remaining arg items
                argParser.processRemainingArgs();

                optionArgsAvailable.addAll(argParser.optionArgsAvailable);
                valueOptionArgsAvailable.putAll(argParser.valueOptionArgsAvailable);
            }
        });

        // remove non-matching items from the results based on what the player has already entered
        if (!args[args.length - 1].isEmpty()) {
            String arg = args[args.length - 1];

            // if the player has only a dash in the arg, only show dash arguments
            if (arg.equals("-")) {
                return optionArgsAvailable;
            }

            // if the player has written the start of a option arg
            if (arg.contains(":")) {
                // if the option arg is available
                String key = arg.substring(0, arg.indexOf(":"));
                if (valueOptionArgsAvailable.containsKey(key)) {
                    tabCompletionResults.clear();
                    String prefix = key + ":";
                    for (String item : valueOptionArgsAvailable.get(key)) {
                        tabCompletionResults.add(prefix + item);
                    }
                }
            }

            // remove items in tabCompletionResults that do not contain the current arg text
            tabCompletionResults.removeIf(item -> !item.contains(arg));
        }

        return tabCompletionResults;
    }

    public String[] getSubCommandList() {
        return this.tabCompletionList.stream()
                .filter(list -> list.length > 0 && !list[0].startsWith("{") && !list[0].endsWith("}"))
                .map(list -> list[0])
                .toArray(String[]::new);
    }
}
