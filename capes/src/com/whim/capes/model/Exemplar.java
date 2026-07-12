package com.whim.capes.model;

/**
 * An Exemplar relationship (pp.75-77): another character who embodies a Drive's
 * tension for the primary character, plus a permanent Free Conflict (the "root
 * conflict") playable once per Scene when both appear.
 */
public final class Exemplar implements java.io.Serializable {
    private final String exemplarCharacterId; // the character acting as Exemplar
    private DriveType drive;                   // which Drive it embodies
    private String rootConflictStatement;      // e.g. "I respect Gerard, but he thinks I'm a criminal."
    private ConflictType freeConflictType;     // EVENT or GOAL for the Free Conflict

    public Exemplar(String exemplarCharacterId, DriveType drive,
                    String rootConflictStatement, ConflictType freeConflictType) {
        this.exemplarCharacterId = exemplarCharacterId;
        this.drive = drive;
        this.rootConflictStatement = rootConflictStatement;
        this.freeConflictType = freeConflictType;
    }

    public String exemplarCharacterId() { return exemplarCharacterId; }
    public DriveType drive() { return drive; }
    public void setDrive(DriveType d) { this.drive = d; }
    public String rootConflictStatement() { return rootConflictStatement; }
    public void setRootConflictStatement(String s) { this.rootConflictStatement = s; }
    public ConflictType freeConflictType() { return freeConflictType; }
    public void setFreeConflictType(ConflictType t) { this.freeConflictType = t; }
}
