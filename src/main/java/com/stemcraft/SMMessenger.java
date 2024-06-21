package com.stemcraft;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.stemcraft.utils.SMUtilsString;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public class SMMessenger {
    public enum MessageType {
        INFO,
        SUCCESS,
        ERROR,
        WARNING,
        ANNOUNCEMENT
    }

    private static final Map<String, String> prefixMap = new HashMap<>();

    public static String getPrefix(MessageType type) {
        return prefixMap.getOrDefault(type.name(), "");
    }

    public static void setPrefix(MessageType type, String prefix) {
        prefixMap.put(type.name(), prefix);
    }

    private static String replaceLocaleHolders(String string) {
        Pattern pattern = Pattern.compile("\\{:(.+?)\\}");
        Matcher matcher = pattern.matcher(string);

        StringBuilder result = new StringBuilder();

        int lastEnd = 0;
        while (matcher.find()) {
            result.append(string, lastEnd, matcher.start());
            String placeholder = matcher.group(1);
            String replacement = STEMCraft.locale(placeholder);
            result.append(replacement);
            lastEnd = matcher.end();
        }

        result.append(string.substring(lastEnd));
        return result.toString();
    }

    /**
     * Send information message to sender.
     *
     * @param sender the command sender
     * @param message the message
     */
    public static void send(MessageType type, final CommandSender sender, final String message) {
        tell(sender, getPrefix(type), replaceLocaleHolders(message));
    }

    /**
     * Send information message to sender.
     *
     * @param sender the command sender
     * @param messages the messages
     */
    public static void send(MessageType type, final CommandSender sender, final List<String> messages) {
        messages.replaceAll(SMMessenger::replaceLocaleHolders);
        tell(sender, getPrefix(type), messages);
    }

    /**
     * Send information message to sender.
     *
     * @param sender the command sender
     * @param message the message
     * @param replacements the replacements
     */
    public static void send(MessageType type, final CommandSender sender, String message, String... replacements) {
        message = replaceLocaleHolders(message);
        message = SMUtilsString.replaceVariables(message, replacements);
        tell(sender, getPrefix(type), message);
    }

    /**
     * Send a blank line to the sender.
     *
     * @param player the command sender
     */
    public static void blankLine(final CommandSender player) {
        player.sendMessage("   ");
    }

    /*
     * Internal function to send message to sender.
     */
    private static void tell(final CommandSender player, final String prefix, String message) {
        String coloredPrefix = SMUtilsString.colorize(prefix);
        final String colorless = SMUtilsString.stripColors(message);

        // Remove prefix for console
        if (!(player instanceof Player)) {
            coloredPrefix = coloredPrefix.replaceAll(".*(?=(§[0-9a-fr])).*", "$1");
        }

        player.sendMessage(coloredPrefix + colorless);
    }

    /*
     * Internal function to send message to sender.
     */
    private static void tell(final CommandSender player, final String prefix, List<String> messages) {
        String coloredPrefix = SMUtilsString.colorize(prefix);
        final String transformedPrefix =
                player instanceof Player ? coloredPrefix : coloredPrefix.replaceAll(".*(?=(§[0-9a-fr])).*", "$1");

        messages.forEach(message -> {
            final String colorless = SMUtilsString.stripColors(message);
            player.sendMessage(transformedPrefix + colorless);
        });
    }

    public static void seperatorLine(final CommandSender player) {
        seperatorLine(player, null);
    }

    public static void seperatorLine(final CommandSender player, ChatColor color) {
        player.sendMessage(color == null ? "" : color + (ChatColor.STRIKETHROUGH + StringUtils.repeat(" ", 80)));
    }
}
