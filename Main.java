import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        System.out.println("Fantasy Football Weekly Score Tracker");

        System.out.print("Enter Sleeper League ID: ");
        String leagueId = input.nextLine();

        System.out.print("Enter week number: ");
        int week = input.nextInt();

        TrackerService tracker = new TrackerService();
        String result = tracker.getReadableScoreboard(leagueId, week);

        System.out.println("\nWeekly Scoreboard:");
        System.out.println(result);

        input.close();
    }
}