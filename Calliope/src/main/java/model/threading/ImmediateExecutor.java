package model.threading;

import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Class that takes a task and IMMEDIATELY begins exeuction without queuing
 */
public class ImmediateExecutor extends BaseCalliopeExecutor
{
	// A list of active tasks being processed
	private final ObservableList<Task<?>> activeTasks = FXCollections.observableArrayList(task -> new Observable[] { task.progressProperty(), task.messageProperty() });
	// A list of tasks to display to the user
	private final ObservableList<Task<?>> activeDisplayedTasks = FXCollections.observableArrayList(task -> new Observable[] { task.progressProperty(), task.messageProperty() });
	// The number of tasks currently running
	private final ReadOnlyIntegerWrapper tasksRunning = new ReadOnlyIntegerWrapper();

	/**
	 * Constructor specifies a cached thread pool which can grow infinitely
	 */
	public ImmediateExecutor()
	{
		super(Executors.newFixedThreadPool(50));
	}

	/**
	 * When a task finishes, just remove it from the list of running tasks
	 *
	 * @param worker The worker that finished
	 */
	@Override
	protected void onSucceeded(Worker<?> worker)
	{
		this.tasksRunning.add(-1);
		if (worker instanceof Task<?>)
		{
			this.activeTasks.remove(worker);
			this.activeDisplayedTasks.remove(worker);
		}
	}

	/**
	 * When a task starts, just add it from the list of running tasks
	 *
	 * @param worker The worker that began
	 */
	@Override
	protected void onRunning(Worker<?> worker)
	{
		this.tasksRunning.add(1);
		if (worker instanceof Task<?>)
		{
			this.activeTasks.add((Task<?>) worker);
		}
	}

	/**
	 * Adds a task to the executor, if display is true then we add it to our tasks to display list which can be rendered with a task progress view
	 *
	 * @param task The task to execute
	 * @param display If the task should be added to the list to be displayed
	 * @param <T> The return type of the task
	 * @return A future representing this task's execution
	 */
	public <T> Future<?> addTask(Task<T> task, Boolean display)
	{
		if (display)
			this.activeDisplayedTasks.add(task);
		return super.addTask(task);
	}

	///
	/// Getters, but use read only
	///

	public Integer getTaskCount()
	{
		return this.tasksRunning.getValue();
	}

	public ReadOnlyIntegerProperty taskRunningProperty()
	{
		return this.tasksRunning.getReadOnlyProperty();
	}

	public ObservableList<Task<?>> getActiveTasks()
	{
		return this.activeTasks;
	}

	public ObservableList<Task<?>> getActiveDisplayedTasks()
	{
		return activeDisplayedTasks;
	}
}
