package com.example.Probablistic;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.Properties;
import java.io.InputStream;
import java.io.FileInputStream;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MLSSimulator {
    private static final int MAX_OUTCOMES = 100;
    private String targetTeamName; // needs to be user input
    private int targetSeed; // needs to be user input
    private boolean greaterThanAndEqualTo; // needs to be user input
    private String history = "";
    private int maxGames = 15;
    private List<Team> teams;
    private List<Match> remainingMatches;
    private List<String> allOutcomes;
    private List<String> satisfactoryOutcomes;
    private List<String> satisfactoryMatches;
    private List<String> remainingMatchesString;
    private List<String> outcomesList;
    private List<String> filteredOutcomes;
    private String connectionUrl;

    public MLSSimulator() throws IOException {
        teams = new ArrayList<>();
        remainingMatches = new ArrayList<>();
        satisfactoryOutcomes = new ArrayList<>();
        satisfactoryMatches = new ArrayList<>();
        remainingMatchesString = new ArrayList<>();
        outcomesList = new ArrayList<>();
        filteredOutcomes = new ArrayList<>();

        allOutcomes = new ArrayList<>();

        Properties prop = new Properties();
        InputStream input = null;

        input = new FileInputStream("db.properties");
        prop.load(input);

        connectionUrl = prop.getProperty("db.url")
                + "database=" + prop.getProperty("db.name") + ";"
                + "user=" + prop.getProperty("db.user") + ";"
                + "password=" + prop.getProperty("db.password") + ";"
                + "encrypt=true;"
                + "trustServerCertificate=false;"
                + "loginTimeout=30;";
    }

    // test method (where most of the printing to console happens)
    public void run() {

            readTeams();
            readRemainingMatches();

            System.out.println(returnTeams());
            System.out.println(returnRemainingMatches());

            setTargetTeam("Miami");
            setTargetSeed(13);
            setGreaterThanAndEqualTo(false);

            long time1 = System.nanoTime();
            simulateOutcomes(0);
            System.out.println("simulation complete");
            long time2 = System.nanoTime();
            System.out.println((time2-time1)/1000000000.0/60.0 + " minutes elapsed");

            writeAllOutcomesToDB();
            System.out.println("writing complete");
            long time3 = System.nanoTime();
            System.out.println((time3-time2)/1000000000.0/60.0 + " minutes elapsed");

            readAllOutcomesFromDB();
            System.out.println("reading complete");
            long time4 = System.nanoTime();
            //System.out.println((time4-time3)/1000000000.0/60.0 + " minutes elapsed");

            checkSatisfactoryOutcomes();
            System.out.println("checking complete");
            long time5 = System.nanoTime();
            System.out.println((time5-time4)/1000000000.0/60.0 + " minutes elapsed");

            //System.out.println(returnSatisfacotyOutcomes());
            long time6 = System.nanoTime();
            System.out.println((time6-time5)/1000000000.0/60.0 + " minutes elapsed");

            int[] gameNums = {0, 1};
            char[] results = {'T', 'L'};

            System.out.println(filter(gameNums, results));
            System.out.println("filtering done");
    }
    // reads the teams and their stats from the teams.txt file and puts them into the teams list
    private void writeAllOutcomesToDB() {
        try {
            Connection connection = DriverManager.getConnection(connectionUrl);
            Statement statement = connection.createStatement();
            statement.executeUpdate("IF OBJECT_ID('dbo.Outcomes', 'U') IS NOT NULL DROP TABLE dbo.Outcomes; CREATE TABLE dbo.Outcomes ( [Outcome] NVARCHAR(3500) NOT NULL,);");
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO dbo.Outcomes ( [Outcome]) VALUES (?);");
            for (int a = 0; a < allOutcomes.size(); a++) {
                if (a != 0 && a % 1000 == 0) {
                    preparedStatement.executeBatch();
                }
                preparedStatement.setString(1, allOutcomes.get(a));
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void readAllOutcomesFromDB() {
        try {
            allOutcomes.clear();
            Connection connection = DriverManager.getConnection(connectionUrl);
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT * FROM dbo.Outcomes;");
            while (result.next()) {
                String outcome = result.getString("Outcome");
                allOutcomes.add(outcome);
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void readTeams() {
        try {
            teams.clear();
            Connection connection = DriverManager.getConnection(connectionUrl);
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT * FROM dbo.Standings;");
            while (result.next()) {
                String teamName = result.getString("TeamName");
                int wins = result.getInt("Wins");
                int ties = result.getInt("Ties");
                int losses = result.getInt("Losses");
                int points = result.getInt("Points");
                int goalDifference = result.getInt("GoalDifference");
                String conference = result.getString("Conference");
                int conferenceInt = -1;
                if (conference.equals("E")) {
                    conferenceInt = 0;
                }
                else if (conference.equals("W")) {
                    conferenceInt = 1;
                }
                teams.add(new Team(teamName, wins, ties, losses, points, goalDifference, conferenceInt));
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // returns the teams, which conference they are in, and which seed they are in their conference
    @GetMapping("/returnteams")// DONE
    public String returnTeams() {
        readTeams();
        String output = "";
        sortTeams();
        int counter = 1;
        for (Team team : teams) {
            if (counter == 1) {
                output += "Eastern Conference\n";
            }
            else if (counter == 16) {
                output += "Western Conference\n";
            }
            output += "\tSeed:" + team.getSeed() + " - " + team.getName() + " - Points: " + team.getPoints() + " (" + team.getWins() + "-" + team.getTies() + "-" + team.getLosses() + ") GD: " + team.getGoalDifference() + "\n";
            counter++;
        }
        return output;
    }
    // reads the remaining matches from the remaining_matches.txt file and puts them into the remainingMatches list
    private void readRemainingMatches() {
        try {
            readTeams();
            Connection connection = DriverManager.getConnection(connectionUrl);
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT * FROM dbo.Schedule;");
            List <Match> allRemainingMatches = new ArrayList<>();
            while (result.next()) {
                String homeTeamName = result.getString("HomeTeamName");
                String awayTeamName = result.getString("AwayTeamName");
                String date = result.getString("Date");

                Team homeTeam = getTeamByName(homeTeamName);
                Team awayTeam = getTeamByName(awayTeamName);

                Match match = new Match(homeTeam, awayTeam, date);
                allRemainingMatches.add(match);
            }
            List <List<Match>> allRemainingMatchesByDate = new ArrayList<>();
            List <Match> firstDate = new ArrayList<>();
            int index = 0;
            for (int a = 0; a < allRemainingMatches.size(); a++) {
                if (a == 0) {
                    allRemainingMatchesByDate.add(firstDate);
                    allRemainingMatchesByDate.get(index).add(allRemainingMatches.get(a));
                }
                else if (allRemainingMatches.get(a).getDate().equals(allRemainingMatches.get(a-1).getDate())) {
                    allRemainingMatchesByDate.get(index).add(allRemainingMatches.get(a));
                }
                else {
                    index++;
                    List <Match> newDate = new ArrayList<>();
                    allRemainingMatchesByDate.add(newDate);
                    allRemainingMatchesByDate.get(index).add(allRemainingMatches.get(a));
                }
            }
            remainingMatches.clear();
            for (int a = 0; a < allRemainingMatchesByDate.size(); a++) {
                if (remainingMatches.size() + allRemainingMatchesByDate.get(a).size() <= maxGames) {
                    for (int b = 0; b < allRemainingMatchesByDate.get(a).size(); b++) {
                        remainingMatches.add(allRemainingMatchesByDate.get(a).get(b));
                    }
                }
                else {
                    break;
                }
            }


        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // reads the remaining matches from the remaining_matches.txt file and the dates of the matches and returns that
    @GetMapping("/returnremainingmatches")// DONE
    public String returnRemainingMatches() {
        readRemainingMatches();
        String output = "";
        for (int a = 0; a < remainingMatches.size(); a++) {
            String game = remainingMatches.get(a).getHomeTeam().getName() + " vs " + remainingMatches.get(a).getAwayTeam().getName() + " on " + remainingMatches.get(a).getDate() + "\n";
            output += game;
            remainingMatchesString.add(game);
        }
        return output;
    }
    // given the team name, returns the team object
    private Team getTeamByName(String name) {
        for (Team team : teams) {
            if (team.getName().equals(name)) {
                return team;
            }
        }
        return null;
    }
    // does the simulation (and for each win/tie/loss, it calls the simulateOutcome method)
    @GetMapping("/simulateoutcomes")// Done
    public void simulateOutcomes(@RequestParam(value = "matchIndex", defaultValue = "0") int matchIndex) {
        if (matchIndex == 0) {
            readTeams();
            readRemainingMatches();
        }
        if (matchIndex == remainingMatches.size()) {
            return;
        }


        Match match = remainingMatches.get(matchIndex);
        Team homeTeam = match.getHomeTeam();
        Team awayTeam = match.getAwayTeam();

        String decrementString = "";

        decrementString = simulateOutcome(match, homeTeam, awayTeam, 3, 0, matchIndex);
        simulateOutcomes(matchIndex + 1);
        decrementPoints(decrementString, homeTeam, awayTeam);
        history = history.substring(0, history.length() - 2);

        decrementString = simulateOutcome(match, homeTeam, awayTeam, 1, 1, matchIndex);
        simulateOutcomes(matchIndex + 1);
        decrementPoints(decrementString, homeTeam, awayTeam);
        history = history.substring(0, history.length() - 2);

        decrementString = simulateOutcome(match, homeTeam, awayTeam, 0, 3, matchIndex);
        simulateOutcomes(matchIndex + 1);
        decrementPoints(decrementString, homeTeam, awayTeam);
        history = history.substring(0, history.length() - 2);
        if (allOutcomes.size() == Math.pow(3.0, remainingMatches.size())) {
            writeAllOutcomesToDB();
            System.out.println("done");
            allOutcomes.add("end");
        }
    }
    // actually does the simulation and returns the outcome of the match and increases the wins/losses/ties of the teams
    private String simulateOutcome(Match match, Team homeTeam, Team awayTeam, int homeScore, int awayScore, int matchIndex) {
        String output = "";

        if (homeScore > awayScore) {
            homeTeam.incrementWins();
            awayTeam.incrementLosses();
            output = "W";
        } else if (homeScore < awayScore) {
            homeTeam.incrementLosses();
            awayTeam.incrementWins();
            output = "L";
        } else {
            homeTeam.incrementTies();
            awayTeam.incrementTies();
            output = "T";
        }

        homeTeam.calculatePoints();
        awayTeam.calculatePoints();

        sortTeams();

        if (matchIndex == 0) {
            history = output + ",";
        }
        else {
            history += output + ",";
        }

        if (matchIndex == remainingMatches.size() - 1) {
            List<Team> outcome = new ArrayList<>();
            for (Team team : teams) {
                outcome.add(new Team(team));
            }
            String[] sat = new String(history).split(",");
            String element = "STANDINGS:\nEastern Conference\n";
            for (int a = 0; a < outcome.size(); a++) {
                if (a == 15) {
                    element += "\nWESTERN CONFERENCE:\n";
                }
                element += "\tSeed:" + outcome.get(a).getSeed() + " - " + outcome.get(a).getName() + " - Points: " + outcome.get(a).getPoints() + " (" + outcome.get(a).getWins() + "-" + outcome.get(a).getTies() + "-" + outcome.get(a).getLosses() + ") GD: " + outcome.get(a).getGoalDifference() + "\n";
            }
            element += "\nSCHEDULE:\n";
            for (int a  = 0; a < remainingMatches.size(); a++) {
                element += "\t" + remainingMatches.get(a).getHomeTeam().getName() + " vs " + remainingMatches.get(a).getAwayTeam().getName() + "(" + remainingMatches.get(a).getDate() + ")" + " - " + sat[a] + "\n";
            }
            allOutcomes.add(element);
            //checkSatisfactoryOutcomes();
        }
        return output;
    }
    // checks to see if an outcome has the target team in the target seed or above depending on user input
    private void checkSatisfactoryOutcomes() {
        Team target = new Team("", 0, 0, 0, 0, 0, 0);
        for (Team team: teams) {
            if (team.getName().equals(targetTeamName)) {
                target = team;
                break;
            }
        }
        String conference = "";
        if (target.getConference() == 0) {
            conference = "East";
        }
        else {
            conference = "West";
        }
        for (int a = 0; a < allOutcomes.size(); a++) {
            String segment = "";
            if (conference.equals("East")) {
                segment = allOutcomes.get(a).substring(0, allOutcomes.get(a).indexOf("WESTERN CONFERENCE:"));
            }
            else if (conference.equals("West")) {
                segment = allOutcomes.get(a).substring(allOutcomes.get(a).indexOf("WESTERN CONFERENCE:"));
            }
            if (greaterThanAndEqualTo) {
                if (segment.indexOf(targetTeamName) <= segment.indexOf("Seed:" + (targetSeed+1))) {
                    satisfactoryOutcomes.add(allOutcomes.get(a));
                }
            }
            else if (!greaterThanAndEqualTo) {
                if (segment.indexOf(targetTeamName) < segment.indexOf("Seed:" + (targetSeed+1)) && segment.indexOf(targetTeamName) > segment.indexOf("Seed:" + targetSeed)) {
                    satisfactoryOutcomes.add(allOutcomes.get(a));
                }
            }

        }
    }
    // undoes the simulation so that the next round of simulations can be done
    private void decrementPoints (String decrementString, Team homeTeam, Team awayTeam) {
        if (decrementString.equals("W")) {
            homeTeam.decrementWins();
            awayTeam.decrementLosses();
        } else if (decrementString.equals("L")) {
            homeTeam.decrementLosses();
            awayTeam.decrementWins();
        } else {
            homeTeam.decrementTies();
            awayTeam.decrementTies();
        }
        homeTeam.calculatePoints();
        awayTeam.calculatePoints();

    }
    // sorts the teams by conference, then by points(highest to lowest), then by wins, then by goal difference (in that order)
    private void sortTeams() {
        List east = new ArrayList<Team>();
        List west = new ArrayList<Team>();
        for (Team team : teams) {
            if (team.getConference() == 0) {
                east.add(team);
            }
            else {
                west.add(team);
            }
        }
        // sort the teams by points(highest to lowest), then by goal difference, then by wins (in that order) for east and west seperately and efficiently
        Comparator<Team> byPoints = Comparator.comparing(Team::getPoints);
        Comparator<Team> byGoalDifference = Comparator.comparing(Team::getGoalDifference);
        Comparator<Team> byWins = Comparator.comparing(Team::getWins);
        Collections.sort(east, byPoints.thenComparing(byGoalDifference).thenComparing(byWins));
        Collections.sort(west, byPoints.thenComparing(byGoalDifference).thenComparing(byWins));

        // set the seeds for the teams
        int seed = east.size();
        for (Object team : east) {
            ((Team)team).setSeed(seed);
            seed--;
        }
        seed = west.size();
        for (Object team : west) {
            ((Team)team).setSeed(seed);
            seed--;
        }

        Comparator<Team> bySeed = Comparator.comparing(Team::getSeed);
        Collections.sort(east, bySeed);
        Collections.sort(west, bySeed);

        // add the teams back into the teams list
        teams.clear();
        teams.addAll(east);
        teams.addAll(west);
    }
    @GetMapping("/returnsatisfactoryoutcomes")// Done
    public String returnSatisfacotyOutcomes(@RequestParam String teamname, @RequestParam int seed, @RequestParam boolean greaterthanorequalto) {
        setTargetSeed(seed);
        setTargetTeam(teamname);
        setGreaterThanAndEqualTo(greaterthanorequalto);
        readAllOutcomesFromDB();
        readTeams();
        checkSatisfactoryOutcomes();
        String output = "";
        for (int a = 0; a < satisfactoryOutcomes.size(); a++) {
            output += satisfactoryOutcomes.get(a) + "------------------------------------------------------------------------------------------------------------\n";
        }
        output += "There were " + satisfactoryOutcomes.size() + " satisfactory outcomes\n";
        return output;
    }
    // checks to see if the team the user inputted is a valid team (may not be needed, but there in case)
    private boolean isAValidTeamInput(String teamName) {
        for (Team team : teams) {
            if (team.getName().equals(teamName)) {
                return true;
            }
        }
        return false;
    }
    private void setTargetTeam(String teamName) {
        teamName = teamName.substring(0, 1).toUpperCase() + teamName.substring(1).toLowerCase();
        targetTeamName = teamName;
    }
    private boolean isAValidSeedInput(int seed) {
        int conference = -1;
        for (Team team : teams) {
            if (team.getName().equals(targetTeamName)) {
                conference = team.getConference();
            }
        }
        if (conference == 0) {
            if (seed <= 15) {
                return true;
            }
        }
        else if (conference == 1) {
            if (seed <= 14) {
                return true;
            }
        }
        return false;
    }
    private void setTargetSeed(int seed) {
        targetSeed = seed;
    }
    private void setGreaterThanAndEqualTo(boolean greaterThanAndEqualTo) {
        this.greaterThanAndEqualTo = greaterThanAndEqualTo;
    }
    private String returnALlOutcomes() {
        String output = "";
        for (int a = 0; a < allOutcomes.size(); a++) {
            output += "Outcome #" + (a + 1) + "\n";
            output += allOutcomes.get(a) + "------------------------------------------------------------------------------------------------------------\n";
        }
        return output;
    }
    private String filter(int[] gameNums, char[] results) {
        filteredOutcomes.clear();
        String output = "";
        for (int b = 0; b < gameNums.length; b++) {
            if (b == 0) {
                for (int a = 0; a < satisfactoryOutcomes.size(); a++) {
                    String outcome = satisfactoryOutcomes.get(a).substring(satisfactoryOutcomes.get(a).indexOf("SCHEDULE:\n") + 10);
                    String[] split = outcome.split("\n");
                    String[] split2 = split[gameNums[b]].split(" - ");
                    if (split2[1].charAt(0) == results[b]) {
                        filteredOutcomes.add(satisfactoryOutcomes.get(a) + "\n");
                    }
                }
            }
            else {
                List<String> temp = new ArrayList<>();
                for (int a = 0; a < filteredOutcomes.size(); a++) {
                    temp.add(filteredOutcomes.get(a) + "\n");
                }
                filteredOutcomes.clear();

                for (int a = 0; a < temp.size(); a++) {
                    String outcome = temp.get(a).substring(temp.get(a).indexOf("SCHEDULE:\n") + 10);
                    String[] split = outcome.split("\n");
                    String[] split2 = split[gameNums[b]].split(" - ");
                    if (split2[1].charAt(0) == results[b]) {
                        filteredOutcomes.add(temp.get(a));
                    }
                }
            }
        }
        for (int a = 0; a < filteredOutcomes.size(); a++) {
            output += filteredOutcomes.get(a);
        }
        output += filteredOutcomes.size() + "/" + satisfactoryOutcomes.size() + " outcomes remain\n";
        return output;
    }
    // main method
    public static void main(String[] args) throws IOException {
        MLSSimulator test = new MLSSimulator();
        //test.run();
    }
}

