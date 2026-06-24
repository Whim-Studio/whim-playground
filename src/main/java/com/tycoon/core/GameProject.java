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
    private Genre genre = null;
    private Topic topic = null;
    private Technology technology = null;
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

    /** Convenience constructor for a fully-specified project (genre + topic + engine). */
    public GameProject(String id, String title, Genre genre, Topic topic, Technology technology) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.topic = topic;
        this.technology = technology;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    /** Game genre; null if unspecified. */
    public Genre genre() {
        return genre;
    }

    public void setGenre(Genre genre) {
        this.genre = genre;
    }

    /** Game topic/setting; null if unspecified. */
    public Topic topic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    /** Engine the game is built on; null if unspecified. */
    public Technology technology() {
        return technology;
    }

    public void setTechnology(Technology technology) {
        this.technology = technology;
    }

    /** Genre/topic fit for this project (OK when either is unspecified). */
    public GenreTopicMatch.Rating matchRating() {
        return GenreTopicMatch.rate(genre, topic);
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
