package hashpay.v1;

import abek.exceptions.BadRequest;
import abek.exceptions.HttpError;
import com.google.cloud.firestore.*;
import com.google.firebase.auth.UserRecord;
import com.google.gson.Gson;

import abek.endpoint.AuthEndpoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.google.firebase.cloud.FirestoreClient.getFirestore;

public class DeleteCurrency extends AuthEndpoint {
    private static final long serialVersionUID = 1L;

    private static final List<String> SUPPORTED_NETWORKS = Arrays.asList("SOL", "ETH", "TRX");
    private static final List<String> SUPPORTED_CURRENCIES = Arrays.asList("USDC", "USDT", "EUR");

    @Override
    protected Map<String, Object> process(UserRecord user, HttpServletRequest request, HttpServletResponse response)
            throws HttpError {

        try {
            Firestore db = getFirestore();
            if (db == null) {
                throw new BadRequest("DB001", "Failed to initialize Firestore");
            }

            String sellerId = user.getUid();

            DocumentReference sellerRef = db.collection("sellers").document(sellerId);
            DocumentSnapshot sellerDoc = sellerRef.get().get();

            if (!sellerDoc.exists()) {
                throw new BadRequest("SELLER001", "Seller not found");
            }

            String network = getRequiredParameter(request, "network").toUpperCase();
            String currency = getRequiredParameter(request, "currency").toUpperCase();

            if (!SUPPORTED_NETWORKS.contains(network)) {
                throw new BadRequest("NET001", "Invalid or unsupported network: " + network);
            }

            if (!SUPPORTED_CURRENCIES.contains(currency)) {
                throw new BadRequest("CUR002", "Unsupported currency: " + currency);
            }

            Map<String, Object> updates = new HashMap<>();

            String currencyNetworkKey = currency + "_" + network;
            updates.put(currencyNetworkKey, FieldValue.delete());

            Map<String, Object> currencyStatusesUpdate = new HashMap<>();
            currencyStatusesUpdate.put(currency, FieldValue.delete());
            updates.put("currencyStatuses." + network, currencyStatusesUpdate);

            updates.put("updatedAt", FieldValue.serverTimestamp());

            sellerRef.set(updates, SetOptions.merge()).get();

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("status", "success");
            responseData.put("network", network);
            responseData.put("currency", currency);
            responseData.put("message", "Seller currency deleted successfully");
            responseData.put("sellerId", sellerId);

            return responseData;
        } catch (InterruptedException | ExecutionException e) {
            throw new BadRequest("EXEC001", "Error executing request: " + e.getMessage());
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