package org.example;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

public class Player {

    private String name;
    private ArrayList<Membership> memberships = new ArrayList<>();

    public Player(String name) {
        this.name = name;
    }
    
    public Player(String name, ArrayList<Membership> memberships) {
        this.name = name;
        this.memberships = memberships;
    }

    public void addMembership(Membership membership) {
        for (Membership m : memberships) {
            if (m.hasOverlap(membership)) {
                throw new IllegalArgumentException("Overlapping memberships");
            }
        }
        memberships.add(membership);
    }

    public Map<String, Integer> getDaysAsMemberByTeam() {
        return memberships
            .stream()
            .collect(
                Collectors.groupingBy(
                    Membership::getTeamName,
                    Collectors.summingInt(Membership::getMembershipDays)
                )
            );
    }

    public int getDaysAsMemberByTeam(String teamName) {
        return getDaysAsMemberByTeam().get(teamName);
    }

    public String getName() {
        return name;
    }
}
