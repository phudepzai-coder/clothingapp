package store.exceptions;

/** Thrown when a duplicate Product/Customer/Transaction ID is used (BR1, BR2). */
public class DuplicateIdException extends Exception {
    public DuplicateIdException(String message) {
        super(message);
    }
}
