package hashpay.v1;

import java.io.FileInputStream;
import java.io.InputStream;

import jakarta.servlet.MultipartConfigElement;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.eclipse.jetty.servlet.ServletHolder;

public class Start
{
    public static void main(String[] args) throws Exception
    {
    	try {
			
			InputStream serviceAccount = new FileInputStream("src/main/java/hashpaytest-firebase-adminsdk-fbsvc-a95e978e30.json");

			GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
			FirebaseOptions options = FirebaseOptions.builder()
			    .setCredentials(credentials)
			    .setProjectId("hashpaytest")
			    .build();
			FirebaseApp.initializeApp(options);

			
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	
    	Server server = new Server(8082);
    	ServletContextHandler handler = new ServletContextHandler(server, "/");
    	handler.addServlet(hashpay.v1.Service.class, "/v1/balance");
		//Authentification

		handler.addServlet(hashpay.v1.auth.Login.class,"/v1/login");
		handler.addServlet(hashpay.v1.auth.Register.class,"/v1/register");


		handler.addServlet(hashpay.v1.SolanaVerifyWallet.class,"/v1/solana/check-solana-wallet");
		handler.addServlet(hashpay.v1.EthVerifyWallet.class,"/v1/eth/check-eth-wallet");
		handler.addServlet(hashpay.v1.CheckTronWallet.class,"/v1/tron/check-tron-wallet");

//		handler.addServlet(hashpay.v1.SetPayoutAdrEndpoint.class, "/v1/setPayoutAddress");
		handler.addServlet(hashpay.v1.EnableCurrency.class, "/v1/EnableCurrency");
		handler.addServlet(DeleteCurrency.class,"/v1/DisableCurrency");
		handler.addServlet(hashpay.v1.GetPayoutProfile.class, "/v1/payout-profile");

		handler.addServlet(hashpay.v1.SetDomain.class, "/v1/SetDomain");
		handler.addServlet(hashpay.v1.DeleteDomain.class, "/v1/deleteDomain");
//		handler.addServlet(hashpay.v1.SetDomainIcon.class, "/v1/SetDomainIcon");
		ServletHolder domainIconServlet = new ServletHolder(new hashpay.v1.SetDomainIcon());
		domainIconServlet.getRegistration().setMultipartConfig(
				new MultipartConfigElement("",
						2 * 1024 * 1024,
						4 * 1024 * 1024,
						0));
		handler.addServlet(domainIconServlet, "/v1/SetDomainIcon");

    	
        server.start();
        server.join();
        
    }

}