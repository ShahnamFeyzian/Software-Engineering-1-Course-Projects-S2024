import csv
import jdatetime
import datetime

def shamsi_to_gregorian(shamsi_date):
    year, month, day = map(int, shamsi_date.split("-"))
    gregorian_date = jdatetime.JalaliToGregorian(year, month, day).getGregorianList()
    return datetime.date(gregorian_date[0], gregorian_date[1], gregorian_date[2])


def calculate_days(start, end):
    start_date = shamsi_to_gregorian(start)
    end_date = shamsi_to_gregorian(end)
    return (end_date - start_date).days + 1


with open("data/players.csv", "r") as file:
    reader = csv.reader(file)
    next(reader)  # Skip header row
    data = list(reader)

membership = {}
for row in data:
    player, team, startday, startmonth, startyear, endday, endmonth, endyear = row
    start_date = f"{startyear}-{startmonth}-{startday}"
    end_date = f"{endyear}-{endmonth}-{endday}"
    days = calculate_days(start_date, end_date)
    if player not in membership:
        membership[player] = {}
    if team not in membership[player]:
        membership[player][team] = 0
    membership[player][team] += days

for player, teams in membership.items():
    print(f"Player: {player}")
    for team, days in teams.items():
        print(f"  Team: {team} - Total Membership Days: {days}")
    print("--------------------\n")
