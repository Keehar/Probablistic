package com.example.Probablistic;

public class Team {
    private String name;
    private int conference;
    private int goalDifference;
    private int wins;
    private int ties;
    private int losses;
    private int points;

    private int seed = -1;
    // name, win, tie, loss, points, GD, conference(E/W)
    public Team(String name, int wins, int ties, int losses, int points, int GD, int conference) {
        this.name = name;
        this.wins = wins;
        this.ties = ties;
        this.losses = losses;
        this.points = points;
        this.goalDifference =GD;
        this.conference = conference;


    }
    public Team(Team team) {
        this.name = team.getName();
        this.wins = team.getWins();
        this.ties = team.getTies();
        this.losses = team.getLosses();
        this.points = team.getPoints();
        this.goalDifference = team.getGoalDifference();
        this.conference = team.getConference();
        this.seed = team.getSeed();
    }

    public Object clone() { return (Team)this; }
    public void setSeed(int seed) { this.seed = seed; }
    public String getName() {
        return name;
    }

    public int getConference() {
        return conference;
    }

    public int getSeed() {
        return seed;
    }

    public int getWins() {
        return wins;
    }

    public int getTies() {
        return ties;
    }

    public int getLosses() {
        return losses;
    }

    public int getPoints() {
        return points;
    }
    public void incrementWins() {
        this.wins++;
    }

    public void incrementTies() {
        this.ties++;
    }

    public void incrementLosses() {
        this.losses++;
    }

    // make me decrement wins, ties, and losses methods that do the opposite of the increment methods above
    public void decrementWins() {
        this.wins--;
    }

    public void decrementTies() {
        this.ties--;
    }

    public void decrementLosses() {
        this.losses--;
    }

    public void calculatePoints() {
        this.points = wins * 3 + ties;
    }

    public int getGoalDifference() {
        return goalDifference;
    }

    public String toString() {
        return seed + ": " + name + " - " + wins + "-" + ties + "-" + losses + " - " + points + " - " + goalDifference + " - " + conference;
    }

    public int getGD() {
        return goalDifference;
    }
}