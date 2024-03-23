package org.example;

public class Membership {

	private String teamName;
	private Date startDate;
	private Date endDate;

	public Membership(String teamName, Date startDate, Date endDate) {
		if (startDate.compareTo(endDate) > 0) {
			throw new IllegalArgumentException(
				"Start date must be before end date"
			);
		}
		this.teamName = teamName;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public boolean hasOverlap(Membership other) {
		return (
			(this.endDate.compareTo(other.startDate) >= 0) &&
			(this.startDate.compareTo(other.endDate) <= 0)
		);
	}

	public int getMembershipDays() {
		return startDate.daysBetween(endDate);
	}

	public String getTeamName() {
		return teamName;
	}
}
