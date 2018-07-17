package model.cyverse;

import javafx.util.Pair;
import model.CalliopeData;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.connection.IRODSSession;
import org.irods.jargon.core.connection.IRODSSimpleProtocolManager;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSAccessObjectFactoryImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that maintains connections to cyverse
 */
class CyVerseSessionManager
{
	// A map of thread -> session objects, used to keep 1 session per thread
	private Map<Thread, IRODSSession> sessions = Collections.synchronizedMap(new HashMap<>());
	private Map<Thread, IRODSAccessObjectFactory> accessObjects = Collections.synchronizedMap(new HashMap<>());
	private Map<Thread, Integer> accessCounts = Collections.synchronizedMap(new HashMap<>());

	// A reference to the authenticated irods account
	private IRODSAccount authenticatedAccount;

	/**
	 * Constructor just needs the authenticated irods account
	 *
	 * @param authenticatedAccount The account that has been authenticated
	 */
	CyVerseSessionManager(IRODSAccount authenticatedAccount)
	{
		this.authenticatedAccount = authenticatedAccount;
	}

	/**
	 * Either returns false if a session is already open in the current thread or a session fails to open, returns true otherwise
	 *
	 * @return True if the session was opened successfully
	 */
	boolean openSession()
	{
		// Grab the current thread
		Thread current = Thread.currentThread();
		// Test if this thread already has a session object
		if (this.sessions.containsKey(current))
		{
			// If it does, we increment our session counter by one
			this.accessCounts.put(current, this.accessCounts.get(current) + 1);
			return true;
		}
		else
		{
			// If it doesn't, create a session
			try
			{
				// Create the session and store it
				IRODSSession newSession = IRODSSession.instance(IRODSSimpleProtocolManager.instance());
				IRODSAccessObjectFactory newAccessFactory = IRODSAccessObjectFactoryImpl.instance(newSession);
				// Store our session, access object factory, and access count (1 to start)
				this.sessions.put(current, newSession);
				this.accessObjects.put(current, newAccessFactory);
				this.accessCounts.put(current, 1);
				return true;
			}
			// Print an error and return false
			catch (JargonException e)
			{
				CalliopeData.getInstance().getErrorDisplay().notify("Error creating a session!\n" + ExceptionUtils.getStackTrace(e));
				return false;
			}
		}
	}

	/**
	 * Closes the session for the current thread if there is one open at the moment
	 */
	void closeSession()
	{
		// Grab the current thread, and see if a session is associated with the thread
		Thread current = Thread.currentThread();
		// If we know about this thread, begin processing it
		if (this.accessCounts.containsKey(current))
		{
			// Grab the number of times we've tried to open this session
			Integer sessionCount = this.accessCounts.get(current);
			// If it's just 1, this session can be closed and removed
			if (sessionCount == 1)
			{
				// Remove any mappings associated with this thread
				this.accessCounts.remove(current);
				this.accessObjects.remove(current);
				IRODSSession session = this.sessions.remove(current);
				try
				{
					// Close the session
					session.closeSession(this.authenticatedAccount);
				}
				// An error occured, ignore it
				catch (JargonException e)
				{
					CalliopeData.getInstance().getErrorDisplay().notify("Error closing a session!\n" + ExceptionUtils.getStackTrace(e));
				}
			}
			else
			{
				// If there's more than one function using this session, decrement the counter
				this.accessCounts.put(current, sessionCount - 1);
			}
		}
	}

	/**
	 * Getter for the current access object this thread is operating on
	 *
	 * @return An access object or null if no access object object is present for this thread
	 */
	IRODSAccessObjectFactory getCurrentAO()
	{
		return this.accessObjects.get(Thread.currentThread());
	}
}
