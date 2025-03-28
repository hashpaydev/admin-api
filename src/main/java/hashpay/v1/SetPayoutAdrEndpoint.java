package hashpay.v1;

import abek.endpoint.AuthEndpoint;
import abek.exceptions.BadRequest;
import abek.exceptions.HttpError;
import abek.util.HTTP;
import com.google.cloud.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static com.google.firebase.cloud.FirestoreClient.getFirestore;

public class SetPayoutAdrEndpoint extends AuthEndpoint {
    private static final long serialVersionUID = 1L;

    private static final Set<String> VALID_NETWORKS = Set.of("SOL", "ETH", "TRX");
    private static final Set<String> VALID_CURRENCIES = Set.of("USDC", "USDT");

    protected void process(HttpServletRequest request, HttpServletResponse response)
            throws HttpError, IOException, InterruptedException, ExecutionException {

        JsonObject jsonInput = parseRequestBody(request);
        String email = getRequiredString(jsonInput, "email", false);
        String network = getRequiredString(jsonInput, "network", true);
        String address = getRequiredString(jsonInput, "address", false);

        String currency = null;
        if (jsonInput.has("currency") && !jsonInput.get("currency").getAsString().trim().isEmpty()) {
            currency = jsonInput.get("currency").getAsString().trim().toUpperCase();
            if (!VALID_CURRENCIES.contains(currency)) {
                throw new BadRequest("CUR002", "Invalid currency: " + currency);
            }
        }

        if (!VALID_NETWORKS.contains(network)) {
            throw new BadRequest("NET002", "Invalid network: " + network);
        }

        String uid;
        try {
            UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(email);
            uid = userRecord.getUid();
        } catch (FirebaseAuthException e) {
            throw new BadRequest("AUTH001", "Invalid user email: " + email);
        }

        Firestore db = getFirestore();
        DocumentReference sellerRef = db.collection("sellers").document(uid);
        DocumentSnapshot docSnapshot = sellerRef.get().get();

        Map<String, Object> updates = new HashMap<>();

        updates.put(network.toLowerCase(), address);


        if (currency != null) {
            String networkCurrency = network + "_" + currency;
            updates.put("addresses." + networkCurrency, address);
        } else {
            for (String curr : VALID_CURRENCIES) {
                String networkCurrency = network + "_" + curr;
                updates.put("addresses." + networkCurrency, address);
            }
        }

        Map<String, Boolean> currencyStatusMap = new HashMap<>();
        if (docSnapshot.exists() && docSnapshot.contains("currencyStatuses." + network)) {

            Map<String, Boolean> existingStatuses = (Map<String, Boolean>) docSnapshot.get("currencyStatuses." + network);
            if (existingStatuses != null) {
                currencyStatusMap.putAll(existingStatuses);
            }
        } else {
            if (network.equals("ETH")) {
                currencyStatusMap.put("USDC", true);
                currencyStatusMap.put("USDT", false);
            } else if (network.equals("SOL") || network.equals("TRX")) {
                currencyStatusMap.put("USDC", false);
                currencyStatusMap.put("USDT", true);
            }
        }

        if (currency != null) {
            currencyStatusMap.put(currency, true);
        }

        updates.put("currencyStatuses." + network, currencyStatusMap);

        List<String> currentCurrencies = new ArrayList<>();
        if (docSnapshot.exists() && docSnapshot.contains("currencies")) {
            @SuppressWarnings("unchecked")
            List<String> existingCurrencies = (List<String>) docSnapshot.get("currencies");
            if (existingCurrencies != null) {
                currentCurrencies.addAll(existingCurrencies);
            }
        }

        for (Map.Entry<String, Boolean> entry : currencyStatusMap.entrySet()) {
            if (entry.getValue()) {
                String networkCurrency = network + "_" + entry.getKey();
                if (!currentCurrencies.contains(networkCurrency)) {
                    currentCurrencies.add(networkCurrency);
                }
            }
        }

        updates.put("currencies", currentCurrencies);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        sellerRef.set(updates, SetOptions.merge()).get();

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("status", "success");
        responseData.put("message", "Seller address updated for network " + network);
        responseData.put("uid", uid);
        responseData.put("network", network);
        responseData.put("address", address);
        responseData.put("currencyStatuses", currencyStatusMap);

        HTTP.sendJSON(response, responseData);
    }

    private JsonObject parseRequestBody(HttpServletRequest request) throws IOException, BadRequest {
        StringBuilder sb = new StringBuilder();
        String line;
        try (java.io.BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        try {
            return JsonParser.parseString(sb.toString()).getAsJsonObject();
        } catch (Exception e) {
            throw new BadRequest("JSON001", "Invalid JSON format");
        }
    }

    private String getRequiredString(JsonObject json, String field, boolean toUpperCase) throws BadRequest {
        if (!json.has(field) || json.get(field).getAsString().trim().isEmpty()) {
            throw new BadRequest(field.toUpperCase() + "001", field + " is required");
        }
        String value = json.get(field).getAsString().trim();
        return toUpperCase ? value.toUpperCase() : value;
    }
}