/**
 *
 */
package version2.prototype.download;

/**
 * @author michael.devos
 *
 */
@SuppressWarnings("serial")
public class RegistrationException extends Exception {

    /**
     * Constructs a new RegistrationException with null as its detail message. The cause is not initialized, and may subsequently be initialized by a call to Throwable.initCause(java.lang.Throwable).
     */
    public RegistrationException() {
        super();
    }

    /**
     * Constructs a new RegistrationException with the specified detail message. The cause is not initialized, and may subsequently be initialized by a call to Throwable.initCause(java.lang.Throwable).
     * @param message  - the detail message.cause. (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public RegistrationException(String message) {
        super(message);
    }

    /**
     * Constructs a new RegistrationException with the specified cause and a detail message of (cause==null ? null : cause.toString()) (which typically contains the class and detail message of cause).
     * This constructor is useful for exceptions that are little more than wrappers for other throwables (for example, PrivilegedActionException).
     * @param cause  - the cause.
     */
    public RegistrationException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new RegistrationException with the specified detail message and cause. Note that the detail message associated with cause is not automatically incorporated in this exception's detail message.
     * @param message  - the detail message.cause. (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
     * @param cause  - the cause.
     */
    public RegistrationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new RegistrationException with the specified detail message, cause, suppression enabled or disabled, and writable stack trace enabled or disabled.
     * @param message  - the detail message.cause. (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
     * @param cause  - the cause.
     * @param enableSuppression  - whether or not suppression is enabled or disabled.
     * @param writableStackTrace  - whether or not the stack trace should be writable.
     */
    public RegistrationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
