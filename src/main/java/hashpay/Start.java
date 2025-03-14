package hashpay;

import java.io.FileInputStream;
import java.io.InputStream;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class Start
{
    public static void main(String[] args) throws Exception
    {
    	try {
			
			InputStream serviceAccount = new FileInputStream("hashpay-dev-5180f825aa94.json");
			GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
			FirebaseOptions options = FirebaseOptions.builder()
			    .setCredentials(credentials)
			    .setProjectId("hashpay-dev")
			    .build();
			FirebaseApp.initializeApp(options);

			
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	
    	Server server = new Server(8081);
    	ServletContextHandler handler = new ServletContextHandler(server, "/");
    	handler.addServlet(hashpay.v1.Service.class, "/v1/balance");

    	
        server.start();
        server.join();
        
    }

}