package com.stemcraft.exceptions;

import com.stemcraft.STEMCraft;

/**
 * Represents our core exception.
 */
public class SMException extends RuntimeException {
    
    /**
     * Create a new exception.
     */
    public SMException() {
        STEMCraft.error(this);
    }

    /**
     * Create a new exception.
     * @param t The throwable
     */
    public SMException(Throwable t) {
        super(t);
        STEMCraft.error(this);
    }

    /**
     * Create a new exception.
     * @param message The message
     */
    public SMException(String message) {
        super(message);
        STEMCraft.error(this, message);
    }

    /**
     * Create a new exception.
     * @param message The message
     * @param t The throwable
     */
    public SMException(String message, Throwable t) {
        super(message, t);
        STEMCraft.error(t, message);
    }
}
