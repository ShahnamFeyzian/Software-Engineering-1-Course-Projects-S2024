package self;

import java.util.ArrayList;

public class Player {
    private final String name;
    private final ArrayList<Membership> memberships;

    public Player(String name, ArrayList<Membership> memberships) {
        this.name = name;
        this.memberships = memberships;
    }

    public void addMembership(Membership newMembership) throws Exception {
        for(Membership mem: this.memberships) {
            if(mem.hasOverlap(newMembership))
                throw new Exception("has overlap with another membership");
        }
        memberships.add(newMembership);
    }

    public int totalMembershipDays(String teamName) {
        return memberships.stream().filter(m -> m.isName(teamName)).mapToInt(m -> m.membershipDays()).sum();
    }

    public boolean isName(String name) {
        return this.name.equals(name);
    }
}
