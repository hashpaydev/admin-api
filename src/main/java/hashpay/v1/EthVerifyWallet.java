package hashpay.v1;

import abek.endpoint.PublicEndpoint;
import abek.exceptions.BadRequest;
import abek.exceptions.HttpError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.web3j.exceptions.MessageDecodingException;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetCode;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthLog;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class EthVerifyWallet extends PublicEndpoint {
	private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(EthVerifyWallet.class.getName());
    private static final String ETHEREUM_RPC_URL = "https://weathered-few-snowflake.quiknode.pro/3b0e06b8398cd962918f4a95f0b63ff285ad3864";
    private static final Web3j web3j = Web3j.build(new HttpService(ETHEREUM_RPC_URL));
    private static final String USDT_CONTRACT = "0xdac17f958d2ee523a2206206994597c13d831ec7";
    private static final String USDC_CONTRACT = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48";

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

        boolean isValidFormat = isValidEthereumAddress(walletAddress);
        boolean existsOnNetwork = false;

        if (isValidFormat) {
            try {
                existsOnNetwork = verifyWalletWithQuickNode(walletAddress);
            } catch (MessageDecodingException e) {
                logger.warning("QuickNode RPC error: " + e.getMessage());
                throw new BadRequest("WALLET002", "Failed to verify wallet with network: " + e.getMessage());
            } catch (Exception e) {
                logger.warning("Error during wallet verification: " + e.getMessage());
                throw new BadRequest("WALLET003", "Error verifying wallet: " + e.getMessage());
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

    public boolean isValidEthereumAddress(String address) {
        return address != null && address.matches("^0x[a-fA-F0-9]{40}$");
    }

    private boolean verifyWalletWithQuickNode(String address) throws IOException {
        try {
            EthGetTransactionCount txCount = web3j.ethGetTransactionCount(
                    address,
                    DefaultBlockParameterName.LATEST
            ).send();
            boolean hasEthTransactions = txCount != null &&
                    txCount.getTransactionCount().compareTo(BigInteger.ZERO) > 0;

            EthGetBalance balance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
            boolean hasEthBalance = balance != null &&
                    balance.getBalance().compareTo(BigInteger.ZERO) > 0;

            EthGetCode ethGetCode = web3j.ethGetCode(address, DefaultBlockParameterName.LATEST).send();
            boolean isContract = ethGetCode != null && !ethGetCode.getCode().equals("0x");

            String tokenTransferTopic = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

            EthFilter filterTo = new EthFilter(
                    DefaultBlockParameterName.EARLIEST,
                    DefaultBlockParameterName.LATEST,
                    (List<String>) null
            ).addOptionalTopics(
                    tokenTransferTopic,
                    null,
                    "0x000000000000000000000000" + address.substring(2)
            );

            filterTo.addSingleTopic(USDT_CONTRACT);
            EthLog usdtLogs = web3j.ethGetLogs(filterTo).send();
            boolean hasUsdtTransfers = usdtLogs != null && !usdtLogs.getLogs().isEmpty();

            filterTo.addSingleTopic(USDC_CONTRACT);
            EthLog usdcLogs = web3j.ethGetLogs(filterTo).send();
            boolean hasUsdcTransfers = usdcLogs != null && !usdcLogs.getLogs().isEmpty();

            return hasEthTransactions || hasEthBalance || isContract || hasUsdtTransfers || hasUsdcTransfers;

        } catch (MessageDecodingException e) {
            logger.warning("RPC error while verifying wallet: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.warning("Error during wallet verification: " + e.getMessage());
            return false;
        }
    }
}