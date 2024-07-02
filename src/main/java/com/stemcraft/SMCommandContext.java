package com.stemcraft;

import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SMCommandContext {
    public SMCommand command;
    public String alias;
    public CommandSender sender;
    public Player player;
    public SMCommandArgs args;
    public List<String> dashArgs;
    public Map<String, String> optionArgs;

    public SMCommandContext(SMCommand command, CommandSender sender, String alias, String[] args) {
        this.command = command;
        this.sender = sender;
        this.alias = alias.toLowerCase();

        this.player = sender instanceof Player ? (Player) sender : null;

//        dashArgs = new ArrayList<>();
//        optionArgs = new HashMap<>();
        this.args = new SMCommandArgs(fromConsole(), args); // Convert args array to list

//        Iterator<String> iterator = this.args.iterator();
//        while (iterator.hasNext()) {
//            String arg = iterator.next();
//            if (arg.startsWith("-")) {
//                dashArgs.add(arg);
//                iterator.remove();
//            } else if (arg.matches("^[a-zA-Z0-9-_]:.*")) {
//                int index = arg.indexOf(":");
//                String option = arg.substring(0, index);
//                String value = arg.substring(index + 1);
//
//                optionArgs.putIfAbsent(option.toLowerCase(), value);
//                iterator.remove();
//            }
//        }
    }

    /**
     * Return if the sender has the specified permission.
     * 
     * @param permission The permission to check.
     * @return True if the sender has the permission, otherwise false.
     */
    public Boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }

    /**
     * Return if command has been run from console.
     * @return True if the sender is console, otherwise false.
     */
    public Boolean fromConsole() {
        return player == null;
    }

    /**
     * Display the command usage string.
     */
    public void usage() {
        this.info("Usage: " + this.command.usage());
    }


    /**
     * Throw an exception on invalid arguments.
     */
    public final void invalidArgs() {
        SMMessenger.send(SMMessenger.MessageType.ERROR, sender, "{:CMD_INVALID_ARGS}");
    }

    /**
     * Display an info message
     * @param message The message
     */
    public final void info(String message) {
        SMMessenger.send(SMMessenger.MessageType.INFO, sender, message);
    }

    public final void info(String message, String... replacements) {
        SMMessenger.send(SMMessenger.MessageType.INFO, sender, message, replacements);
    }

    /**
     * Display a success message.
     * @param message The message
     */
    public final void success(String message) {
        SMMessenger.send(SMMessenger.MessageType.SUCCESS, sender, message);
    }

    public final void success(String message, String... replacements) {
        SMMessenger.send(SMMessenger.MessageType.SUCCESS, sender, message, replacements);
    }

    /**
     * Display a warning message.
     * @param message The message.
     */
    public final void warning(String message) {
        SMMessenger.send(SMMessenger.MessageType.WARNING, sender, message);
    }

    public final void warning(String message, String... replacements) {
        SMMessenger.send(SMMessenger.MessageType.WARNING, sender, message, replacements);
    }

    /**
     * Return with an error message.
     * @param message the message
     */
    public final void error(String message) {
        SMMessenger.send(SMMessenger.MessageType.ERROR, sender, message);
    }

    public final void error(String message, String... replacements) {
        SMMessenger.send(SMMessenger.MessageType.ERROR, sender, message, replacements);
    }

    public final void errorNoPermission() {
        error("You do not have permission to use this command");
    }

    /**
     * Resolve a sender name as either console or player
     * @return The sender name
     */
    public String senderName() {
        return sender instanceof Player ? sender.getName() : STEMCraft.locale("CONSOLE_NAME");
    }

    public String[] getSubCommandList() {
        return command.getSubCommandList();
    }
}
