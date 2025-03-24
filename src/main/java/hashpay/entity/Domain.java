package hashpay.entity;

import com.google.cloud.firestore.DocumentSnapshot;

import abek.util.Document;

public class Domain {

	public Document doc;
	public String domain;
	public String webhook;
	public String color;
	public String icon;
	public String iconUrl;
	public boolean notify;

	public Domain(DocumentSnapshot ds) {
		doc = new Document(ds);
		domain = doc.getId();
		webhook = doc.getString("webhook");
		icon = doc.getString("icon");
		iconUrl = doc.getString("iconUrl");
		color = doc.getString("color");

		if (doc.contains("notify")) {
			notify = doc.getBoolean("notify");
		} else if (doc.contains("emailNotification")) {
			notify = doc.getBoolean("emailNotification");
		} else {
			notify = false; // Default value
		}
	}
}