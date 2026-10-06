package store.exceptions;

/** Thrown when the quantity sold exceeds available stock (BR9). */
public class InsufficientStockException extends Exception {
    public InsufficientStockException(String message) {
        super(message);
    }
}
