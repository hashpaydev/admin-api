package hashpay.v1.auth;

import abek.endpoint.PublicEndpoint;
import abek.exceptions.BadRequest;
import abek.exceptions.HttpError;
import abek.util.HTTP;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.gson.Gson;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static com.google.firebase.cloud.FirestoreClient.getFirestore;


public class Register extends PublicEndpoint {
    private static final Gson gson = new Gson();
    private static final String FIREBASE_API_KEY = "AIzaSyDii8mqfU_rNQ9oMDs5bpCacniGvpgywno";

    @Override
    protected Map<String, Object> process(HttpServletRequest request, HttpServletResponse response) throws HttpError {
        try {
            String email = getRequiredParameter(request, "email");
            String password = getRequiredParameter(request, "password");

            UserRecord.CreateRequest newUser = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(password);

            UserRecord userRecord = FirebaseAuth.getInstance().createUser(newUser);

            Firestore db = getFirestore();
            DocumentReference userRef = db.collection("sellers").document(userRecord.getUid());
            Map<String, Object> userData = new HashMap<>();
            userData.put("email", email);
            userData.put("createdAt", System.currentTimeMillis());

            userRef.set(userData, SetOptions.merge());

            String idToken = authenticateWithEmailAndPassword(email, password);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("status", "success");
            responseData.put("userId", userRecord.getUid());
            responseData.put("email", email);
            responseData.put("bearerToken", idToken);
            responseData.put("message", "User registered successfully.");

            return responseData;

        } catch (FirebaseAuthException e) {
            throw new BadRequest("AUTH004", "Registration failed: " + e.getMessage());
        } catch (IOException e) {
            throw new BadRequest("AUTH005", "Registration failed: " + e.getMessage());
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
            return null;
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