package hashpay.entity;

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

public class Payment {
	
	public Document doc;
	public String id;
	public String seller;
	public String publicKey;
	public String privateKey;
	public String address;
	public String network;
	public String currency;
	
	
	public double amount;
	
	public boolean ETH = false;
	public boolean SOL = false;
	public boolean TRX = false;
	public boolean USDT = false;
	public boolean USDC = false;
	
	public Payment(String seller, String currency, String network) {
		
		this.seller = seller;
		this.network = network.toUpperCase();
		this.currency = currency.toUpperCase();
		setBooleans();
	}
	
	public Payment(DocumentSnapshot ds) throws HttpError {
		fromDocumentSnapshot(ds);
	}
	
	public Payment(Firestore db, String id) throws HttpError{
		if(Util.isEmpty(id)) throw new BadRequest("Id is required");
		try {
			DocumentSnapshot ds = db.collection("payments").document(id).get().get();
			fromDocumentSnapshot(ds);
		} catch (InterruptedException | ExecutionException e) {
			throw new InternalServerError(e.getMessage());
		}
	}
	private void fromDocumentSnapshot(DocumentSnapshot ds) throws HttpError{
		if( !doc.exists()) throw new NotFound(doc.getId());
		doc = new Document(ds);
		id = doc.getId();
		publicKey = doc.getString("publicKey");
		network = doc.getString("network");
		currency = doc.getString("currency");
		seller = doc.getString("seller");
		amount = doc.getDouble("amount");
		setBooleans();
	}
	
	public void setBooleans() {
		ETH = SOL = TRX = USDC = USDT = false;
		if("ETH".equals(network)) ETH = true;
		else if("SOL".equals(network)) SOL = true;
		else if("TRX".equals(network)) TRX = true;
		if("USDT".equals(currency)) USDT = true;
		else if("USDC".equals(currency)) USDC = true;
	}
	
	public void setAddress(Address addy) {
		address = addy.address;
		publicKey = addy.publicKey;
		privateKey = addy.privateKey;
	}
	public Map<String,Object> toMap(){
		Map<String,Object> map = Util.toMap("id",id);
		map.put("seller",seller);
		map.put("privateKey",privateKey);
		map.put("publicKey",publicKey);
		map.put("address",address);
		map.put("network",network);
		map.put("currency",currency);
		return map;
	}
}
