package hashpay.entity;

import com.google.cloud.firestore.DocumentSnapshot;

import abek.util.Document;

public class Domain {
	
	public Document doc;
	public String domain;
	public String webhook;
	public String color;
	public String icon;
	public boolean notify;
	
	public Domain(DocumentSnapshot ds) {
		doc = new Document(ds);
		domain = doc.getId();
		webhook = doc.getString("webhook");
		icon = doc.getString("icon");
		color = doc.getString("wolor");
		notify = doc.getBoolean("notify");
	}

}
