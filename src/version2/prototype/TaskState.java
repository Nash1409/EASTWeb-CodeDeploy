package version2.prototype;

/**
 * Defined states the system tasks can be in.
 *
 * @author michael.devos
 *
 */

public enum TaskState {
    DELETING,   // Describes an object that is destroying itself, releasing its resources, and removing its stored progress
    RUNNING,    // Describes an object that's currently executing on an active thread
    STARTED,    // Describes an object that has been started but is waiting to be executed on an active thread  -- currently not used
    STARTING,   // Describes an object that is being enabled and moved into a state awaiting execution by an active thread
    STOPPED,    // Describes an object that is is disabled and thus no longer allowing an active thread to execute it
    STOPPING    // Describes an object that is in the process of disabling itself to prevent an active thread from executing it
}
