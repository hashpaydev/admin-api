package hashpay.v1;

import java.util.Map;

import com.google.firebase.auth.UserRecord;

import abek.endpoint.AuthEndpoint;
import abek.exceptions.HttpError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class Service extends AuthEndpoint {

	private static final long serialVersionUID = 1L;

	protected Map<String,Object> process(UserRecord user, HttpServletRequest request, HttpServletResponse response) throws HttpError{
		
		return null;
	}
	
	
}
