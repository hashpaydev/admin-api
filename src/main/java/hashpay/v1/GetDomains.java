package hashpay.v1;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.firebase.auth.UserRecord;

import abek.endpoint.AuthEndpoint;
import abek.exceptions.HttpError;
import abek.exceptions.InternalServerError;
import abek.exceptions.NotFound;
import hashpay.entity.Domain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class GetDomains extends AuthEndpoint {

	private static final long serialVersionUID = 1L;

	protected Map<String,Object> process(UserRecord user, HttpServletRequest request, HttpServletResponse response) throws HttpError{
		
		String origin = request.getHeader("origin");
		
		try {
			DocumentSnapshot doc = db.collection("domains").document(origin).get().get();
			if(!doc.exists()) throw new NotFound(origin);
			
			Domain domain = new Domain(doc);
			
			
		} catch (InterruptedException | ExecutionException e) {
			throw new InternalServerError(e.getMessage());
		}
		
		return null;
		
	}
	
	
}
