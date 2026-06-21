package com.tycoon.core;
// LOCAL-ONLY VERIFICATION STUB (Task 1 owns this) — never committed by Task 2.
import java.util.ArrayList;
import java.util.List;
public class GameProject {
    private final String id, title;
    private ProjectPhase phase = ProjectPhase.DESIGN;
    private double developmentPoints, bugs, polish;
    private Integer reviewScore;
    private final List<String> assignedEmployeeIds = new ArrayList<String>();
    public GameProject(String id, String title) { this.id = id; this.title = title; }
    public String id() { return id; }
    public String title() { return title; }
    public ProjectPhase phase() { return phase; }
    public void setPhase(ProjectPhase p) { phase = p; }
    public double developmentPoints() { return developmentPoints; }
    public void addDevelopmentPoints(double p) { developmentPoints += p; }
    public double bugs() { return bugs; }
    public void addBugs(double b) { bugs = Math.max(0.0, bugs + b); }
    public double polish() { return polish; }
    public void addPolish(double amount) { polish = Math.max(0.0, Math.min(100.0, polish + amount)); }
    public Integer reviewScore() { return reviewScore; }
    public void setReviewScore(int score) { reviewScore = score; }
    public List<String> assignedEmployeeIds() { return assignedEmployeeIds; }
}
