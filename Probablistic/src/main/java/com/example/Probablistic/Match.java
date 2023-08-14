package com.example.Probablistic;


public class Match {
    private Team homeTeam;
    private Team awayTeam;
    private String date;

    public Match(Team homeTeam, Team awayTeam, String date) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.date = date;
    }

    public Team getHomeTeam() {
        return homeTeam;
    }

    public Team getAwayTeam() {
        return awayTeam;
    }

    public String getDate() {
        return date;
    }
}