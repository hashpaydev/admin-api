package hashpay.v1.auth;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.google.gson.Gson;
import abek.endpoint.PublicEndpoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import abek.exceptions.BadRequest;
import abek.exceptions.HttpError;

public class Login extends PublicEndpoint {
	private static final long serialVersionUID = 1L;
    private static final Gson gson = new Gson();
    private static final String FIREBASE_API_KEY = "AIzaSyDKMJiV-a1dandXcqTJTGofwUvZMsw1V9I";

    @Override
    protected Map<String, Object> process(HttpServletRequest request, HttpServletResponse response) throws HttpError {
        try {
            String email = getRequiredParameter(request, "email");
            String password = getRequiredParameter(request, "password");

            String idToken = authenticateWithEmailAndPassword(email, password);
            if (idToken == null) {
                throw new BadRequest("AUTH003", "Invalid email or password.");
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("status", "success");
            responseData.put("email", email);
            responseData.put("bearerToken", idToken);
            responseData.put("message", "Login successful.");

            return responseData;

        } catch (IOException e) {
            throw new BadRequest("AUTH002", "Authentication failed: " + e.getMessage());
        }
    }

    private String authenticateWithEmailAndPassword(String email, String password) throws IOException {
        String firebaseAuthUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + FIREBASE_API_KEY;

        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", password);
        credentials.put("returnSecureToken", "true");

        String jsonInputString = gson.toJson(credentials);

        URL url = new URL(firebaseAuthUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (Scanner scanner = new Scanner(connection.getInputStream(), "utf-8")) {
                String responseBody = scanner.useDelimiter("\\A").next();
                Map<String, Object> responseMap = gson.fromJson(responseBody, Map.class);
                return (String) responseMap.get("idToken");
            }
        } else {
            try (Scanner scanner = new Scanner(connection.getErrorStream(), "utf-8")) {
                if (scanner.hasNext()) {
                    String errorBody = scanner.useDelimiter("\\A").next();
                    System.err.println("Firebase auth error: " + errorBody);

                    // Parse the error for better handling
                    Map<String, Object> errorMap = gson.fromJson(errorBody, Map.class);
                    if (errorMap.containsKey("error")) {
                        Map<String, Object> error = (Map<String, Object>) errorMap.get("error");
                        String errorMessage = (String) error.get("message");
                        throw new IOException("Firebase authentication error: " + errorMessage);
                    }
                }
            }
            throw new IOException("Failed to authenticate with Firebase");
        }
    }

    private String getRequiredParameter(HttpServletRequest request, String param) throws BadRequest {
        String value = request.getParameter(param);
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequest(param.toUpperCase() + "001", param + " parameter is required");
        }
        return value.trim();
    }
}