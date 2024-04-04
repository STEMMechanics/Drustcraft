package com.stemcraft.core.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.stemcraft.core.SMCommon;
import com.stemcraft.core.SMLocale;
import com.stemcraft.core.SMMessenger;
import com.stemcraft.core.SMReplacer;
import com.stemcraft.core.exception.SMCommandException;

public class SMCommandContext {
    public SMCommand command;
    public String alias;
    public CommandSender sender;
    public Player player;
    public List<String> args;
    public List<String> dashArgs;
    public Map<String, String> optionArgs;

    public SMCommandContext(SMCommand command, CommandSender sender, String alias, String[] args) {
        this.command = command;
        this.sender = sender;
        this.alias = alias.toLowerCase();

        this.player = sender instanceof Player ? (Player) sender : null;

        dashArgs = new ArrayList<>();
        optionArgs = new HashMap<>();
        this.args = new ArrayList<>(Arrays.asList(args)); // Convert args array to list

        Iterator<String> iterator = this.args.iterator();
        while (iterator.hasNext()) {
            String arg = iterator.next();
            if (arg.startsWith("-")) {
                dashArgs.add(arg);
                iterator.remove();
            } else if (arg.matches("^[a-zA-Z0-9-_]:.*")) {
                int index = arg.indexOf(":");
                String option = arg.substring(0, index);
                String value = arg.substring(index + 1);

                optionArgs.putIfAbsent(option.toLowerCase(), value);
                iterator.remove();
            }
        }
    }

