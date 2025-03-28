package hashpay.v1.auth;

import abek.exceptions.HttpError;
import abek.util.HTTP;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class AuthEndpoint extends HttpServlet {
    protected Firestore db;
    protected FirebaseAuth fireAuth;

    public void init() throws ServletException {
        try {
            this.db = FirestoreClient.getFirestore();
        } catch (Exception var3) {
            var3.printStackTrace();
        }

        try {
            this.fireAuth = FirebaseAuth.getInstance();
        } catch (Exception var2) {
            var2.printStackTrace();
        }

        this.startup();
    }

    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String origin = request.getHeader("origin");
        if (origin == null || "".equals(origin)) {
            origin = "*";
        }

        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"));
        response.setHeader("Access-Control-Max-Age", "600");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String origin = request.getHeader("origin");
        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Credentials", "true");

        try {
            // Try to get token from "token" header first
            String token = request.getHeader("token");

            // If not found, check for Authorization header with Bearer token
            if (token == null || token.isEmpty()) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7); // Remove "Bearer " prefix
                }
            }

            // Validate that we have a token
            if (token == null || token.isEmpty()) {
                throw new IllegalArgumentException("ID token must not be null or empty");
            }

            FirebaseToken firebaseToken = this.fireAuth.verifyIdToken(token);
            String uid = firebaseToken.getUid();
            UserRecord user = this.fireAuth.getUser(uid);
            Map<String, Object> map = this.process(user, request, response);
            if (map != null) {
                HTTP.sendJSON(response, map);
            }
        } catch (HttpError var9) {
            HTTP.sendERROR(response, var9.code, var9.type, var9.getMessage());
        } catch (FirebaseAuthException var10) {
            HTTP.sendERROR(response, 401, "Unauthorized", var10.getMessage());
        } catch (IllegalArgumentException var11) {
            HTTP.sendERROR(response, 401, "Unauthorized", var11.getMessage());
        } catch (Exception var12) {
            HTTP.sendERROR(response, 500, var12.getClass().getSimpleName(), var12.getMessage());
        }
    }

    protected void startup() {
    }

    protected Map<String, Object> process(UserRecord user, HttpServletRequest request, HttpServletResponse response) throws HttpError {
        return null;
    }
}