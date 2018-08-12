import com.sun.net.httpserver.*;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.irods.jargon.core.connection.AuthScheme;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.connection.IRODSSession;
import org.irods.jargon.core.connection.IRODSSimpleProtocolManager;
import org.irods.jargon.core.connection.auth.AuthResponse;
import org.irods.jargon.core.exception.AuthenticationException;
import org.irods.jargon.core.exception.InvalidUserException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSAccessObjectFactoryImpl;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Simple program used to forward authentication requests received from HTTP auth headers to iRODS using jargon
 */
public class CalliopeAuth
{
	// The port we get data from
	private static final Integer INPUT_PORT = 5235;
	// The string containing the host address that we connect to
	private static final String CYVERSE_HOST = "data.cyverse.org";
	// The string containing the host address of CyVerse
	private static final Integer CYVERSE_PORT = 1247;
	// The directory that each user has as their home directory
	private static final String CYVERSE_HOME_DIRECTORY = "/iplant/home/";
	// Each user is part of the iPlant zone
	private static final String CYVERSE_ZONE = "iplant";

	// 3 endpoints, one for each different program, one for calliope users, one for calliope view users, and one for calliope admins
	private static final String CALLIOPE_USER_ENDPOINT = "/calliopeUserLogin";
	private static final String CALLIOPE_VIEW_USER_ENDPOINT = "/calliopeViewUserLogin";
	private static final String CALLIOPE_ADMIN_ENDPOINT = "/calliopeAdminLogin";

	// A list of admin accounts
	private static final List<String> CALLIOPE_ADMIN_ACCOUNTS = Arrays.asList("dslovikosky", "danielslee");

	// A username and password to be used by the calliope view program
	private static final String CALLIOPE_VIEW_GUEST_USERNAME = "guest";
	private static final String CALLIOPE_VIEW_GUEST_PASSWORD = "guest";

	/**
	 * Main just starts a sever that listens
	 *
	 * @param args Ignored
	 */
	public static void main(String[] args)
	{
		// Create an HTTP server to server HTTP requests
		HttpServer server = null;
		// Attempt to open a port to listen for requests on
		try
		{
			// The second parameter lets us control the number of connects allowed at once. 0 means use the system default
			server = HttpServer.create(new InetSocketAddress(InetAddress.getLocalHost(), INPUT_PORT), 0);
		}
		catch (IOException e)
		{
			// If an exception occurs, print an error and exit the program
			System.err.println("Could not open port " + INPUT_PORT + " to listen on. Error was:");
			e.printStackTrace();
			System.exit(-1);
		}

		HttpHandler defaultHandler = httpExchange ->
		{
			// Send back html code 200 with no body
			httpExchange.sendResponseHeaders(200, 0);
			httpExchange.close();
		};

		// Create a context for the user endpoint (localhost:port/calliopeUserLogin) that will be called if we get a valid request
		HttpContext calliopeUserContext = server.createContext(CALLIOPE_USER_ENDPOINT, defaultHandler);
		// Set the authenticator for the root to ask Jargon to authenticate.
		calliopeUserContext.setAuthenticator(new BasicAuthenticator(CALLIOPE_USER_ENDPOINT)
		{
			@Override
			public boolean checkCredentials(String username, String password)
			{
				return performIRODSAuth(username, password);
			}
		});

		// Create a context for the calliope view program (localhost:port/calliopeViewUserLogin) that will be called if we get a valid request
		HttpContext calliopeViewUserContext = server.createContext(CALLIOPE_VIEW_USER_ENDPOINT, defaultHandler);
		// Set the authenticator to ensure the account is the single valid guest account
		calliopeViewUserContext.setAuthenticator(new BasicAuthenticator(CALLIOPE_VIEW_USER_ENDPOINT)
		{
			@Override
			public boolean checkCredentials(String username, String password)
			{
				return username.equals(CALLIOPE_VIEW_GUEST_USERNAME) && password.equals(CALLIOPE_VIEW_GUEST_PASSWORD);
			}
		});

		// Create a context for a calliope admin login (localhost:port/calliopeAdminLogin) that will be called if we get a valid request
		HttpContext calliopeAdminContext = server.createContext(CALLIOPE_ADMIN_ENDPOINT, defaultHandler);
		// Set the authenticator to ensure the account is an admin account and authenticates
		calliopeAdminContext.setAuthenticator(new BasicAuthenticator(CALLIOPE_ADMIN_ENDPOINT)
		{
			@Override
			public boolean checkCredentials(String username, String password)
			{
				return CALLIOPE_ADMIN_ACCOUNTS.contains(username) && CalliopeAuth.performIRODSAuth(username, password);
			}
		});
		// Use the default server executor
		server.setExecutor(null);
		// Start the server
		server.start();
		System.out.println("Authentication server started successfully...");
	}

	/**
	 * Given a username and a password this function lets us test if the account is valid
	 *
	 * @param username The username to test
	 * @param password The password to test
	 * @return True if the account is valid, false otherwise
	 */
	private static boolean performIRODSAuth(String username, String password)
	{
		// The session we will work with to authenticate
		IRODSSession session = null;
		try
		{
			// Create a new CyVerse account given the host address, port, username, password, homedirectory, and one field I have no idea what it does..., however leaving it as empty string makes file creation work!
			IRODSAccount account = IRODSAccount.instance(CYVERSE_HOST, CYVERSE_PORT, username, password, CYVERSE_HOME_DIRECTORY + username, CYVERSE_ZONE, "", AuthScheme.STANDARD);
			// Create a new session
			session = IRODSSession.instance(IRODSSimpleProtocolManager.instance());
			// Create an irodsAO
			IRODSAccessObjectFactory irodsAO = IRODSAccessObjectFactoryImpl.instance(session);
			// Perform the authentication and get a response
			AuthResponse authResponse = irodsAO.authenticateIRODSAccount(account);
			// If the authentication worked, return true otherwise return false
			return authResponse.isSuccessful();
		}
		// If the authentication failed due to an exception, return false
		catch (Exception e)
		{
			System.out.println("Authentication failed.");
			return false;
		}
		finally
		{
			// After we're done, if the session is non-null terminate it
			if (session != null)
			{
				// If closing the session fails, ignore the error
				try { session.closeSession(); }
				catch (JargonException ignored) {}
			}
		}
	}
}