    /**
     * Return if the sender has the specified permission.
     * 
     * @param permission
     * @return
     */
    public Boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }

    /**
     * Return if command has been run from console.
     * 
     * @return
     */
    public Boolean fromConsole() {
        return player == null;
    }

    /**
     * Internal throw command exception with string and replacements
     * 
     * @param msg
     * @param replacements
     */
    private void throwCommandException(String msg, String... replacements) {
        if (replacements.length % 2 != 0) {
            throw new IllegalArgumentException("Odd number of replacements provided. Expecting key-value pairs.");
        }

        Map<String, String> replacementMap = new HashMap<>();
        for (int i = 0; i < replacements.length; i += 2) {
            String key = replacements[i].toString();
            String value = (replacements[i + 1] != null) ? replacements[i + 1].toString() : null;
            replacementMap.put(key, value);
        }

        replacementMap.put("label", command.getLabel());
        msg = SMReplacer.replaceVariables(msg, replacementMap);

        throw new SMCommandException(msg);
    }

    /**
     * Throw an exception unless the sender is a player.
     */
    public void checkNotConsole() {
        if (!(sender instanceof Player)) {
            throwCommandException(
                    SMReplacer.replaceVariables(
                            SMLocale.get(this.sender, "CMD_ONLY_PLAYERS"),
                            "label", command.getLabel()));
        }
    }

    /**
     * Throw an exception unless a boolean is true.
     * 
     * @param b
     * @param onFail
     */
    public void checkBoolean(Boolean b, String onFail) {
        if (!b) {
            throwCommandException(onFail);
        }
    }

    /**
     * Throw an exception unless a boolean is true.
     * 
     * @param b
     * @param onFail
     */
    public void checkBooleanLocale(Boolean b, String onFailId) {
        if (!b) {
            throwCommandException(SMLocale.get(sender, onFailId));
        }
    }

    /**
     * Throw an exception unless a object is not null.
     * 
     * @param b
     * @param onFail
     */
    public void checkNotNullLocale(Object o, String onFailId) {
        if (o == null) {
            throwCommandException(SMLocale.get(sender, onFailId));
        }
    }

    /**
     * Check the the argument size is equal to or larger than specified.
     * 
     * @param size
     * @param onFail
     */
    public void checkArgs(Integer size, String onFail) {
        if (this.args.size() < size) {
            throwCommandException(onFail);
        }
    }

    /**
     * Check the the argument size is equal to or larger than specified.
     * 
     * @param size
     * @param onFail
     */
    public void checkArgsLocale(Integer size, String onFailId) {
        if (this.args.size() < size) {
            throwCommandException(SMLocale.get(sender, onFailId));
        }
    }

    /**
     * Check the the argument size is equal to or larger than specified.
     * 
     * @param array
     * @param value
     * @param onFail
     */
    public void checkInArrayLocale(String[] array, String value, String onFailId) {
        boolean valueFound = false;

        for (String element : array) {
            if (element.equalsIgnoreCase(value)) {
                valueFound = true;
                break;
            }
        }

        if (!valueFound) {
            throwCommandException(SMLocale.get(sender, onFailId));
        }
    }

    /**
     * Throw an exception unless the sender has the specified permission.
     */
    public void checkPermission(String permission) {
        if (!sender.hasPermission(permission)) {
            throwCommandException(SMLocale.get(this.sender, "CMD_NO_PERMISSION"));
        }
    }

    /**
     * Throw an exception unless the sender has the specified permission or condition is true.
     */
    public void checkPermission(Boolean condition, String permission) {
        if (!condition && !sender.hasPermission(permission)) {
            throwCommandException(SMLocale.get(this.sender, "CMD_NO_PERMISSION"));
        }
    }

    /**
     * Get Command argument from index.
     * 
     * @param idx
     * @return
     */
    public String getArg(int idx, String defValue) {
        return idx > 0 && this.args.size() >= idx ? this.args.get(idx - 1) : defValue;
    }

    public String getArg(int idx) {
        return getArg(idx, "");
    }

    /**
     * Get Command argument from index.
     * 
     * @param idx
     * @return
     */
    private int getArgInt(int idx, int defValue, String onFail, Boolean isLocale) {
        try {
            return idx > 0 && this.args.size() >= idx ? Integer.parseInt(this.args.get(idx - 1)) : defValue;
        } catch (Exception e) {
            if (onFail != null) {
                if (isLocale) {
                    throwCommandException(SMLocale.get(sender, onFail));
                } else {
                    throwCommandException(onFail);
                }
            }

            return defValue;
        }
    }

    public int getArgInt(int idx, int defValue) {
        return getArgInt(idx, defValue, null, false);
    }

    public int getArgInt(int idx, String onFail) {
        return getArgInt(idx, 0, onFail, false);
    }

    public int getArgIntLocale(int idx, String onFail) {
        return getArgInt(idx, 0, onFail, true);
    }

    /**
     * Get Command argument from index.
     * 
     * @param idx
     * @return
     */
    private float getArgFloat(int idx, float defValue, String onFail, Boolean isLocale) {
        try {
            return idx > 0 && this.args.size() >= idx ? Float.parseFloat(this.args.get(idx - 1)) : defValue;
        } catch (Exception e) {
            if (onFail != null) {
                if (isLocale) {
                    throwCommandException(SMLocale.get(sender, onFail));
                } else {
                    throwCommandException(onFail);
                }
            }

            return defValue;
        }
    }

    public float getArgFloat(int idx, float defValue) {
        return getArgFloat(idx, defValue, null, false);
    }

    public float getArgFloat(int idx, String onFail) {
        return getArgFloat(idx, 0f, onFail, false);
    }

    public float getArgFloatLocale(int idx, String onFail) {
        return getArgFloat(idx, 0f, onFail, true);
    }

    /**
     * Get Player from argument index.
     * 
     * @param idx
     * @param defValue
     * @return
     */
    public Player getArgAsPlayer(Integer idx, Player defValue) {
        String playerName = getArg(idx, "");
        if (playerName != "") {
            return SMCommon.findPlayer(playerName);
        }

        return defValue;
    }

    /**
     * Throw an exception on invalid arguments.
     */
    public final void returnInvalidArgs() {
        throwCommandException(SMLocale.get(this.sender, "CMD_INVALID_OPTION"));
    }

    /**
     * Return with an info message.
     * 
     * @param message
     */
    public final void returnInfo(String message) {
        SMMessenger.info(sender, message);
        throw new SMCommandException();
    }

    /**
     * Return with an info locale message.
     * 
     * @param message
     */
    public final void returnInfoLocale(String id, String... replacements) {
        SMMessenger.infoLocale(sender, id, replacements);
        throw new SMCommandException();
    }

    /**
     * Return with an success message.
     * 
     * @param message
     */
    public final void returnSuccess(String message) {
        SMMessenger.success(sender, message);
        throw new SMCommandException();
    }

    /**
     * Return with an success locale message.
     * 
     * @param message
     */
    public final void returnSuccessLocale(String id, String... replacements) {
        SMMessenger.successLocale(sender, id, replacements);
        throw new SMCommandException();
    }

    /**
     * Return with an warning message.
     * 
     * @param message
     */
    public final void returnWarn(String message) {
        SMMessenger.warn(sender, message);
        throw new SMCommandException();
    }

    /**
     * Return with an warning locale message.
     * 
     * @param message
     */
    public final void returnWarnLocale(String id, String... replacements) {
        SMMessenger.warnLocale(sender, id, replacements);
        throw new SMCommandException();
    }

    /**
     * Return with an error message.
     * 
     * @param message
     */
    public final void returnError(String message) {
        SMMessenger.error(sender, message);
        throw new SMCommandException();
    }

    /**
     * Return with an error locale message.
     * 
     * @param message
     */
    public final void returnErrorLocale(String id, String... replacements) {
        SMMessenger.errorLocale(sender, id, replacements);
        throw new SMCommandException();
    }

    /**
     * Resolve a sender name as either console or player
     * 
     * @param sender
     * @return
     */
    public String senderName() {
        return sender instanceof Player ? sender.getName() : SMLocale.get("CONSOLE_NAME");
    }

    public String[] getSubCommandList() {
        return command.getSubCommandList();
    }
}
