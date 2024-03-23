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

	private static final String filePath =
		"src/main/resources/data/players.csv";
	private static final String resultFilePath =
		"src/main/resources/data/result.txt";

	private static Map<String, String> colors = new HashMap<>();

	public Main() {
		colors.put("blue", "\u001B[34m");
		colors.put("green", "\u001B[32m");
		colors.put("purple", "\u001B[35m");
		colors.put("cyan", "\u001B[36m");
		colors.put("reset", "\u001B[0m");
	}

	public static void main(String[] args) {
		Date date = new Date(18, 11, 1402);
		Date nextDay = date.nextDay();
		System.out.println("Given Date: " + date + ", Next Date: " + nextDay);

		Map<String, Player> players = readPlayersFromCSV(filePath);
		try (
			PrintWriter writer = new PrintWriter(
				new FileWriter(resultFilePath, false)
			)
		) {
			for (Player player : players.values()) {
				writer.println(
					colors.get("blue") +
					"Player: " +
					player.getName() +
					colors.get("reset")
				);

				Map<String, Integer> daysAsMemberByTeam = player.getDaysAsMemberByTeam();

				for (Map.Entry<String, Integer> entry : daysAsMemberByTeam.entrySet()) {
					String teamName = entry.getKey();
					int totalMembershipDays = entry.getValue();

					writer.println(
                        colors.get("green") +
						teamName +
						" - Total Membership Days: " +
						totalMembershipDays +
                        colors.get("reset")
					);

                    writer.println(
                        colors.get("purple") +
                        "--------------------" +
                        colors.get("reset")
                    );
				}

                writer.println(
                    colors.get("cyan") +
                    "*********************" +
                    colors.get("reset")
                );
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
		} catch (IOException | CsvValidationException e) {
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
