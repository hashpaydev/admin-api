package hashpay.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

import abek.exceptions.BadRequest;
import abek.exceptions.HttpError;
import abek.exceptions.InternalServerError;
import abek.exceptions.NotFound;
import abek.util.Document;
import abek.util.Util;

public class Seller {

	public Document doc;
	public String id;
	public String USDT_SOL;
	public String USDT_ETH;
	public String USDT_TRX;
	public String USDC_SOL;
	public String USDC_ETH;
	public Map<String, Object> currencies;
	
	public Seller(Firestore db, String seller) throws HttpError{
		if(seller == null ) throw new BadRequest("seller id is blank");
		try {
			DocumentSnapshot ds = db.collection("sellers").document(seller).get().get();
			fromDocumentSnapshot(ds);
		} catch (InterruptedException | ExecutionException e) {
			throw new InternalServerError(e.getMessage());
		}

         
        
	}
	
	public Seller(DocumentSnapshot ds) throws HttpError {
		fromDocumentSnapshot(ds);
	}
	private void fromDocumentSnapshot(DocumentSnapshot ds) throws HttpError{
		id = ds.getId();
		if( !ds.exists()) throw new NotFound(id);
		doc = new Document(ds);
		USDT_SOL = doc.getString("USDT_SOL");
		USDT_ETH = doc.getString("USDT_ETH");
		USDT_TRX = doc.getString("USDT_TRX");
		USDC_SOL = doc.getString("USDC_SOL");
		USDC_ETH = doc.getString("USDC_ETH");
		
		currencies = Util.toMap("USDT_SOL",USDT_SOL);
		currencies.put("USDT_ETH",USDT_ETH);
		currencies.put("USDT_TRX",USDT_TRX);
		currencies.put("USDC_SOL",USDC_SOL);
		currencies.put("USDC_ETH",USDC_ETH);
	}
	
	public List<String> getCurrencyList(){
		List<String> list = new ArrayList<String>();
		if(USDT_SOL != null) list.add("USDT_SOL");
		if(USDT_ETH != null) list.add("USDT_ETH");
		if(USDT_TRX != null) list.add("USDT_TRX");
		if(USDC_SOL != null) list.add("USDC_SOL");
		if(USDC_ETH != null) list.add("USDC_ETH");
		return list;
	}
	
	public boolean isCurrencyEnabled(String currency, String network) {
		if(currency == null || network == null) return false;
		String v = (String) currencies.get(currency.toUpperCase()+"_"+network.toUpperCase());
		return ! Util.isEmpty(v);
	}
	
	
}
