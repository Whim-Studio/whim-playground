package com.tiwas.rpg.json;

/** Thrown when JSON text is malformed or a coercion fails. */
public class JsonException extends RuntimeException {
    public JsonException(String msg) {
        super(msg);
    }
}
