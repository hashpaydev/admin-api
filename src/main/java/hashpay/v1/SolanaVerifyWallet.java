package hashpay.v1;

import abek.endpoint.PublicEndpoint;
import abek.exceptions.BadRequest;
import abek.exceptions.HttpError;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.rpc.RpcClient;
import org.p2p.solanaj.rpc.RpcException;
import org.p2p.solanaj.rpc.types.AccountInfo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class SolanaVerifyWallet extends PublicEndpoint {
	private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(SolanaVerifyWallet.class.getName());
    private static final RpcClient rpcClient = new RpcClient("https://weathered-few-snowflake.solana-mainnet.quiknode.pro/3b0e06b8398cd962918f4a95f0b63ff285ad3864");

    @Override
    protected Map<String, Object> process(HttpServletRequest request, HttpServletResponse response)
            throws HttpError {

        try {
            logger.info("Starting wallet verification process");

            String walletAddress = request.getParameter("wallet");
            logger.info("Request parameter wallet: " + walletAddress);

            if (walletAddress == null || walletAddress.isEmpty()) {
                throw new BadRequest("WALLET001", "Missing required parameter: wallet");
            }

            walletAddress = walletAddress.trim();
            logger.info("Processing wallet address: " + walletAddress);

            boolean isValidFormat = isValidSolanaAddress(walletAddress);
            boolean existsOnNetwork = false;

            if (isValidFormat) {
                existsOnNetwork = verifyWalletWithQuickNode(walletAddress);
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("status", "success");
            responseData.put("isValid", isValidFormat);
            responseData.put("exists", existsOnNetwork);
            responseData.put("wallet", walletAddress);

            logger.info("Success response prepared for wallet: " + walletAddress);
            return responseData;

        } catch (RpcException e) {
            logger.severe("QuickNode RPC error: " + e.getMessage());
            throw new BadRequest("RPC001", "Failed to verify wallet with network: " + e.getMessage());
        } catch (Exception e) {
            logger.severe("Unexpected error: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            throw new BadRequest("EXEC001", "Unexpected error: " + e.getMessage());
        }
    }

    public boolean isValidSolanaAddress(String address) {
        try {
            if (address == null || address.isEmpty()) {
                return false;
            }
            new PublicKey(address);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifyWalletWithQuickNode(String address) throws RpcException {
        try {
            AccountInfo accountInfo = rpcClient.getApi().getAccountInfo(new PublicKey(address));
            return accountInfo != null && accountInfo.getValue() != null;
        } catch (RpcException e) {
            logger.warning("RPC error while verifying wallet: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.warning("Error during wallet verification: " + e.getMessage());
            return false;
        }
    }
}