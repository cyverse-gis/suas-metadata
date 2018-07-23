package model.threading;

/**
 * Class used to keep track of threads to run in the background
 */
public class CalliopeExecutor
{
	// Queued executor is used to perform tasks one by one
	private QueuedExecutor queuedExecutor = new QueuedExecutor();
	// Immediate executor is used to do tasks at once
	private ImmediateExecutor immediateExecutor = new ImmediateExecutor();
	// Background executor is used to do anything that can be canceled and isn't very important
	private BackgroundExecutor backgroundExecutor = new BackgroundExecutor();

	/**
	 * Returns the queued executor, use this if you want to perform tasks one by one
	 *
	 * @return The queued executor
	 */
	public QueuedExecutor getQueuedExecutor()
	{
		return this.queuedExecutor;
	}

	/**
	 * Returns the immediate executor, use this if you want to perform tasks at the same time immediately
	 *
	 * @return The immediate executor
	 */
	public ImmediateExecutor getImmediateExecutor()
	{
		return this.immediateExecutor;
	}

	/**
	 * Returns the background executor, use this if you want to perform tasks that can be canceled and are not that important
	 *
	 * @return The background executor
	 */
	public BackgroundExecutor getBackgroundExecutor()
	{
		return this.backgroundExecutor;
	}

	/**
	 * Returns true if any of the executors are performing tasks, false otherwise
	 *
	 * @return True if any task is running, false otherwise
	 */
	public Boolean anyTaskRunning()
	{
		return this.queuedExecutor.getTaskRunning() || this.immediateExecutor.getTaskCount() > 0;
	}
}
