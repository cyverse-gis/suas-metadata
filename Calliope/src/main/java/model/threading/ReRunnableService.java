package model.threading;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import model.CalliopeData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility class which allows for simple service restarting. By requesting a restart, this service will automatically re-run the service after finish the
 * existing run without killing the exiting run
 *
 * @param <T> The type of the service to return
 */
public class ReRunnableService<T>
{
	// The service reference
	private ErrorService<T> service;
	// A flag which we use to test if we need to perform another run after finishing the exiting one
	private AtomicBoolean needsAnotherRun = new AtomicBoolean(false);
	// A flag which we use to check if the service is currently running
	private AtomicBoolean serviceInProgress = new AtomicBoolean(false);

	/**
	 * Primary constructor requires one argument, a factory for a task object.
	 *
	 * @param taskCreator The task factory
	 */
	public ReRunnableService(Supplier<Task<T>> taskCreator)
	{
		// Call the other constructor with the immediate sanimal executor which executes tasks immediately
		this(taskCreator, CalliopeData.getInstance().getExecutor().getImmediateExecutor());
	}

	/**
	 * Alternate constructor which takes a factory like the first constructor but also an executor which is to be used to execute tasks.
	 *
	 * @param taskCreator The task factory
	 * @param executor The executor the service will use to run tasks
	 */
	public ReRunnableService(Supplier<Task<T>> taskCreator, BaseCalliopeExecutor executor)
	{
		// Setup our service, when create task gets called we ask our factory for the task and return it
		this.service = new ErrorService<T>()
		{
			@Override
			protected Task<T> createTask()
			{
				return taskCreator.get();
			}
		};

		// When we finish the service, test if we need to run again
		this.service.setOnSucceeded(event -> {
			// After finishing, check if we need to run again. If so set the flag to false and run once again
			if (this.needsAnotherRun.get())
			{
				this.needsAnotherRun.set(false);
				this.service.restart();
			}
			// If we don't need to run again set the sync in progress flag to false
			else
			{
				this.serviceInProgress.set(false);
			}
		});
		executor.registerService(this.service);
	}

	/**
	 * Most important function here, it allows requesting another service run. If the service is already running it will wait for the current run to finish
	 * and then run again.
	 */
	public void requestAnotherRun()
	{
		// If a run is already in progress, we set a flag telling the current run to perform another run right after it finishes
		if (this.serviceInProgress.get())
		{
			this.needsAnotherRun.set(true);
		}
		// If a run is not in progress, go ahead and run
		else
		{
			this.serviceInProgress.set(true);
			// Perform the task
			this.service.restart();
		}
	}

	/**
	 * Allows us to add a listener for when the currently executing task finishes. Takes one argument which is the consumer to take the result of the
	 * execution and pass it off
	 *
	 * @param toExecute The consumer to be called with the result of the thread execution
	 */
	public void addFinishListener(Consumer<T> toExecute)
	{
		// Get the current succeeded event handler
		EventHandler<WorkerStateEvent> onSucceeded = this.service.getOnSucceeded();
		// Once the service finishes...
		this.service.setOnSucceeded(event ->
		{
			// Call our parameter's accept function to perform whatever action necessary
			toExecute.accept(this.service.getValue());
			// Fire our old handler which finishes the task's execution
			onSucceeded.handle(event);
		});
	}
}
