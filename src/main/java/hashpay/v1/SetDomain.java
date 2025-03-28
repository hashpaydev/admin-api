package hashpay.v1;

import abek.exceptions.BadRequest;
import abek.exceptions.HttpError;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.UserRecord;
import abek.endpoint.AuthEndpoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import static com.google.firebase.cloud.FirestoreClient.getFirestore;

public class SetDomain extends AuthEndpoint {
    private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(SetDomain.class.getName());

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
            logger.info("Firestore connection established");

            String domain = request.getParameter("domain");
            String webhook = request.getParameter("webhook"); // Optional
            String color = request.getParameter("color");
            String emailNotificationParam = request.getParameter("emailNotification");
            String icon = request.getParameter("icon"); // Added to handle icon parameter

            logger.info("Request parameters: domain=" + domain + ", webhook=" + webhook +
                    ", color=" + color + ", emailNotification=" + emailNotificationParam +
                    ", icon=" + icon);

            if (domain == null || domain.trim().isEmpty()) {
                throw new BadRequest("Missing required parameter: domain");
            }

            domain = domain.trim().toLowerCase();

            if (!domain.contains(".") || domain.length() > 253) {
                throw new BadRequest("Invalid domain format");
            }

            boolean emailNotification = false;
            if (emailNotificationParam != null) {
                emailNotification = Boolean.parseBoolean(emailNotificationParam);
            }

            logger.info("Checking if domain exists in Firestore");
            DocumentReference docRef = db.collection("domains").document(domain);
            DocumentSnapshot document = docRef.get().get();

            Map<String, Object> domainData = new HashMap<>();
            boolean isUpdate = document.exists();

            if (isUpdate) {
                logger.info("Updating existing domain");

                domainData.put("domain", domain);
                domainData.put("updatedAt", System.currentTimeMillis());

                if (document.contains("createdAt")) {
                    domainData.put("createdAt", document.getLong("createdAt"));
                } else {
                    domainData.put("createdAt", System.currentTimeMillis());
                }

                if (webhook != null && !webhook.trim().isEmpty()) {
                    if (!webhook.startsWith("http://") && !webhook.startsWith("https://")) {
                        throw new BadRequest("Invalid webhook URL format");
                    }
                    domainData.put("webhook", webhook);
                } else if (document.contains("webhook")) {
                    domainData.put("webhook", document.getString("webhook")); // Keep existing webhook
                }

                if (color != null && !color.isEmpty()) {
                    domainData.put("color", color);
                } else if (document.contains("color")) {
                    domainData.put("color", document.getString("color")); // Keep existing color
                }

                if (icon != null && !icon.isEmpty()) {
                    domainData.put("icon", icon);
                } else if (document.contains("icon")) {
                    domainData.put("icon", document.getString("icon")); // Keep existing icon
                }

                if (document.contains("iconUrl")) {
                    domainData.put("iconUrl", document.getString("iconUrl"));
                }


                domainData.put("notify", emailNotification);

            } else {
                logger.info("Creating new domain");
                domainData.put("domain", domain);
                domainData.put("createdAt", System.currentTimeMillis());
                domainData.put("notify", emailNotification); // Using notify to match Domain class
                domainData.put("color", (color != null) ? color : ""); // Default color is empty
                domainData.put("webhook", (webhook != null && webhook.startsWith("http")) ? webhook : ""); // Default webhook empty if not valid
                domainData.put("icon", (icon != null) ? icon : ""); // Default icon is empty
            }

            logger.info("Saving domain data to Firestore");
            docRef.set(domainData).get();
            logger.info("Domain data saved successfully");

            result.put("status", "success");
            result.put("message", isUpdate ? "Domain settings updated successfully" : "Domain created successfully");
            result.put("operation", isUpdate ? "update" : "create");
            result.put("domain", domain);

            return result;

        } catch (BadRequest e) {
            logger.warning("Bad request: " + e.getMessage());
            result.put("status", "error");
            result.put("code", 400);
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
            result.put("message", "Failed to process domain: " + e.getCause().getMessage());
            return result;
        } catch (Exception e) {
            logger.severe("Unexpected error: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            result.put("status", "error");
            result.put("code", 500);
            result.put("message", "Unexpected error: " + e.getMessage());
            return result;
        }
    }
}