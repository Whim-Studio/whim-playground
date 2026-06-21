package com.tycoon.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The development pipeline for one game in production. Sim accumulates
 * development points, bugs and polish; the project is scored on release.
 */
public class GameProject {
    private final String id;
    private final String title;
    private ProjectPhase phase = ProjectPhase.DESIGN;
    private double developmentPoints = 0.0;
    private double bugs = 0.0;
    private double polish = 0.0;
    private Integer reviewScore = null;
    private final List<String> assignedEmployeeIds = new ArrayList<String>();

    public GameProject(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public ProjectPhase phase() {
        return phase;
    }

    public void setPhase(ProjectPhase p) {
        this.phase = p;
    }

    public double developmentPoints() {
        return developmentPoints;
    }

    public void addDevelopmentPoints(double p) {
        this.developmentPoints += p;
    }

    public double bugs() {
        return bugs;
    }

    public void addBugs(double b) {
        this.bugs += b;
    }

    /** 0..100 polish progress. */
    public double polish() {
        return polish;
    }

    public void addPolish(double amount) {
        this.polish += amount;
    }

    /** null until scored; 0..100 once RELEASED and reviewed. */
    public Integer reviewScore() {
        return reviewScore;
    }

    public void setReviewScore(int score) {
        this.reviewScore = score;
    }

    public List<String> assignedEmployeeIds() {
        return assignedEmployeeIds;
    }
}
