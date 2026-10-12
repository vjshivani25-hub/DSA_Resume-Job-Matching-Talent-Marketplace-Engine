import service.ApiServer;
import service.DataStore;

public class Main {
    public static void main(String[] args) {
        try {
            new ApiServer(new DataStore()).start(8080);
        } catch (Exception e) {
            System.err.println("Could not start TalentMatch: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
