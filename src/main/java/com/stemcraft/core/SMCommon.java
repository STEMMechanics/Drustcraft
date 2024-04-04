package com.stemcraft.core;

import org.atteo.evo.inflector.English;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import com.stemcraft.STEMCraft;
import com.stemcraft.core.config.SMConfig;
import lombok.NonNull;
import static org.bukkit.ChatColor.COLOR_CHAR;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SMCommon {

    /*
     * Regex for finding color codes
     */
    private static final Pattern COLOR_PATTERN = Pattern.compile("((&|" + COLOR_CHAR + ")[0-9a-fk-or])|(" + COLOR_CHAR
        + "x(" + COLOR_CHAR + "[0-9a-fA-F]){6})|((?<!\\\\)(\\{|&|)#((?:[0-9a-fA-F]{3}){2})(}|))");

    /** Pattern matching "nicer" legacy hex chat color codes - &#rrggbb */
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");

    private static DecimalFormat DECIMAL_FORMAT = null;
    private static DecimalFormat COMMA_FORMAT = null;
    private static SimpleDateFormat DATE_FORMAT = null;

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
     * Colourize a string.
     * 
     * @param message
     * @return
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
     * Colourize a string array.
     * 
     * @param messages
     * @return
     */
    public static String[] colorizeAll(String... messages) {
        String[] colorizedMessages = new String[messages.length];
        for (int i = 0; i < messages.length; i++) {
            colorizedMessages[i] = colorize(messages[i]);
        }

        return colorizedMessages;
    }

    /**
     * Colourize a string array.
     * 
     * @param messages
     * @return
     */
    public static List<String> colorizeAll(List<String> messages) {
        List<String> colorizedMessages = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            colorizedMessages.add(colorize(messages.get(i)));
        }

        return colorizedMessages;
    }

    /**
     * Retrieve a player by name or UUID.
     * 
     * @param nameOrUUID
     * @return
     */
    public static Player findPlayer(String nameOrUUID) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getUniqueId().toString().equalsIgnoreCase(nameOrUUID)
                || player.getDisplayName().equalsIgnoreCase(nameOrUUID)) {
                return player;
            }
        }
        return null;
    }

    /**
     * Is the specified location safe for a player to spawn
     * 
     * @param location
     * @return
     */
    public static boolean isSafeLocation(Location location) {
        Block block = location.getBlock();
        Block aboveBlock = block.getRelative(BlockFace.UP);
        Block belowBlock = block.getRelative(BlockFace.DOWN);

        final Material[] includeMaterials = {
                Material.AIR, Material.GRASS_BLOCK
        };

        // Check if the block and the block above are air blocks
        if (isInArray(includeMaterials, block.getType()) && aboveBlock.getType() == Material.AIR
            && belowBlock.getType().isSolid()) {
            String materialString = belowBlock.getType().toString();
            if (!materialString.endsWith("_BED") && !materialString.endsWith("_SIGN")
                && !materialString.contains("FENCE")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Find the first safe location within the range from the specified location.
     * 
     * @param location
     * @param range
     * @return
     */
    public static Location findSafeLocation(Location location, int range) {
        return findSafeLocation(location, range, false);
    }

    /**
     * Find the first or random safe location within the range from the specified location.
     * 
     * @param location
     * @param rangeMax
     * @param random
     * @return
     */
    public static Location findSafeLocation(Location location, int rangeMax, boolean random) {
        return findSafeLocation(location, 0, rangeMax, random);
    }

    /**
     * Find the first or random safe location within the range from the specified location.
     * 
     * @param location
     * @param rangeMin
     * @param rangeMax
     * @param random
     * @return
     */
    public static Location findSafeLocation(Location location, int rangeMin, int rangeMax, boolean random) {
        Location closestLocation = null;
        double minDistance = Double.MAX_VALUE;
        List<Location> safeLocations = new ArrayList<>();

        for (int x = location.getBlockX() - rangeMax; x <= location.getBlockX() + rangeMax; x++) {
            for (int y = location.getBlockY() - rangeMax; y <= location.getBlockY() + rangeMax; y++) {
                for (int z = location.getBlockZ() - rangeMax; z <= location.getBlockZ() + rangeMax; z++) {
                    Location candidateLocation = new Location(location.getWorld(), x + 0.5, y, z + 0.5);

                    // Skip locations outside the specified range
                    if (Math.abs(location.getBlockX() - x) > rangeMax ||
                        Math.abs(location.getBlockY() - y) > rangeMax ||
                        Math.abs(location.getBlockZ() - z) > rangeMax) {
                        continue;
                    }

                    if (isSafeLocation(candidateLocation)) {
                        if (random) {
                            safeLocations.add(candidateLocation);
                        } else {
                            double distance = location.distance(candidateLocation);

                            // Update closest location if the new candidate is closer
                            if (closestLocation == null || distance < minDistance
                                || (distance == minDistance && Math.abs(location.getBlockY() - y) < Math
                                    .abs(location.getBlockY() - closestLocation.getBlockY()))) {
                                closestLocation = candidateLocation;
                                minDistance = distance;
                            }
                        }
                    }
                }
            }
        }

        if (random && !safeLocations.isEmpty()) {
            Random randomLoc = new Random();
            int randomIndex = randomLoc.nextInt(safeLocations.size());
            return safeLocations.get(randomIndex);
        }

        return closestLocation;
    }

    /**
     * Equivalent to {@link String#replace(CharSequence, CharSequence)}, but uses a {@link Supplier} for the
     * replacement.
     *
     * @param pattern the pattern for the replacement target
     * @param input the input string
     * @param replacement the replacement
     * @return the input string with the replacements applied
     */
    public static String replaceAll(Pattern pattern, String input, Supplier<String> replacement) {
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.replaceAll(Matcher.quoteReplacement(replacement.get()));
        }
        return input;
    }

    /**
     * Add items to a List<String>
     * 
     * @param list
     * @param messages
     */
    public static void append(List<String> list, String... messages) {
        list.addAll(Arrays.asList(messages));
    }

    /**
     * Returns the value if it is not {@code null}. Otherwise, the default value is returned. If the value is a
     * {@link String}, the default value is returned if the value is empty.
     *
     * @param value the value to be checked.
     * @param defaultValue the default value to be returned if the actual value is {@code null}.
     * @param <T> the type of the value and default value.
     * @return the value if it is not {@code null}, otherwise the default value.
     */
    public static <T> T getOrDefault(final T value, final T defaultValue) {
        if (value instanceof String && "".equals(value)) {
            return defaultValue;
        }

        return value != null ? value : defaultValue;
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

    public static HashMap<String, Object> mapOfArray(final Object... array) {
        HashMap<String, Object> map = new HashMap<>();

        if (array != null) {
            for (int i = 0; i < array.length; i += 2) {
                String key = array[i].toString();
                Object value = null;

                if (i + 1 < array.length) {
                    value = array[i + 1];
                }

                map.put(key, value);
            }
        }

        return map;
    }

    public static Boolean itemIsRepairable(ItemStack item) {
        ItemMeta itemMeta = item.getItemMeta();
        return (itemMeta instanceof Damageable);
    }

    public static ItemStack repairItem(ItemStack item) {
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta instanceof Damageable) {
            Damageable damageable = (Damageable) itemMeta;
            damageable.setDamage(0);
            item.setItemMeta(itemMeta);
        }

        return item;
    }

    /**
     * Teleport a player after 1 tick. This avoids the moved too quickly issue
     * 
     * @param player
     * @param location
     */
    public static void delayedPlayerTeleport(Player player, Location location) {
        STEMCraft.runLater(1, () -> {
            player.teleport(location);
        });
    }

    /**
     * Teleport a player to the nearest safe location
     * 
     * @param player
     * @param location
     */
    public static Boolean safePlayerTeleport(Player player, Location location) {
        Location safeLocation = findSafeLocation(location, 30);
        if (safeLocation != null) {
            STEMCraft.runLater(1, () -> {
                player.teleport(safeLocation);
            });

            return true;
        }

        return false;
    }

    public static String getKeyByValue(Map<String, String> map, String value) {
        return getKeyByValue(map, value, null);
    }

    public static String getKeyByValue(Map<String, String> map, String value, String defaultValue) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return defaultValue;
    }

    public static String format(String str) {
        return colorize(str);
    }

    /**
     * Joins multiple arrays into one array.
     *
     * @param arrays the arrays to be joined.
     * @param <T> the type of elements in the arrays.
     * @return an array containing all the elements from the input arrays.
     */
    @SafeVarargs
    public static <T> Object[] joinArrays(final T[]... arrays) {
        final List<T> all = new ArrayList<>();

        for (final T[] array : arrays)
            all.addAll(Arrays.asList(array));

        return all.toArray();
    }

    /**
     * Joins multiple {@link Iterable lists} into one {@link List}.
     *
     * @param lists the {@link Iterable lists} to be joined.
     * @param <T> the type of elements in the lists.
     * @return a {@link List} containing all the elements from the input {@link Iterable lists}.
     */
    @SafeVarargs
    public static <T> List<T> joinLists(final Iterable<T>... lists) {
        final List<T> all = new ArrayList<>();

        for (final Iterable<T> array : lists)
            for (final T element : array)
                all.add(element);

        return all;
    }

    /**
     * Joins elements from the input array, separated by ", ". We invoke {@link T#toString()} for each element given it
     * is not {@code null}, or return an empty {@link String} if it is.
     *
     * @param array the input array.
     * @param <T> the type of elements in the array.
     * @return a {@link String} containing the joined elements of the array.
     */
    public static <T> String join(final T[] array) {
        return array == null ? "null" : join(Arrays.asList(array));
    }

    /**
     * Joins elements from the input {@link Iterable array}, separated by ", ". We invoke {@link T#toString()} for each
     * element given it is not {@code null}, or return an empty {@link String} if it is.
     *
     * @param array the input {@link Iterable array}.
     * @param <T> the type of elements in the array.
     * @return a {@link String} containing the joined elements of the iterable.
     */
    public static <T> String join(final Iterable<T> array) {
        return array == null ? "null" : join(array, ", ");
    }

    /**
     * Joins elements from the input array, separated by the specified delimiter. We invoke {@link T#toString()} for
     * each element given it is not null, or return an empty {@link String} if it is.
     *
     * @param array the input array.
     * @param delimiter the delimiter used to separate the joined elements.
     * @param <T> the type of elements in the array.
     * @return a {@link String} containing the joined elements of the array with the specified delimiter.
     */
    public static <T> String join(final T[] array, final String delimiter) {
        return join(array, delimiter);
    }

    /**
     * Joins elements from the input {@link Iterable array}, separated by the specified delimiter. We invoke
     * {@link T#toString()} for each element given it is not null, or return an empty {@link String} if it is.
     *
     * @param array the input {@link Iterable array}.
     * @param delimiter the delimiter used to separate the joined elements.
     * @param <T> the type of elements in the array.
     * @return a {@link String} containing the joined elements of the {@link Iterable array} with the specified
     *         delimiter.
     */
    public static <T> String join(final Iterable<T> array, final String delimiter) {
        return join(array, delimiter);
    }

    /**
     * Is player holding a tool with silk touch?
     * 
     * @param Player the player.
     * @return if it is true.
     */
    public static Boolean playerHasSilkTouch(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand != null && itemInHand.getType() != Material.AIR) {
            if (itemInHand.getEnchantments().containsKey(Enchantment.SILK_TOUCH)) {
                return true;
            }
        }

        return false;
    }

    private static void initalizeFormatting() {
        if (COMMA_FORMAT == null || DECIMAL_FORMAT == null || DATE_FORMAT == null) {
            String defaultDateFormat = "dd/M/yyyy";
            String defaultDecimalSeparator = ".";
            String defaultCommaSeparator = ",";
            String defaultCommaFormat = "#,###";
            String defaultDecimalFormat = "#,###.00";

            String dateFormat = SMConfig.main().getString("date-format", defaultDateFormat);
            String decimalSeparator =
                SMConfig.main().getString("number-formats.decimal-separator", defaultDecimalSeparator);
            String commaSeparator = SMConfig.main().getString("number-formats.comma-separator", defaultCommaSeparator);
            String commaFormat = SMConfig.main().getString("number-formats.comma-format", defaultCommaFormat);
            String decimalFormat = SMConfig.main().getString("number-formats.decimal-format", defaultDecimalFormat);

            DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(Locale.getDefault());
            formatSymbols.setDecimalSeparator(decimalSeparator.charAt(0));
            formatSymbols.setGroupingSeparator(commaSeparator.charAt(0));

            try {
                DATE_FORMAT = new SimpleDateFormat(dateFormat, Locale.getDefault());
            } catch (NullPointerException | IllegalArgumentException exception) {
                STEMCraft.warning("date-format is NOT a valid format! Using default American English format.");
                exception.printStackTrace();
                DATE_FORMAT = new SimpleDateFormat(defaultDateFormat, Locale.ENGLISH);
            }

            try {
                COMMA_FORMAT = new DecimalFormat(commaFormat, formatSymbols);
            } catch (NullPointerException | IllegalArgumentException exception) {
                STEMCraft.warning("number-formats.comma-format is NOT a valid format! Using default #,### instead.");
                exception.printStackTrace();
                COMMA_FORMAT = new DecimalFormat(defaultCommaFormat, formatSymbols);
            }

            try {
                DECIMAL_FORMAT = new DecimalFormat(decimalFormat, formatSymbols);
            } catch (NullPointerException | IllegalArgumentException exception) {
                STEMCraft
                    .warning("number-formats.decimal-format is NOT a valid format! Using default #,###.00 instead.");
                exception.printStackTrace();
                DECIMAL_FORMAT = new DecimalFormat(defaultDecimalFormat, formatSymbols);
            }
        }
    }

    /**
     * Formats a number to make it pretty. Example: 4322 to 4,322
     *
     * @param number The number to format.
     * @return The formatted number.
     */
    public static String formatInt(int number) {
        initalizeFormatting();
        String finalNumber = COMMA_FORMAT.format(number);
        finalNumber = finalNumber.replaceAll("[\\x{202f}\\x{00A0}]", " ");
        return finalNumber;
    }

    /**
     * Formats a number to make it pretty. Example: 4322.33 to 4,322.33
     *
     * @param number The number to format.
     * @return The formatted number.
     */
    public static String formatDouble(double number) {
        initalizeFormatting();
        String finalNumber = DECIMAL_FORMAT.format(number);
        finalNumber = finalNumber.replaceAll("[\\x{202f}\\x{00A0}]", " ");
        return finalNumber;
    }

    /**
     * Formats a date into the readable format.
     *
     * @param date The date to format.
     * @return The date into a readable format.
     */
    public static String formatDate(Date date) {
        initalizeFormatting();
        return DATE_FORMAT.format(date);
    }

    /**
     * Show the player a full screen greeting
     * 
     * @param player
     * @param title
     * @param subtitle
     */
    public static void showGreeting(Player player, String title, String subtitle) {
        showGreeting(player, title, subtitle, 10, 60, 10);
    }

    /**
     * Show the player a full screen greeting
     * 
     * @param player
     * @param title
     * @param subtitle
     * @param fadeInTime
     * @param showTime
     * @param fadeOutTime
     */
    public static void showGreeting(Player player, String title, String subtitle, int fadeInTime, int showTime,
        int fadeOutTime) {
        player.sendTitle(SMCommon.colorize(title), SMCommon.colorize(subtitle), fadeInTime, showTime, fadeOutTime);
    }

    /**
     * Compute the (Greatest Common Divisor) GCD of two numbers.
     * 
     * @param a First number.
     * @param b Second number.
     * @return GCD of the two numbers.
     */
    public static float gcd(float a, float b) {
        if (b == 0)
            return a;
        return gcd(b, a % b);
    }

    /**
     * Compute the GCD of fractional parts of denominations.
     * 
     * @return GCD of the fractional parts.
     */
    public static float computeGCDOfFractions(HashMap<String, Float> items) {
        float result = 0;
        for (float value : items.values()) {
            float fractionalPart = value - (int) value;
            result = gcd(result, fractionalPart);
        }
        return result;
    }

    /**
     * Round the fractional component of a value.
     * 
     * @param value The value to be rounded.
     * @param gcd The GCD to use for rounding.
     * @return Rounded value.
     */
    public static float roundFractionalComponent(float value, float gcd) {
        int wholeNumber = (int) value;
        float fractionalComponent = value - wholeNumber;
        float roundedFraction = Math.round(fractionalComponent / gcd) * gcd;
        return wholeNumber + roundedFraction;
    }

    /**
     * Compare the 2 players to see if they are the same.
     * 
     * @param p1 First player to compare.
     * @param p2 Second player to compare.
     * @return If the 2 players are the same.
     */
    public static Boolean isSamePlayer(Player p1, Player p2) {
        if (p1 != null && p2 != null) {
            return p1.getUniqueId().equals(p2.getUniqueId());
        }

        return false;
    }

    /**
     * Convert a set object to a list object.
     * 
     * @param set The set object to convert.
     * @return The converted list object.
     */
    public static List<String> setToList(Set<?> set) {
        List<String> stringList = new ArrayList<>();
        for (Object obj : set) {
            stringList.add(String.valueOf(obj));
        }

        return stringList;
    }

    /**
     * Pluralize a string
     * 
     * @param string The string to pluralize.
     * @return The pluralized result.
     */
    public static String pluralize(String string) {
        return English.plural(string);
    }

    /**
     * Pluralize a string
     * 
     * @param string The string to pluralize.
     * @param count The number of items in the plural.
     * @return The pluralized result.
     */
    public static String pluralize(String string, int count) {
        return English.plural(string, count);
    }

    /**
     * Get the total amount of an item in an inventory.
     * 
     * @param inventory The inventory to search.
     * @param material The material name to count.
     * @return The total amount of material items in the inventory.
     */
    public static Integer totalItemType(Inventory inventory, String material) {
        Integer count = 0;
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack != null && SMBridge.getMaterialName(itemStack).equals(material)) {
                count += itemStack.getAmount();
            }
        }
        return count;
    }

    /**
     * Return the previous item in a String list.
     * 
     * @param list The list itself.
     * @param currentItem The item after the item to return.
     * @return The item before currentItem or null.
     */
    public static String getPrevListItem(List<String> list, String currentItem) {
        // Find the index of the current item
        int index = list.indexOf(currentItem);

        // Check if the current item is in the list and not at the end
        if (index >= 1 && index < list.size() - 1) {
            // Return the next item in the list
            return list.get(index - 1);
        }

        // Return null if the current item is at the end of the list or not found
        return null;
    }

    /**
     * Return the next item in a String list.
     * 
     * @param list The list itself.
     * @param currentItem The item before the item to return.
     * @return The item after currentItem or null.
     */
    public static String getNextListItem(List<String> list, String currentItem) {
        // Find the index of the current item
        int index = list.indexOf(currentItem);

        // Check if the current item is in the list and not at the end
        if (index >= 0 && index < list.size() - 1) {
            // Return the next item in the list
            return list.get(index + 1);
        }

        // Return null if the current item is at the end of the list or not found
        return null;
    }

    /**
     * Return a list of players within a distance from a location.
     * 
     * @param location The starting location.
     * @param distance The distance up to to check for players.
     * @return A player list.
     */
    public static List<Player> getPlayersNearLocation(Location location, int distance) {
        List<Player> nearbyPlayers = new ArrayList<>();
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (location.getWorld().getName().equals(player.getLocation().getWorld().getName())) {
                // Using distanceSquared for performance, as it avoids the sqrt operation
                if (location.distanceSquared(player.getLocation()) <= distance * distance) {
                    nearbyPlayers.add(player);
                }
            }
        }
        return nearbyPlayers;
    }

    /**
     * Converts a YAW value to a compass direction.
     * 
     * @param yaw The yaw value to convert.
     * @return The compass direction.
     */
    public static String getCompassDirection(float yaw) {
        double rotation = (yaw - 90) % 360;
        if (rotation < 0) {
            rotation += 360.0;
        }

        if (0 <= rotation && rotation < 22.5 || 337.5 <= rotation && rotation < 360) {
            return "W"; // West
        } else if (22.5 <= rotation && rotation < 67.5) {
            return "NW"; // Northwest
        } else if (67.5 <= rotation && rotation < 112.5) {
            return "N"; // North
        } else if (112.5 <= rotation && rotation < 157.5) {
            return "NE"; // Northeast
        } else if (157.5 <= rotation && rotation < 202.5) {
            return "E"; // East
        } else if (202.5 <= rotation && rotation < 247.5) {
            return "SE"; // Southeast
        } else if (247.5 <= rotation && rotation < 292.5) {
            return "S"; // South
        } else if (292.5 <= rotation && rotation < 337.5) {
            return "SW"; // Southwest
        }
        return ""; // This should never happen
    }

    /**
     * Convert a world time to a real time.
     * 
     * @param world The world to convert.
     * @return The converted time.
     */
    public static String convertWorldToRealTime(World world) {
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

    /**
     * Convert seconds to a relative time string.
     *
     * @param totalSeconds The total seconds to convert.
     * @return The relative time string.
     */
    public static String convertSecondsToRelative(long totalSeconds) {
        return convertSecondsToRelative(totalSeconds, false);
    }

    public static String convertSecondsToRelative(long totalSeconds, boolean shortFormat) {
        long days = totalSeconds / 86400;
        totalSeconds %= 86400;
        long hours = totalSeconds / 3600;
        totalSeconds %= 3600;
        long minutes = totalSeconds / 60;
        totalSeconds %= 60;
        long seconds = totalSeconds;

        StringBuilder relativeTime = new StringBuilder();
        if (days > 0) {
            if(shortFormat) {
                relativeTime.append(days).append("d ");
            } else {
                relativeTime.append(days).append(" day");
                if (days > 1)
                    relativeTime.append("s");
                relativeTime.append(", ");
            }
        }
        if (hours > 0) {
            if(shortFormat) {
                relativeTime.append(hours).append("h ");
            } else {
                relativeTime.append(hours).append(" hour");
                if (hours > 1)
                    relativeTime.append("s");
                relativeTime.append(", ");
            }
        }
        if (minutes > 0) {
            if(shortFormat) {
                relativeTime.append(minutes).append("m ");
            } else {
                relativeTime.append(minutes).append(" minute");
                if (minutes > 1)
                    relativeTime.append("s");
                relativeTime.append(", ");
            }
        }
        if(shortFormat) {
            relativeTime.append(seconds).append("s");
        } else {
            relativeTime.append(seconds).append(" second");
            if (seconds != 1)
                relativeTime.append("s");
        }

        return relativeTime.toString();
    }

    /**
     * Convert a duration string (1d) to seconds or epoch expiry time.
     * 
     * @param duration The duration string.
     * @param toEpochTime Return a epoch time instead of seconds.
     * @return The result or null if error.
     */
    public static Long durationToSeconds(String duration, boolean toEpochTime) {
        if (duration == null || duration.isEmpty()) {
            return null;
        }

        long multiplier;
        long seconds;
        char timeUnit = duration.charAt(duration.length() - 1);

        try {
            switch (timeUnit) {
                case 's': // Seconds
                    multiplier = 1;
                    seconds = Long.parseLong(duration.substring(0, duration.length() - 1));
                    break;
                case 'm': // Minutes
                    multiplier = 60;
                    seconds = Long.parseLong(duration.substring(0, duration.length() - 1));
                    break;
                case 'h': // Hours
                    multiplier = 3600;
                    seconds = Long.parseLong(duration.substring(0, duration.length() - 1));
                    break;
                case 'd': // Days
                    multiplier = 86400;
                    seconds = Long.parseLong(duration.substring(0, duration.length() - 1));
                    break;
                case 'w': // Weeks
                    multiplier = 604800;
                    seconds = Long.parseLong(duration.substring(0, duration.length() - 1));
                    break;
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return null; // Input is not a valid number
        }

        long totalSeconds = seconds * multiplier;

        return toEpochTime ? Instant.now().plus(totalSeconds, ChronoUnit.SECONDS).getEpochSecond() : totalSeconds;
    }

    /**
     * Convert a duration string (1d) to seconds or epoch expiry time.
     * 
     * @param player The player to give the item.
     * @param item The item to give.
     * @param dropOnFail Drop the item if the player inventory is full.
     * @param showMessage Show a message if giving the item failed and item was not dropped.
     * @return The result or null if error.
     */
    public static Boolean givePlayerItem(Player player, ItemStack item, Boolean dropOnFail, Boolean showMessage) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            // Handle the full inventory case here
            player.sendMessage("You don't have enough room in your inventory!");

            if (dropOnFail) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
                return true;
            } else if (showMessage) {
                SMMessenger.error(player, SMLocale.get("INV_NO_ROOM"));
            }

            return false;
        }

        return true;
    }

    public static Boolean givePlayerItem(Player player, ItemStack item) {
        return givePlayerItem(player, item, false, true);
    }

    /**
     * Is string in array, ignoring case.
     * 
     * @param array The array to check.
     * @param value The string to check.
     * @return
     */
    public static Boolean isInArrayIgnoreCase(String[] array, String value) {
        for (String element : array) {
            if (element.equalsIgnoreCase(value)) {
                return true;
            }
        }

        return false;
    }

    public static Boolean isInArrayIgnoreCase(List<String> array, String value) {
        for (String element : array) {
            if (element.equalsIgnoreCase(value)) {
                return true;
            }
        }

        return false;
    }

    public static <T> Boolean isInArray(T[] array, T value) {
        for (T element : array) {
            if (element.equals(value)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return the direction from yaw
     * 
     * @param yaw The yaw value
     * @param opposite Return the opposite direction
     * @return
     */
    public static BlockFace getClosestBlockFace(double yaw, boolean opposite) {
        if (yaw >= -45 && yaw <= 45) {
            return opposite ? BlockFace.NORTH : BlockFace.SOUTH;
        } else if (yaw >= 45 && yaw <= 135) {
            return opposite ? BlockFace.EAST : BlockFace.WEST;
        } else if (yaw >= -135 && yaw <= -45) {
            return opposite ? BlockFace.WEST : BlockFace.EAST;
        } else {
            return opposite ? BlockFace.SOUTH : BlockFace.NORTH;
        }
    }

    public static BlockFace getClosestBlockFace(double yaw) {
        return getClosestBlockFace(yaw, false);
    }

    /**
     * Check if a string matches an array of patterns
     * 
     * @param test The string to test
     * @param patterns The patterns to test against
     * @return
     */
    public static boolean regexMatch(String test, String[] patterns) {
        for (String pattern : patterns) {
            if (Pattern.compile(pattern).matcher(test).find()) {
                return true;
            }
        }

        return false; // No match found
    }

    public static boolean regexMatch(String test, Pattern[] patterns) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(test).find()) {
                return true;
            }
        }

        return false; // No match found
    }

    public static boolean worldExists(String raceWorldName) {
        return Bukkit.getWorld(raceWorldName) != null;
    }

    public static Location stringToLocation(String s) {
        String[] parts = s.split(",");
        if (parts.length == 4) {
            World world = Bukkit.getWorld(parts[0]);
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            return new Location(world, x, y, z);
        }

        return null;
    }

    public static String locationToString(Location location) {
        return location.getWorld().getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ();
    }
}
