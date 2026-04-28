package Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import ACLCapp.AppSettings;
import ACLCapp.DBConnection;
import ACLCapp.SimilarityUtil;

public class Web {
    public String title;
    public double score;
    public String source;

    public Web(String title, double score, String source) {
        this.title = title;
        this.score = score;
        this.source = source;
    }

    // Your search method (unchanged)
    public static List<TimeLimit> search(String query) {
        // ... same as before ...
        return new ArrayList<>();
    }

    // The update method
    public static void updateOnlineBin() {
        if (!DBConnection.isInternetAvailable()) {
            System.err.println("No internet – can't update online bin.");
            return;
        }

        String binUrl = DBConnection.getProperty("jsonblob.api.url");

        try {
            // Build the path to research.json (same folder as SQLite DB)
            String appFolder = System.getenv("LOCALAPPDATA");
            if (appFolder == null) {
                appFolder = System.getProperty("user.home");
            }
            appFolder += java.io.File.separator + "ACLC Research Title";
            String filePath = appFolder + java.io.File.separator + "research.json";

            // Read the file
            java.io.File jsonFile = new java.io.File(filePath);
            if (!jsonFile.exists()) {
                System.err.println("research.json not found at: " + filePath);
                return;
            }

            String jsonString = new String(java.nio.file.Files.readAllBytes(jsonFile.toPath()), "UTF-8");
            System.out.println("=== FULL JSON BEING SENT (from file) ===");
            System.out.println(jsonString);
            System.out.println("=========================================");

            URL url = new URL(binUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            OutputStream os = conn.getOutputStream();
            os.write(jsonString.getBytes("UTF-8"));
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            System.out.println("HTTP Response Code: " + responseCode);

            if (responseCode >= 200 && responseCode < 300) {
                System.out.println("✅ Online bin updated successfully!");
            } else {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuilder errorMsg = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorMsg.append(line);
                }
                errorReader.close();
                System.err.println("❌ Update failed. Server response:");
                System.err.println(errorMsg.toString());
            }

        } catch (Exception e) {
            System.err.println("Web update exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}