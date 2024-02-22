package self;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Main {
    private static final String dataPath = "src/main/resources/data.csv";
    private static final String targetPlayerName = "Gholam";
    private static final String targetTeamName = "Golgohar";

    public static void main(String[] args) throws IOException, CsvException, Exception {
        Date date = new Date(18, 11, 1402);
        System.out.println(date.nextDay());

        List<String[]> data = readData();
        ArrayList<Player> players = createPlayers(data);

        players.forEach(p -> {
            if(p.isName(targetPlayerName)) {
                String message = targetPlayerName + " played " +
                        p.totalMembershipDays(targetTeamName) +
                        " days in " + targetTeamName;
                System.out.println(message);
            }
        });
    }

    private static List<String[]> readData() throws Exception {
        FileReader filereader = new FileReader(dataPath);
        CSVReader csvReader = new CSVReaderBuilder(filereader).build();
        return csvReader.readAll();
    }

    private static ArrayList<Player> createPlayers(List<String[]> data) throws Exception {
        ArrayList<Player> players = new ArrayList<Player>();
        for (String[] record : data) {
            Iterator<Player> plIt = players.iterator();
            boolean duplicateFlag = false;
            while (plIt.hasNext()) {
                Player player = plIt.next();
                if (player.isName(record[0])) {
                    duplicateFlag = true;
                    updatePlayer(player, record);
                    break;
                }
            }
            if (!duplicateFlag)
                players.add(newPlayer(record));
        }
        return players;
    }

    private static Player newPlayer(String[] record) throws Exception {
        Player player = new Player(record[0], new ArrayList<Membership>());
        updatePlayer(player, record);
        return player;
    }

    private static void updatePlayer(Player pl, String[] record) throws Exception {
        Date start = new Date(
                Integer.parseInt(record[2]),
                Integer.parseInt(record[3]),
                Integer.parseInt(record[4])
        );
        Date end = new Date(
                Integer.parseInt(record[5]),
                Integer.parseInt(record[6]),
                Integer.parseInt(record[7])
        );
        Membership mem = new Membership(record[1], start, end);
        pl.addMembership(mem);
    }
}
