package com.stemcraft;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SMTabCompletion<T> {
    @Getter
    private final String name;

    public SMTabCompletion(String name) {
        this.name = name;
    }

    public boolean onLoad() {
        return true;
    }

    public List<String> completions(String arg) {
        return new ArrayList<>();
    }

    public T resolve(String arg) {
        return null;
    }

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
