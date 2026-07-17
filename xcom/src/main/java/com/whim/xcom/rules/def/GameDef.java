package com.whim.xcom.rules.def;

/**
 * Common contract for every data-driven content definition. Each def is
 * identified by a stable string id (its key in the {@code Ruleset} registry and
 * in data packs) and carries a human-readable display name.
 */
public interface GameDef {

    /** Stable identifier, unique within its def category (e.g. {@code "rifle"}). */
    String id();

    /** Display name for the UI (e.g. {@code "Rifle"}). */
    String name();
}
