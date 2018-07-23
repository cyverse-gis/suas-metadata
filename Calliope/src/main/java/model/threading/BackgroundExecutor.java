package model.threading;

import javafx.concurrent.Worker;

import java.util.concurrent.Executors;

/**
 * Background executor used for tasks that aren't very important but shouldn't freeze the UI
 */
public class BackgroundExecutor extends BaseCalliopeExecutor
{
	/**
	 * Constructor specifies a cached thread pool which can grow infinitely
	 */
	public BackgroundExecutor()
	{
		super(Executors.newFixedThreadPool(25));
	}

	/**
	 * Ignored
	 *
	 * @param worker The worker that finished
	 */
	@Override
	protected void onSucceeded(Worker<?> worker)
	{
	}

	/**
	 * Ignored
	 *
	 * @param worker The worker that started
	 */
	@Override
	protected void onRunning(Worker<?> worker)
	{
	}
}
