package com.stemcraft;

import com.stemcraft.utils.SMUtilsItemStack;
import org.bukkit.inventory.ItemStack;

public class SMItem {
    public static boolean destroyOnDrop(ItemStack item) {
        return destroyOnDrop(item, null);
    }

    public static boolean destroyOnDrop(ItemStack item, Boolean destroy) {
        String ATTRIBUTE_DESTROY_ON_DROP = "destroy-on-drop";
        if(destroy != null) {
            SMUtilsItemStack.addAttribute(item, ATTRIBUTE_DESTROY_ON_DROP, destroy ? 1 : 0);
        }

        return SMUtilsItemStack.getAttribute(item, ATTRIBUTE_DESTROY_ON_DROP, Integer.class, 0) == 1;
    }
}
