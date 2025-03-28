package hashpay.v1;

import abek.endpoint.PublicEndpoint;
import abek.exceptions.BadRequest;
import abek.exceptions.HttpError;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

public class CheckTronWallet extends PublicEndpoint {
    private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(CheckTronWallet.class.getName());
    private static final String QUICKNODE_URL = "https://weathered-few-snowflake.tron-mainnet.quiknode.pro/3b0e06b8398cd962918f4a95f0b63ff285ad3864";

    @Override
    protected Map<String, Object> process(HttpServletRequest request, HttpServletResponse response)
            throws HttpError {

        logger.info("Starting wallet verification process");

        String walletAddress = request.getParameter("wallet");
        logger.info("Request parameter wallet: " + walletAddress);

        if (walletAddress == null || walletAddress.isEmpty()) {
            throw new BadRequest("WALLET001", "Missing required parameter: wallet");
        }

        walletAddress = walletAddress.trim();
        logger.info("Processing wallet address: " + walletAddress);

        boolean isValidFormat = isValidTronAddress(walletAddress);
        boolean existsOnNetwork = false;

        if (isValidFormat) {
            try {
                existsOnNetwork = verifyWalletWithQuickNode(walletAddress);
            } catch (Exception e) {
                logger.warning("Error during QuickNode wallet verification: " + e.getMessage());
                throw new BadRequest("WALLET002", "Error verifying wallet with QuickNode: " + e.getMessage());
            }
        }

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("status", "success");
        responseData.put("isValid", isValidFormat);
        responseData.put("exists", existsOnNetwork);
        responseData.put("wallet", walletAddress);

        logger.info("Success response prepared for wallet: " + walletAddress);
        return responseData;
    }

    public boolean isValidTronAddress(String address) {
        return address != null && address.matches("^T[a-zA-Z0-9]{33}$");
    }

    private boolean verifyWalletWithQuickNode(String address) throws IOException {
        URL url = new URL(QUICKNODE_URL + "/wallet/getaccount");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("address", address);
        requestBody.addProperty("visible", true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.toString().getBytes());
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            StringBuilder response = new StringBuilder();
            try (Scanner scanner = new Scanner(conn.getInputStream())) {
                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }
            }

            JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
            return jsonResponse != null && !jsonResponse.entrySet().isEmpty();
        }
        return false;
    }
}