package com.stemcraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SMUtils {

    /**
     * Convert a set object to a list object.
     *
     * @param set The set object to convert.
     * @return The converted list object.
     */
    public static List<String> convertSetToList(Set<?> set) {
        List<String> stringList = new ArrayList<>();
        for (Object obj : set) {
            stringList.add(String.valueOf(obj));
        }

        return stringList;
    }
}
