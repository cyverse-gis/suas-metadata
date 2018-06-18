public class DroneLogger
{
	private static final Boolean DEBUG_MSGS = true;
	private static final Boolean ERROR_MSGS = true;

	public static void logError(String error)
	{
		if (ERROR_MSGS)
			System.err.println(error);
	}

	public static void logDebug(String debug)
	{
		if (DEBUG_MSGS)
			System.out.println(debug);
	}
}
