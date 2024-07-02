package com.stemcraft;

import com.stemcraft.utils.SMUtilsItemStack;
import com.stemcraft.utils.SMUtilsString;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.*;

@SuppressWarnings("UnusedReturnValue")
@Setter
@Getter
public class SMBook {
    private static HashMap<String, SMBook> bookList = new HashMap<>();
    private static String DEFAULT_TITLE = "STEMCraft";
    private static String DEFAULT_AUTHOR = "Unknown";
    private static List<String> DEFAULT_CONTENT = Collections.singletonList("The book is empty");

    private String name;
    private String title;
    private String author;
    private List<String> content;

    public SMBook(String name) {
        if (bookList.containsKey(name)) {
            throw new IllegalArgumentException("Book with the same name already exists.");
        }

        this.name = name;
        this.author = DEFAULT_TITLE;
        this.title = DEFAULT_AUTHOR;
        this.content = DEFAULT_CONTENT;
    }

    public void save() {
        SMConfig.set("books." + name + ".title", title);
        SMConfig.set("books." + name + ".author", author);
        SMConfig.set("books." + name + ".content", content);
        SMConfig.save("books");
    }

    public void open(Player player) {
        ItemStack book = asItem();

        if(SMPlayer.isBedrock(player)) {
            SMPlayer.give(player, book);
        } else {
            player.openBook(book);
        }
    }

    public ItemStack asItem() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta)book.getItemMeta();

        if(meta != null) {
            meta.setAuthor(this.author);
            meta.setTitle(this.title);
            meta.setPages(this.content);
            book.setItemMeta(meta);
        }

        SMUtilsItemStack.addAttribute(book, "book-name", this.name);
        SMItem.destroyOnDrop(book, true);
        return book;
    }

    public void remove() {
        SMBook.bookList.remove(name);
        SMConfig.remove("books." + name);
        SMConfig.save("books");
    }

    public boolean rename(String newName) {
        if(newName.isEmpty() || SMBook.exists(newName)) {
            return false;
        }

        bookList.remove(name);
        bookList.put(newName, this);
        SMConfig.renameKey("books." + name, "books." + newName);
        SMConfig.save("books");

        name = newName;
        return true;
    }

    public void setTitle(String title) {
        this.title = Objects.requireNonNullElse(title, "");
    }

    public void setContent(List<String> content) {
        if(content == null) {
            this.content = new ArrayList<>();
        } else {
            this.content = SMUtilsString.colorizeAll(content);
        }
    }

    public static SMBook get(String name) {
        return get(name, false);
    }

    public static SMBook get(String name, boolean create) {
        if(SMBook.bookList.containsKey(name)) {
            return SMBook.bookList.get(name);
        }

        if(SMConfig.getKeys("books").contains(name)) {
            SMBook book = new SMBook(name);
            String path = "books." + name;

            book.setAuthor(SMConfig.getString(path + ".author", DEFAULT_TITLE));
            book.setTitle(SMConfig.getString(path + ".title", DEFAULT_AUTHOR));
            book.setContent(SMConfig.getStringList(path + ".content", DEFAULT_CONTENT));

            bookList.put(name, book);
            return book;
        }

        if(!create) {
            return null;
        }

        return new SMBook(name);
    }

    public static SMBook create(String name) {
        if(SMBook.bookList.containsKey(name)) {
            return SMBook.bookList.get(name);
        }

        return new SMBook(name);
    }

    public static SMBook getOrCreate(String name) {
        return SMBook.get(name, true);
    }

    public static boolean exists(String name) {
        return SMConfig.getKeys("books").contains(name);
    }

    public static List<String> list() {
        return SMConfig.getKeys("books");
    }
}
