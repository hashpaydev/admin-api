package hashpay.v1;


import abek.exceptions.BadRequest;
import abek.exceptions.HttpError;
import abek.exceptions.NotFound;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.UserRecord;
import com.google.gson.JsonObject;

import hashpay.v1.auth.AuthEndpoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import static com.google.firebase.cloud.FirestoreClient.getFirestore;

public class DeleteDomain extends AuthEndpoint {
    private static final Logger logger = Logger.getLogger(DeleteDomain.class.getName());

    @Override
    protected Map<String, Object> process(UserRecord user, HttpServletRequest request, HttpServletResponse response)
            throws HttpError {

        Map<String, Object> result = new HashMap<>();

        try {
            logger.info("Initializing Firestore connection");
            Firestore db = getFirestore();
            if (db == null) {
                logger.severe("Firestore initialization failed");
                throw new BadRequest("Failed to initialize Firestore");
            }

            String domain = request.getParameter("domain");
            logger.info("Request parameter: domain=" + domain);

            if (domain == null || domain.isEmpty()) {
                throw new BadRequest("Missing required parameter: domain");
            }

            domain = domain.trim().toLowerCase();
            logger.info("Processing deletion for domain: " + domain);

            DocumentReference docRef = db.collection("domains").document(domain);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();

            if (!document.exists()) {
                throw new NotFound("Domain not found: " + domain);
            }

            logger.info("Deleting domain from Firestore");
            docRef.delete().get();
            logger.info("Domain deleted successfully");

            result.put("status", "success");
            result.put("message", "Domain deleted successfully");
            result.put("domain", domain);

            return result;

        } catch (BadRequest e) {
            logger.warning("Bad request: " + e.getMessage());
            result.put("status", "error");
            result.put("code", 400);
            result.put("message", e.getMessage());
            return result;
        } catch (NotFound e) {
            logger.warning("Not found: " + e.getMessage());
            result.put("status", "error");
            result.put("code", 404);
            result.put("message", e.getMessage());
            return result;
        } catch (InterruptedException e) {
            logger.severe("Operation interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            result.put("status", "error");
            result.put("code", 500);
            result.put("message", "Operation interrupted: " + e.getMessage());
            return result;
        } catch (ExecutionException e) {
            logger.severe("Execution error: " + e.getMessage());
            result.put("status", "error");
            result.put("code", 500);
            result.put("message", "Failed to delete domain: " + e.getCause().getMessage());
            return result;
        } catch (Exception e) {
            logger.severe("Unexpected error: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            result.put("status", "error");
            result.put("code", 500);
            result.put("message", "Internal server error");
            return result;
        }
    }
}