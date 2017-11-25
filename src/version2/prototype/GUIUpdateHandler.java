package version2.prototype;


/**
 * Used to define an object for handling updating the GUI with a {@link version2.Scheduler#SchedulerStatus SchedulerStatus} object. Synchronously called
 * by EASTWebManager's background thread when state information tracked in a {@link version2.Scheduler#SchedulerStatus SchedulerStatus} has changed.
 *
 * @author michael.devos
 *
 */
public interface GUIUpdateHandler {
    /**
     * Executes the main work this handler will do for updating the GUI. Called by {@link version2#EASTWebManager EASTWebManager} to update GUI when a
     * change has been noted.
     */
    public void run();
}
