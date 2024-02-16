package org.example;

import java.util.ArrayList;

public class Player {
    private String name;
    private ArrayList<Membership> memberships = new ArrayList<>();

    public Player(String name) {
        this.name = name;
    }

    public void addMembership(Membership membership) {
        for (Membership m : memberships) {
            if (m.hasOverlap(membership)) {
                throw new IllegalArgumentException("Overlapping memberships");
            }
        }
        memberships.add(membership);
    }
}
