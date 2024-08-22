package com.stemcraft;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

@Getter
public class SMTabCompletion<T> {
    /**
     * The name of this tab completion handler.
     */
    private final String name;

    /**
     * Function to generate tab completions.
     */
    private final Function<String, List<String>> completionsCallback;

    /**
     * Function to resolve an argument to the generic type T.
     */
    private final Function<String, T> resolveCallback;

    /**
     * Constructor
     */
    public SMTabCompletion(String name) {
        this(name, null, null);
    }

    public SMTabCompletion(String name, Function<String, List<String>> completionsCallback) {
        this(name, completionsCallback, null);
    }

    public SMTabCompletion(String name, Function<String, List<String>> completionsCallback, Function<String, T> resolveCallback) {
        this.name = name;
        this.completionsCallback = completionsCallback;
        this.resolveCallback = resolveCallback;
    }

    /**
     * Initializer method
     * @return If the tab completion class loaded
     */
    public boolean onLoad() {
        return true;
    }

    /**
     * Return an array list of completions
     * @param unused This param is unused
     * @return An array list of valid completions or an empty arraylist
     */
    public List<String> completions(String unused) {
        if(completionsCallback != null) {
            return completionsCallback.apply(unused);
        }
        return new ArrayList<>();
    }

    /**
     * Resolve a string to an object
     * @param arg The string to resolve
     * @return The resolved object or null
     */
    public T resolve(String arg) {
        if(resolveCallback != null) {
            return resolveCallback.apply(arg);
        }
        return null;
    }

    /**
     * Resolve a collection of strings to a collection of objects
     * @param args The collection of strings to resolve
     * @return The resolved collection of objects
     */
    public Collection<T> resolveCollection(Collection<String> args) {
        List<T> resolved = new ArrayList<>();
        for (String arg : args) {
            T resolvedArg = resolve(arg);
            if (resolvedArg != null) {
                resolved.add(resolvedArg);
            }
        }

        return resolved;
    }

    public Collection<T> resolveAll(Collection<String> args) {
        return new ArrayList<>();
    }
}
