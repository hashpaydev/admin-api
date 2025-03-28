package hashpay.v1;

import abek.exceptions.BadRequest;
import abek.exceptions.HttpError;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.firebase.auth.UserRecord;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import hashpay.v1.auth.AuthEndpoint;

import static com.google.firebase.cloud.FirestoreClient.getFirestore;
import static com.google.firebase.cloud.StorageClient.getInstance;

public class SetDomainIcon extends AuthEndpoint {
    private static final Logger logger = Logger.getLogger(SetDomainIcon.class.getName());
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;
    private static final String BUCKET_NAME = "hashpaytest.firebasestorage.app";
    private static final String[] ALLOWED_CONTENT_TYPES = {
            "image/jpeg", "image/png", "image/gif", "image/svg+xml", "image/webp"
    };

    @Override
    protected Map<String, Object> process(UserRecord user, HttpServletRequest request, HttpServletResponse response)
            throws HttpError {

        Map<String, Object> result = new HashMap<>();

        try {
            Firestore db = getFirestore();
            if (db == null) {
                logger.severe("Firestore initialization failed");
                throw new BadRequest("Failed to initialize Firestore");
            }

            Storage storage = getInstance().bucket(BUCKET_NAME).getStorage();
            if (storage == null) {
                logger.severe("Firebase Storage initialization failed");
                throw new BadRequest("Failed to initialize Firebase Storage");
            }

            String domain = request.getParameter("domain");
            if (domain == null || domain.trim().isEmpty()) {
                throw new BadRequest("Missing required parameter: domain");
            }

            domain = domain.trim().toLowerCase();

            if (!domain.contains(".") || domain.length() > 253) {
                throw new BadRequest("Invalid domain format");
            }

            DocumentReference docRef = db.collection("domains").document(domain);
            DocumentSnapshot document = docRef.get().get();

            if (!document.exists()) {
                throw new BadRequest("Domain does not exist. Please create domain first.");
            }

            try {
                request.setCharacterEncoding("UTF-8");
                Part filePart = request.getPart("icon");

                if (filePart == null) {
                    throw new BadRequest("No icon file uploaded");
                }

                if (filePart.getSize() > MAX_FILE_SIZE) {
                    throw new BadRequest("File size exceeds the maximum limit of 2MB");
                }

                String contentType = filePart.getContentType();
                boolean validContentType = false;
                for (String allowedType : ALLOWED_CONTENT_TYPES) {
                    if (allowedType.equals(contentType)) {
                        validContentType = true;
                        break;
                    }
                }

                if (!validContentType) {
                    throw new BadRequest("Invalid file type. Allowed types: JPEG, PNG, GIF, SVG, WebP");
                }

                byte[] fileContent = readAllBytes(filePart.getInputStream());

                String fileExtension = getFileExtension(contentType);
                String fileName = domain + "_" + UUID.randomUUID().toString() + fileExtension;
                String objectPath = "domains/" + domain + "/" + fileName;

                logger.info("Uploading file to Firebase Storage: " + objectPath);
                BlobId blobId = BlobId.of(BUCKET_NAME, objectPath);
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                        .setContentType(contentType)
                        .build();
                Blob blob = storage.create(blobInfo, fileContent);
                String iconUrl = String.format("https://storage.googleapis.com/%s/%s", BUCKET_NAME, objectPath);
                logger.info("File uploaded successfully: " + iconUrl);

                if (document.contains("iconUrl")) {
                    String oldIconUrl = document.getString("iconUrl");
                    if (oldIconUrl != null && !oldIconUrl.isEmpty()) {
                        String oldPath = oldIconUrl.replace("https://storage.googleapis.com/" + BUCKET_NAME + "/", "");
                        BlobId oldBlobId = BlobId.of(BUCKET_NAME, oldPath);
                        boolean deleted = storage.delete(oldBlobId);
                        logger.info("Old icon deletion status: " + deleted);
                    }
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("iconUrl", iconUrl);
                updates.put("updatedAt", System.currentTimeMillis());

                logger.info("Updating domain with icon URL in Firestore");
                docRef.update(updates).get();
                logger.info("Domain updated with icon URL");

                result.put("status", "success");
                result.put("message", "Domain icon uploaded successfully");
                result.put("iconUrl", iconUrl);
                result.put("domain", domain);

                return result;

            } catch (ServletException e) {
                logger.severe("ServletException: " + e.getMessage());
                throw new BadRequest("Error processing file upload: " + e.getMessage());
            }

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
            result.put("message", "Failed to process domain icon: " + e.getCause().getMessage());
            return result;
        } catch (Exception e) {
            logger.severe("Unexpected error: " + e.getClass().getName() + ": " + e.getMessage());
            result.put("status", "error");
            result.put("code", 500);
            result.put("message", "Unexpected error: " + e.getMessage());
            return result;
        }
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    private String getFileExtension(String contentType) {
        switch (contentType) {
            case "image/jpeg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "image/gif":
                return ".gif";
            case "image/svg+xml":
                return ".svg";
            case "image/webp":
                return ".webp";
            default:
                return "";
        }
    }
}