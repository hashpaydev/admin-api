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

import java.util.*;
import java.util.concurrent.ExecutionException;

import static com.google.firebase.cloud.FirestoreClient.getFirestore;


public class GetPayoutProfile extends AuthEndpoint {

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

            Map<String, Object> sellerData = new HashMap<>();
            sellerData.put("sellerId", sellerId);

            List<Map<String, Object>> payoutAddresses = new ArrayList<>();

            for (String network : SUPPORTED_NETWORKS) {
                String networkLower = network.toLowerCase();

                if (sellerDoc.contains(networkLower)) {
                    String address = sellerDoc.getString(networkLower);

                    Map<String, Boolean> currencyStatuses = new HashMap<>();

                    if (sellerDoc.contains("currencyStatuses")) {

                        Map<String, Object> allStatuses = (Map<String, Object>) sellerDoc.get("currencyStatuses");

                        if (allStatuses != null && allStatuses.containsKey(network)) {

                            Map<String, Boolean> networkStatuses = (Map<String, Boolean>) allStatuses.get(network);
                            if (networkStatuses != null) {
                                currencyStatuses = networkStatuses;
                            }
                        }
                    }

                    Map<String, Object> networkDetails = new HashMap<>();
                    networkDetails.put("network", network);
                    networkDetails.put("address", address);
                    networkDetails.put("currencies", new ArrayList<>());

                    for (String currency : SUPPORTED_CURRENCIES) {
                        Map<String, Object> currencyDetails = new HashMap<>();
                        currencyDetails.put("currency", currency);

                        boolean isEnabled = currencyStatuses.getOrDefault(currency, false);
                        currencyDetails.put("enabled", isEnabled);

                        ((List<Map<String, Object>>) networkDetails.get("currencies")).add(currencyDetails);
                    }

                    payoutAddresses.add(networkDetails);
                }
            }

            sellerData.put("payoutAddresses", payoutAddresses);

            if (sellerDoc.contains("updatedAt")) {
                sellerData.put("updatedAt", sellerDoc.get("updatedAt"));
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("status", "success");
            responseData.put("seller", sellerData);

            return responseData;
        } catch (InterruptedException | ExecutionException e) {
            throw new BadRequest("EXEC001", "Error executing request: " + e.getMessage());
        }
    }
}