package store.exceptions;

/** Thrown when user input violates validation or business rules (BR17). */
public class InvalidInputException extends Exception {
    public InvalidInputException(String message) {
        super(message);
    }
}
