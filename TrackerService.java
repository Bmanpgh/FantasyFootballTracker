import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrackerService {
    private final SleeperApi apiClient;

    public TrackerService() {
        this.apiClient = new SleeperApi();
    }

    public String getReadableScoreboard(String leagueId, int week) {
        String usersJson = apiClient.getUsers(leagueId);
        String rostersJson = apiClient.getRosters(leagueId);
        String matchupsJson = apiClient.getMatchups(leagueId, week);

        Map<String, String> userIdToName = getUserNames(usersJson);
        Map<Integer, String> rosterIdToOwnerId = getRosterOwners(rostersJson);
        List<TeamScore> teamScores = getTeamScores(matchupsJson);

        Map<Integer, List<TeamScore>> groupedMatchups = new TreeMap<>();

        for (TeamScore score : teamScores) {
            groupedMatchups
                    .computeIfAbsent(score.matchupId, k -> new ArrayList<>())
                    .add(score);
        }

        StringBuilder scoreboard = new StringBuilder();

        scoreboard.append("League ID: ").append(leagueId).append("\n");
        scoreboard.append("Week: ").append(week).append("\n");
        scoreboard.append("----------------------------------\n");

        for (Map.Entry<Integer, List<TeamScore>> entry : groupedMatchups.entrySet()) {
            List<TeamScore> matchup = entry.getValue();

            if (matchup.size() == 2) {
                TeamScore team1 = matchup.get(0);
                TeamScore team2 = matchup.get(1);

                String team1Name = getTeamName(team1.rosterId, rosterIdToOwnerId, userIdToName);
                String team2Name = getTeamName(team2.rosterId, rosterIdToOwnerId, userIdToName);

                scoreboard.append("Matchup ").append(entry.getKey()).append(":\n");
                scoreboard.append(team1Name).append(" - ").append(team1.points).append("\n");
                scoreboard.append(team2Name).append(" - ").append(team2.points).append("\n");

                if (team1.points > team2.points) {
                    scoreboard.append("Winner: ").append(team1Name).append("\n");
                } else if (team2.points > team1.points) {
                    scoreboard.append("Winner: ").append(team2Name).append("\n");
                } else {
                    scoreboard.append("Result: Tie\n");
                }

                scoreboard.append("----------------------------------\n");
            }
        }

        return scoreboard.toString();
    }

    private String getTeamName(int rosterId, Map<Integer, String> rosterIdToOwnerId, Map<String, String> userIdToName) {
        String ownerId = rosterIdToOwnerId.get(rosterId);

        if (ownerId == null) {
            return "Roster " + rosterId;
        }

        return userIdToName.getOrDefault(ownerId, "Roster " + rosterId);
    }

    private Map<String, String> getUserNames(String usersJson) {
        Map<String, String> userNames = new HashMap<>();

        List<String> objects = splitJsonObjects(usersJson);

        for (String object : objects) {
            String userId = extractString(object, "user_id");
            String displayName = extractString(object, "display_name");
            String username = extractString(object, "username");
            String teamName = extractTeamName(object);

            if (userId != null) {
                if (teamName != null && !teamName.isEmpty()) {
                    userNames.put(userId, teamName);
                } else if (displayName != null && !displayName.isEmpty()) {
                    userNames.put(userId, displayName);
                } else if (username != null && !username.isEmpty()) {
                    userNames.put(userId, username);
                } else {
                    userNames.put(userId, "User " + userId);
                }
            }
        }

        return userNames;
    }

    private Map<Integer, String> getRosterOwners(String rostersJson) {
        Map<Integer, String> rosterOwners = new HashMap<>();

        List<String> objects = splitJsonObjects(rostersJson);

        for (String object : objects) {
            Integer rosterId = extractInteger(object, "roster_id");
            String ownerId = extractString(object, "owner_id");

            if (rosterId != null && ownerId != null) {
                rosterOwners.put(rosterId, ownerId);
            }
        }

        return rosterOwners;
    }

    private List<TeamScore> getTeamScores(String matchupsJson) {
        List<TeamScore> scores = new ArrayList<>();

        List<String> objects = splitJsonObjects(matchupsJson);

        for (String object : objects) {
            Integer rosterId = extractInteger(object, "roster_id");
            Integer matchupId = extractInteger(object, "matchup_id");
            Double points = extractDouble(object, "points");

            if (rosterId != null && matchupId != null && points != null) {
                scores.add(new TeamScore(rosterId, matchupId, points));
            }
        }

        return scores;
    }

    private List<String> splitJsonObjects(String json) {
        List<String> objects = new ArrayList<>();

        int depth = 0;
        int start = -1;
        boolean insideString = false;

        for (int i = 0; i < json.length(); i++) {
            char current = json.charAt(i);

            if (current == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                insideString = !insideString;
            }

            if (!insideString) {
                if (current == '{') {
                    if (depth == 0) {
                        start = i;
                    }
                    depth++;
                } else if (current == '}') {
                    depth--;

                    if (depth == 0 && start != -1) {
                        objects.add(json.substring(start, i + 1));
                        start = -1;
                    }
                }
            }
        }

        return objects;
    }

    private String extractString(String jsonObject, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"(.*?)\"");
        Matcher matcher = pattern.matcher(jsonObject);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private Integer extractInteger(String jsonObject, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(jsonObject);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return null;
    }

    private Double extractDouble(String jsonObject, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+(\\.\\d+)?)");
        Matcher matcher = pattern.matcher(jsonObject);

        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }

        return null;
    }

    private String extractTeamName(String jsonObject) {
        Pattern pattern = Pattern.compile("\"team_name\"\\s*:\\s*\"(.*?)\"");
        Matcher matcher = pattern.matcher(jsonObject);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private static class TeamScore {
        int rosterId;
        int matchupId;
        double points;

        TeamScore(int rosterId, int matchupId, double points) {
            this.rosterId = rosterId;
            this.matchupId = matchupId;
            this.points = points;
        }
    }
}