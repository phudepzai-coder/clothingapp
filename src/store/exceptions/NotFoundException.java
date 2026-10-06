package store.exceptions;

/** Thrown when a Product/Customer/Transaction ID does not exist (BR7). */
public class NotFoundException extends Exception {
    public NotFoundException(String message) {
        super(message);
    }
}
