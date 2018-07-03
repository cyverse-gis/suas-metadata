package model;

/**
 * Class used to wrap any debug or error messages with a boolean
 */
public class DroneLogger
{
	private static final Boolean DEBUG_MSGS = true;
	private static final Boolean ERROR_MSGS = true;

	/**
	 * Logs an error messages if error messages are enabled
	 *
	 * @param error The error to print out
	 */
	public static void logError(String error)
	{
		if (ERROR_MSGS)
			System.err.println(error);
	}

	/**
	 * Logs an debug messages if debug messages are enabled
	 *
	 * @param debug The debug message to print out
	 */
	public static void logDebug(String debug)
	{
		if (DEBUG_MSGS)
			System.out.println(debug);
	}
}
