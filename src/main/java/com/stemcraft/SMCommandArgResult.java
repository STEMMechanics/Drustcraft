package com.stemcraft;

public class SMCommandArgResult<T> {
    public T value;
    public final String arg;
    public final boolean found;

    public SMCommandArgResult(T value, String arg, boolean found) {
        this.value = value;
        this.arg = arg;
        this.found = found;
    }
}