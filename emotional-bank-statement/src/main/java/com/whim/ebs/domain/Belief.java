package com.whim.ebs.domain;

/**
 * Immutable value object wrapping a single belief name.
 *
 * <p>Equality and hash code are based solely on the belief {@code name}.</p>
 */
public final class Belief {

    private final String name;

    /**
     * Creates a belief with the given name.
     *
     * @param name the belief name (must not be null)
     */
    public Belief(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        this.name = name;
    }

    /**
     * Returns the belief name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Belief other = (Belief) o;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
