package com.whim.ebs.domain;

/**
 * Mutable holder for the user's current working session: the selected belief,
 * exactly three proof entries, and a daily action.
 *
 * <p>String inputs are never stored as {@code null}; null is coerced to the
 * empty string. The proofs array is always of fixed length 3.</p>
 */
public final class SessionState {

    /** Fixed number of proof entries. */
    public static final int PROOF_COUNT = 3;

    private Belief selectedBelief;
    private final String[] proofs;
    private String dailyAction;

    /**
     * Creates an empty session state with three empty proofs and an empty
     * daily action.
     */
    public SessionState() {
        this.proofs = new String[PROOF_COUNT];
        for (int i = 0; i < PROOF_COUNT; i++) {
            this.proofs[i] = "";
        }
        this.dailyAction = "";
    }

    /**
     * Returns the selected belief, or {@code null} if none is selected.
     *
     * @return the selected belief or null
     */
    public Belief getSelectedBelief() {
        return selectedBelief;
    }

    /**
     * Sets the selected belief.
     *
     * @param belief the belief (may be null)
     */
    public void setSelectedBelief(Belief belief) {
        this.selectedBelief = belief;
    }

    /**
     * Returns the proof at the given index.
     *
     * @param index the index, 0..2
     * @return the proof string (never null)
     */
    public String getProof(int index) {
        checkIndex(index);
        return proofs[index];
    }

    /**
     * Sets the proof at the given index. Null is coerced to "".
     *
     * @param index the index, 0..2
     * @param proof the proof string (null becomes "")
     */
    public void setProof(int index, String proof) {
        checkIndex(index);
        proofs[index] = proof == null ? "" : proof;
    }

    /**
     * Returns a defensive copy of the proofs array.
     *
     * @return a copy of the proofs (length 3)
     */
    public String[] getProofs() {
        String[] copy = new String[PROOF_COUNT];
        System.arraycopy(proofs, 0, copy, 0, PROOF_COUNT);
        return copy;
    }

    /**
     * Returns the daily action.
     *
     * @return the daily action (never null)
     */
    public String getDailyAction() {
        return dailyAction;
    }

    /**
     * Sets the daily action. Null is coerced to "".
     *
     * @param dailyAction the daily action (null becomes "")
     */
    public void setDailyAction(String dailyAction) {
        this.dailyAction = dailyAction == null ? "" : dailyAction;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= PROOF_COUNT) {
            throw new IndexOutOfBoundsException("proof index must be 0.." + (PROOF_COUNT - 1) + ", was " + index);
        }
    }
}
