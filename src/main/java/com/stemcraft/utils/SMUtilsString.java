package com.stemcraft.utils;

import lombok.NonNull;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.bukkit.ChatColor.COLOR_CHAR;

public class SMUtilsString {

    /** Regex for finding color codes */
    private static final Pattern COLOR_PATTERN = Pattern.compile("((&|" + COLOR_CHAR + ")[0-9a-fk-or])|(" + COLOR_CHAR
            + "x(" + COLOR_CHAR + "[0-9a-fA-F]){6})|((?<!\\\\)(\\{|&|)#((?:[0-9a-fA-F]{3}){2})(}|))");

    /** Pattern matching "nicer" legacy hex chat color codes - &#rrggbb */
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");

    /**
     * Colourise a string.
     *
     * @param message The message to colourise.
     * @return The colourised message.
     */
    public static String colorize(String message) {
        // Convert from the '&#rrggbb' hex color format to the '&x&r&r&g&g&b&b' one used by Bukkit.
        Matcher matcher = HEX_COLOR_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            StringBuilder replacement = new StringBuilder(14).append("&x");
            for (char character : matcher.group(1).toCharArray()) {
                replacement.append('&').append(character);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);

        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    /**
     * Colourise a string array.
     *
     * @param messages The messages to colourise.
     * @return The colourised messages.
     */
    public static String[] colorizeAll(String... messages) {
        String[] colorizedMessages = new String[messages.length];
        for (int i = 0; i < messages.length; i++) {
            colorizedMessages[i] = colorize(messages[i]);
        }

        return colorizedMessages;
    }

    /**
     * Colourise a string array.
     *
     * @param messages The messages to colourise.
     * @return The colourised messages.
     */
    public static List<String> colorizeAll(List<String> messages) {
        List<String> colorizedMessages = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            colorizedMessages.add(colorize(messages.get(i)));
        }

        return colorizedMessages;
    }

    /**
     * Strip color codes from string.
     *
     * @param message
     * @return
     */
    public static String stripColors(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        final Matcher matcher = COLOR_PATTERN.matcher(message);

        while (matcher.find()) {
            message = matcher.replaceAll("");
        }

        return message;
    }


    /**
     * Returns the first non-null and non-empty value from the provided values.
     * If no such value is found, returns an empty string.
     *
     * @param values the values to be checked.
     * @param <T> the type of the values.
     * @return the first non-null and non-empty value, or an empty string if no such value is found.
     */
    public static <T> T getOrDefault(final T... values) {
        for (T value : values) {
            if (value != null) {
                if (value instanceof String && !((String) value).isEmpty()) {
                    return value;
                } else if (!(value instanceof String)) {
                    return value;
                }
            }
        }

        return (T) "";
    }

    /**
     * Replace variables in the specified string.
     * @param message The message to replace variables in.
     * @param replacements The key-value pairs to replace.
     * @return The message with the variables replaced.
     */
    public static String replaceVariables(String message, String... replacements) {
        if (replacements.length % 2 != 0) {
            throw new IllegalArgumentException("Odd number of replacements provided. Expecting key-value pairs.");
        }

        Map<String, String> replacementMap = new HashMap<>();
        for (int i = 0; i < replacements.length; i += 2) {
            String key = replacements[i];
            String value = (replacements[i + 1] != null) ? replacements[i + 1] : null;
            replacementMap.put(key, value);
        }

        return replaceVariables(message, replacementMap);
    }

    /**
     * Replace variables in the specified string.
     * @param message The message to replace variables in.
     * @param replacements The key-value pairs to replace.
     * @return The message with the variables replaced.
     */
    public static String replaceVariables(String message, Map<String, String> replacements) {
        StringBuilder result = new StringBuilder(message);
        replacements.forEach((key, val) -> {
            if (val != null) {
                String pattern = "{" + key + "}";
                int index;
                while ((index = result.indexOf(pattern)) != -1) {
                    result.replace(index, index + pattern.length(), val);
                }
            }
        });

        return result.toString();
    }

    public static String beforeFirst(String str, String character) {
        int index = str.indexOf(character);
        if (index != -1) {
            return str.substring(0, index);
        }

        return str;
    }

    public static String afterFirst(String str, String character) {
        int index = str.indexOf(character);
        if (index != -1) {
            return str.substring(index + 1);
        }

        return "";
    }

    public static String capitalize(String str) {
        return capitalize(str, false);
    }

    public static String capitalize(String str, Boolean ignoreColors) {
        if (str != null && str.length() != 0) {
            final int strLen = str.length();
            final StringBuffer buffer = new StringBuffer(strLen);
            boolean capitalizeNext = true;

            for (int i = 0; i < strLen; ++i) {
                final char ch = str.charAt(i);

                if (Character.isWhitespace(ch)) {
                    buffer.append(ch);

                    capitalizeNext = true;
                } else if (ch == '&' && ignoreColors && i + 1 < strLen
                        && "0123456789abcdefklmnor".indexOf(str.charAt(i + 1)) != -1) {
                    buffer.append(ch).append(str.charAt(i + 1));
                    i++;

                } else if (capitalizeNext) {
                    buffer.append(Character.toTitleCase(ch));

                    capitalizeNext = false;
                } else {
                    buffer.append(ch);
                }
            }

            return buffer.toString();
        }

        return str;
    }

    public static String beautify(String str) {
        return str.toLowerCase().replace("_", " ");
    }

    public static String beautifyCapitalize(String str) {
        return capitalize(beautify(str));
    }

    public static String beautifyCapitalize(@NonNull Enum<?> enumeration) {
        return beautifyCapitalize(enumeration.toString().toLowerCase());
    }

    public static boolean isInteger(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
