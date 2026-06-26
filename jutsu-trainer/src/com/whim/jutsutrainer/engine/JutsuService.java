package com.whim.jutsutrainer.engine;

import com.whim.jutsutrainer.domain.ChakraNature;
import com.whim.jutsutrainer.domain.HandSeal;
import com.whim.jutsutrainer.domain.Jutsu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure-logic search and training engine over a catalog of {@link Jutsu}.
 *
 * <p>Fully decoupled from UI and the data package: constructed from a plain
 * {@code List<Jutsu>}. All returned lists are unmodifiable; inputs and domain
 * objects are never mutated.
 */
public final class JutsuService {

    private final List<Jutsu> catalog;

    /**
     * @param catalog the jutsu catalog; an unmodifiable defensive copy is stored.
     *                A null catalog is treated as empty.
     */
    public JutsuService(List<Jutsu> catalog) {
        List<Jutsu> copy = new ArrayList<Jutsu>();
        if (catalog != null) {
            copy.addAll(catalog);
        }
        this.catalog = Collections.unmodifiableList(copy);
    }

    /** @return an unmodifiable list of every jutsu, in catalog order. */
    public List<Jutsu> all() {
        return catalog;
    }

    /**
     * Combined search: partial case-insensitive name substring match AND nature
     * filter. A null/blank {@code nameQuery} matches all names; a null
     * {@code nature} matches all natures. Catalog order is preserved.
     */
    public List<Jutsu> search(String nameQuery, ChakraNature nature) {
        String needle = (nameQuery == null) ? "" : nameQuery.trim().toLowerCase();
        List<Jutsu> result = new ArrayList<Jutsu>();
        for (Jutsu j : catalog) {
            if (nature != null && j.getNature() != nature) {
                continue;
            }
            if (needle.length() > 0) {
                String name = j.getName();
                if (name == null || !name.toLowerCase().contains(needle)) {
                    continue;
                }
            }
            result.add(j);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Training simulator: every jutsu whose seal list STARTS WITH {@code input}
     * (prefix match, in order). An empty/null input returns all jutsu that have
     * at least one seal. A jutsu shorter than the input cannot match.
     */
    public List<Jutsu> matchPrefix(List<HandSeal> input) {
        boolean emptyInput = (input == null || input.isEmpty());
        List<Jutsu> result = new ArrayList<Jutsu>();
        for (Jutsu j : catalog) {
            List<HandSeal> seals = j.getSeals();
            if (seals == null || seals.isEmpty()) {
                continue;
            }
            if (emptyInput) {
                result.add(j);
                continue;
            }
            if (seals.size() < input.size()) {
                continue;
            }
            if (startsWith(seals, input)) {
                result.add(j);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Subset of {@link #matchPrefix}: jutsu whose seal list EQUALS {@code input}
     * exactly. An empty/null input matches only jutsu with no seals.
     */
    public List<Jutsu> matchExact(List<HandSeal> input) {
        int inputSize = (input == null) ? 0 : input.size();
        List<Jutsu> result = new ArrayList<Jutsu>();
        for (Jutsu j : catalog) {
            List<HandSeal> seals = j.getSeals();
            int sealSize = (seals == null) ? 0 : seals.size();
            if (sealSize != inputSize) {
                continue;
            }
            if (inputSize == 0 || startsWith(seals, input)) {
                result.add(j);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** @return true if {@code seals} begins with the full {@code prefix} sequence, in order. */
    private static boolean startsWith(List<HandSeal> seals, List<HandSeal> prefix) {
        for (int i = 0; i < prefix.size(); i++) {
            HandSeal a = seals.get(i);
            HandSeal b = prefix.get(i);
            if (a == null ? b != null : !a.equals(b)) {
                return false;
            }
        }
        return true;
    }
}
