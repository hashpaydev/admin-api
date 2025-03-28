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

            Map<String, Object> responseData = new HashMap<>();

            for (String network : SUPPORTED_NETWORKS) {
                for (String currency : SUPPORTED_CURRENCIES) {
                    String currencyKey = currency + "_" + network;

                    if (sellerDoc.contains(currencyKey)) {
                        String address = sellerDoc.getString(currencyKey);
                        if (address != null && !address.trim().isEmpty()) {
                            responseData.put(currencyKey, address);
                        }
                    }
                }
            }

            return responseData;
        } catch (InterruptedException | ExecutionException e) {
            throw new BadRequest("EXEC001", "Error executing request: " + e.getMessage());
        }
    }
}