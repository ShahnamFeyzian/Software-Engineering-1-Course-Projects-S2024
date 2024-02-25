package org.example;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class Main {
    
    private static final String filePath = "src/main/resources/data/players.csv";
    
    public static void main(String[] args) {
        Date date = new Date(18, 11, 1402);
        Date nextDay = date.nextDay();
        System.out.println("Given Date: " + date + ", Next Date: " + nextDay);

        Map<String, Player> players = readPlayersFromCSV(filePath);
        try (
            PrintWriter writer = new PrintWriter(
                new FileWriter("src/main/resources/data/result.ansi", false)
            )
        ) {
            for (Player player : players.values()) {
                writer.println(
                    "\u001B[34mPlayer: " + player.getName() + "\u001B[0m"
                );

                Map<String, Integer> daysAsMemberByTeam = player.getDaysAsMemberByTeam();

                for (Map.Entry<String, Integer> entry : daysAsMemberByTeam.entrySet()) {
                    String teamName = entry.getKey();
                    int totalMembershipDays = entry.getValue();

                    writer.println(
                        "\u001B[32m  Team: " +
                        teamName +
                        " - Total Membership Days: " +
                        totalMembershipDays +
                        "\u001B[0m"
                    );
                    writer.println("\u001B[35m--------------------\u001B[0m");
                }

                writer.println("\u001B[36m\n*********************\n\u001B[0m");
            }
            System.out.println("Result written to result.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Player> readPlayersFromCSV(String filePath) {
        Map<String, Player> playerMap = new HashMap<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            reader.readNext(); // skip first line
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                String playerName = nextLine[0];
                Membership membership = getMembership(nextLine);

                if (playerMap.containsKey(playerName)) {
                    Player existingPlayer = playerMap.get(playerName);
                    existingPlayer.addMembership(membership);
                } else {
                    Player newPlayer = new Player(playerName);
                    newPlayer.addMembership(membership);
                    playerMap.put(playerName, newPlayer);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CsvValidationException e) {
            e.printStackTrace();
        }

        return playerMap;
    }

    private static Membership getMembership(String[] nextLine) {
        String teamName = nextLine[1];
        int startDay = Integer.parseInt(nextLine[2]);
        int startMonth = Integer.parseInt(nextLine[3]);
        int startYear = Integer.parseInt(nextLine[4]);
        Date start = new Date(startDay, startMonth, startYear);
        int endDay = Integer.parseInt(nextLine[5]);
        int endMonth = Integer.parseInt(nextLine[6]);
        int endYear = Integer.parseInt(nextLine[7]);
        Date end = new Date(endDay, endMonth, endYear);

        Membership membership = new Membership(teamName, start, end);
        return membership;
    }
}
