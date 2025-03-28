package hashpay.v1;

import abek.exceptions.BadRequest;
import abek.exceptions.HttpError;
import com.google.cloud.firestore.*;
import com.google.firebase.auth.UserRecord;
import com.google.gson.Gson;

import hashpay.entity.Seller;
import abek.endpoint.AuthEndpoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.google.firebase.cloud.FirestoreClient.getFirestore;

public class EnableCurrency extends AuthEndpoint {
    private static final long serialVersionUID = 1L;
    //private static final Gson gson = new Gson();

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
            Seller seller = new Seller(db, sellerId);
            String network = getRequiredParameter(request, "network").toUpperCase();
            String currency = getRequiredParameter(request, "currency").toUpperCase();
            String address = request.getParameter("add");

            if (!SUPPORTED_NETWORKS.contains(network)) {
                throw new BadRequest("NET001", "Invalid or unsupported network: " + network);
            }

            if (!SUPPORTED_CURRENCIES.contains(currency)) {
                throw new BadRequest("CUR002", "Unsupported currency: " + currency);
            }

            String networkField = network.toLowerCase();
            String finalAddress = null;


            if (address == null || address.trim().isEmpty()) {

                String existingAddress = seller.doc.getString(networkField);
                if (existingAddress != null && !existingAddress.trim().isEmpty()) {
                    finalAddress = existingAddress;
                } else {
                    throw new BadRequest("ADR001",
                            "No address set for network " + network + ". Please provide an address parameter.");
                }
            } else {
                finalAddress = address.trim();
            }


            boolean isValidAddress = validateAddress(network, finalAddress);
            if (!isValidAddress) {
                throw new BadRequest("ADR002", "Invalid wallet address for network " + network);
            }



            DocumentReference sellerRef = db.collection("sellers").document(sellerId);

            Map<String, Object> updates = new HashMap<>();
            updates.put(networkField, finalAddress);

            updates.put("currencyStatuses." + network + "." + currency, true);

            updates.put(currency + "_" + network, finalAddress);

            updates.put("updatedAt", FieldValue.serverTimestamp());

            sellerRef.set(updates, SetOptions.merge()).get();

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("status", "success");
            responseData.put("network", network);
            responseData.put("currency", currency);
            responseData.put("address", finalAddress);
            responseData.put("currencyKey", currency + "_" + network);
            responseData.put("sellerId", sellerId);
            responseData.put("message", "Seller currency enabled and address updated successfully");

            return responseData;
        } catch (InterruptedException | ExecutionException e) {
            throw new BadRequest("EXEC001", "Error executing request: " + e.getMessage());
        }
    }

    private boolean validateAddress(String network, String address) throws BadRequest {
        if (address == null || address.trim().isEmpty()) {
            return false;
        }

        try {
            boolean isValid = false;
            if ("SOL".equals(network)) {
                SolanaVerifyWallet verifier = new SolanaVerifyWallet();
                isValid = verifier.isValidSolanaAddress(address);
            } else if ("ETH".equals(network)) {
                EthVerifyWallet verifier = new EthVerifyWallet();
                isValid = verifier.isValidEthereumAddress(address);
            } else if ("TRX".equals(network)) {
                CheckTronWallet verifier = new CheckTronWallet();
                isValid = verifier.isValidTronAddress(address);
            }
            return isValid;
        } catch (Exception e) {
            throw new BadRequest("ADR003", "Error validating wallet address: " + e.getMessage());
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