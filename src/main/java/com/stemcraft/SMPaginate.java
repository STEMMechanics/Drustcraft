package com.stemcraft;

import java.util.List;
import java.util.function.BiFunction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class SMPaginate {
    private CommandSender sender;
    private int page;
    private int count;
    private String command;
    private String title;
    private String none = "No items where found";
    private final static int ITEMS_PER_PAGE = 8;

    public SMPaginate(CommandSender sender, int page) {
        this.sender = sender;
        this.page = page;
    }

    public SMPaginate(CommandSender sender, String page) {
        try {
            this.page = Integer.parseInt(page);
        } catch(Exception e) {
            this.page = 0;
        }
    }

    public SMPaginate count(int count) {
        this.count = count;
        return this;
    }

    public SMPaginate command(String command) {
        this.command = command;
        return this;
    }

    public SMPaginate title(String title) {
        this.title = title;
        return this;
    }

    public SMPaginate none(String none) {
        this.none = none;
        return this;
    }

    public SMPaginate showItems(BiFunction<Integer, Integer, List<BaseComponent[]>> func) {
        int start = (page - 1) * ITEMS_PER_PAGE;
        SMMessenger.send(SMMessenger.MessageType.INFO, sender, count + " items found");
        SMMessenger.send(SMMessenger.MessageType.INFO, sender, ITEMS_PER_PAGE + " items per page");
        int maxPages = (count + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
        List<BaseComponent[]> lines = func.apply(start, ITEMS_PER_PAGE);

        if(lines.isEmpty()) {
            SMMessenger.send(SMMessenger.MessageType.ERROR, sender, none);
            return this;
        }

        sender.spigot().sendMessage(createSeperatorString(ChatColor.AQUA + title));

        // Display the content for the current page
        for (int i = 0; i < lines.size(); i++) {
            sender.spigot().sendMessage(lines.get(i));
        }

        // Pagination
        BaseComponent prev = new TextComponent((page <= 1 ? ChatColor.GRAY : ChatColor.GOLD) + "<<< ");
        if(page > 1) {
            prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command + " " + (page - 1)));
            prev.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Previous page")));
        }

        BaseComponent pageInfo = new TextComponent(ChatColor.YELLOW + "Page " + ChatColor.GOLD + page + ChatColor.YELLOW + " of " + maxPages);

        BaseComponent next = new TextComponent((page >= maxPages ? ChatColor.GRAY : ChatColor.GOLD) + " >>>");
        if(page < maxPages) {
            next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command + " " + (page + 1)));
            next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Next page")));
        }

        BaseComponent components[] = { prev, pageInfo, next };
        sender.spigot().sendMessage(createSeperatorString(components));
        return this;
    }

    private static BaseComponent[] createSeperatorString(String title) {
        TextComponent textComponent = new TextComponent(title);
        return createSeperatorString(new BaseComponent[]{textComponent});
    }

    private static BaseComponent[] createSeperatorString(BaseComponent[] titles) {
        StringBuilder plainTitleBuilder = new StringBuilder();
        for (BaseComponent title : titles) {
            plainTitleBuilder.append(title.toPlainText());
        }
        String plainTitle = plainTitleBuilder.toString();
        String separator = "-";

        int maxLength = 58;
        int titleLength = ChatColor.stripColor(plainTitle).length();
        int separatorLength = (maxLength - titleLength - 6) / 2;

        String separatorStr = ChatColor.YELLOW + separator.repeat(separatorLength) + ChatColor.RESET;

        BaseComponent[] components = {
                new TextComponent(separatorStr + " "),
                new TextComponent(titles),
                new TextComponent(" " + separatorStr)
        };

        return components;
    }

}
