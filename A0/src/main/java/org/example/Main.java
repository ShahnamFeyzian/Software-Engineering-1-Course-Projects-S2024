package org.example;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        Date date = new Date(18, 11, 1402);
        Date nextDay = date.nextDay();
        System.out.println("Given Date: " + date + ", Next Date: " + nextDay);


        String filePath = "src/main/resources/data/players.csv";
        Map<String, Player> players = readPlayersFromCSV(filePath);
        for (Player player : players.values()) {
            System.out.println("Player: " + player.getName());
            Map<String, Integer> daysAsMemberByTeam = player.getDaysAsMemberByTeam();

            for (Map.Entry<String, Integer> entry : daysAsMemberByTeam.entrySet()) {
                String teamName = entry.getKey();
                int totalMembershipDays = entry.getValue();

                System.out.println("  Team: " + teamName + " - Total Membership Days: " + totalMembershipDays);
                System.out.println("--------------------");
            }

            System.out.println("\n*********************\n");
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
