import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SleeperApi {
    private final HttpClient client;

    public SleeperApi() {
        this.client = HttpClient.newHttpClient();
    }

    public String getMatchups(String leagueId, int week) {
        String url = "https://api.sleeper.app/v1/league/" + leagueId + "/matchups/" + week;
        return sendGetRequest(url, "Could not retrieve matchup data.");
    }

    public String getUsers(String leagueId) {
        String url = "https://api.sleeper.app/v1/league/" + leagueId + "/users";
        return sendGetRequest(url, "Could not retrieve users.");
    }

    public String getRosters(String leagueId) {
        String url = "https://api.sleeper.app/v1/league/" + leagueId + "/rosters";
        return sendGetRequest(url, "Could not retrieve rosters.");
    }

    private String sendGetRequest(String url, String errorMessage) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.body();

        } catch (Exception e) {
            return "{ \"error\": \"" + errorMessage + "\" }";
        }
    }
}