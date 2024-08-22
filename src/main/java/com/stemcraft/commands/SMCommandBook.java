package com.stemcraft.commands;

import com.stemcraft.*;
import com.stemcraft.utils.SMUtilsItemStack;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class SMCommandBook extends SMCommand {

    public SMCommandBook() {
        super("book");
        description("Book management");
        permission("stemcraft.command.book");
        tabCompletion("create");
        tabCompletion("save");
        tabCompletion("list");
        tabCompletion("delete", "{book}");
        tabCompletion("give", "{book}");
        tabCompletion("show", "{book}");
        tabCompletion("rename", "{book}");
        register();
    }

    @Override
    public String usage() {
        return "/book [create|save|list|delete|give|show|rename] [name] [author]";
    }

    @Override
    public void execute(SMCommandContext ctx) {
        if(ctx.args.wantsHelp()) {
            ctx.usage();
            return;
        }

        String action = ctx.args.shift("create|save|list|delete|give|show|rename");
        if(action == null) {
            ctx.usage();
            return;
        } else if(action.equalsIgnoreCase("create")) {
            if(ctx.fromConsole()) {
                ctx.error("This command is required to be run by a player");
                return;
            }

            if(!ctx.hasPermission("stemcraft.command.book.create")) {
                ctx.errorNoPermission();
                return;
            }

            String name = ctx.args.shift();
            if(name == null) {
                ctx.error("No book name was entered");
                return;
            }

            if(SMBook.exists(name)) {
                ctx.error("A book with that name already exists");
                return;
            }

            Material material = Material.getMaterial("BOOK_AND_QUILL");
            if (material == null)
                material = Material.getMaterial("WRITABLE_BOOK");
            if (material == null)
                throw new UnsupportedOperationException("Something went wrong with Bukkit Material!");

            ItemStack item = new ItemStack(material);
            SMUtilsItemStack.addAttribute(item, "book-name", name);
            SMItem.destroyOnDrop(item, true);

            if(SMPlayer.give(ctx.player, item)) {
                ctx.info("You have received a new book. Run '/book save' to save it to the server.");
            }
        } else if(action.equalsIgnoreCase("save")) {
            ItemStack item = ctx.player.getInventory().getItemInMainHand();
            if (!Objects.equals(item.getType().toString(), "BOOK_AND_QUILL") && !Objects.equals(item.getType().toString(), "WRITABLE_BOOK") && !SMUtilsItemStack.hasAttribute(item, "book-name")) {
                ctx.error("You are not holding a STEMCraft book");
                return;
            }

            SMBook book = SMBook.getOrCreate(SMUtilsItemStack.getAttribute(item, "book-name", String.class));
            if(book == null) {
                ctx.error("Error getting the book information");
                return;
            }

            BookMeta meta = (BookMeta) item.getItemMeta();
            if(meta == null) {
                ctx.error("Error getting the book meta data");
                return;
            }

            String title = meta.getTitle();
            
            book.setAuthor(ctx.player.getName());
            if(title != null) {
                book.setTitle(title);
            }
            book.setContent(meta.getPages());

            meta.getPages().forEach(ctx::info);

            book.save();

            ctx.info("The book has been saved");
        } else if(action.equalsIgnoreCase("list")) {
            Collection<String> bookList = SMBook.list();

            new SMPaginate(ctx.sender, ctx.args.shift("{integer}", 1))
                .count(bookList.size())
                .command("book list")
                .title("Books")
                .none("No books where found")
                .showItems((start, max) -> {
                    List<BaseComponent[]> rows = new ArrayList<>();
                    int end = Math.min(start + max, bookList.size());

                    for (String bookName : bookList.stream().skip(start).limit(end - start).toList()) {
                        BaseComponent[] row = new BaseComponent[1];
                        row[0] = new TextComponent(ChatColor.GOLD + bookName);
                        rows.add(row);
                    }

                    return rows;
                });
        } else if(action.equalsIgnoreCase("delete")) {
            if(ctx.args.isEmpty()) {
                ctx.error("No book name was entered");
                return;
            }

            String name = ctx.args.shift("{book}");
            if(name == null) {
                ctx.error("The book was not found");
                return;
            }

            SMBook book = SMBook.get(name);
            if(book == null) {
                ctx.error("Error loading the book");
                return;
            }

            book.remove();
            ctx.info("The book has been removed");
        } else if(action.equalsIgnoreCase("give")) {
            if(ctx.args.isEmpty()) {
                ctx.error("No book name was entered");
                return;
            }

            String name = ctx.args.shift("{book}");
            if(name == null) {
                ctx.error("The book was not found");
                return;
            }

            SMBook book = SMBook.get(name);
            if(book == null) {
                ctx.error("Error loading the book");
                return;
            }

            Material material = Material.getMaterial("BOOK_AND_QUILL");
            if (material == null)
                material = Material.getMaterial("WRITABLE_BOOK");
            if (material == null)
                throw new UnsupportedOperationException("Something went wrong with Bukkit Material!");

            ItemStack item = new ItemStack(material);
            BookMeta meta = (BookMeta) item.getItemMeta();
            if(meta == null) {
                ctx.error("Could not get the book meta");
                return;
            }

            meta.setAuthor(book.getAuthor());
            meta.setTitle(book.getTitle());
            meta.setPages(book.getContent());
            item.setItemMeta(meta);

            SMUtilsItemStack.addAttribute(item, "book-name", name);
            SMItem.destroyOnDrop(item, true);

            if(SMPlayer.give(ctx.player, item)) {
                ctx.info("You have received a new book. Run '/book save' to save it to the server.");
            }
        } else if(action.equalsIgnoreCase("show")) {
            if(ctx.args.isEmpty()) {
                ctx.error("No book name was entered");
                return;
            }

            String name = ctx.args.shift("{book}");
            if(name == null) {
                ctx.error("The book was not found");
                return;
            }

            SMBook book = SMBook.get(name);
            if(book == null) {
                ctx.error("Error loading the book");
                return;
            }

            book.open(ctx.player);
        } else if(action.equalsIgnoreCase("rename")) {
            if(ctx.args.isEmpty()) {
                ctx.error("No book name was entered");
                return;
            }

            String name = ctx.args.shift("{book}");
            if(name == null) {
                ctx.error("The book was not found");
                return;
            }

            if(ctx.args.isEmpty()) {
                ctx.error("No new book name was entered");
                return;
            }

            String newName = ctx.args.shift();

            SMBook book = SMBook.get(name);
            if(book == null) {
                ctx.error("Error loading the book");
                return;
            }

            book.rename(newName);
            ctx.info("The book was renamed");
        } else {
            ctx.error("Unknown action: " + action);
        }
    }
}
