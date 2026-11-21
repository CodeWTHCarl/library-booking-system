package service;

/**
 * One exception type for everything that can go wrong in the service layer (broken rule,
 * bad input, database problem). The UI catches this and shows getMessage() in a dialog.
 */
public class LibraryException extends Exception {

    public LibraryException(String message) {
        super(message);
    }

    public LibraryException(String message, Throwable cause) {
        super(message, cause);
    }
}
