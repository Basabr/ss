package it.polito.extgol;

/**
 * Signals error conditions specific to the Extended Game of Life.
 *
 * Used to indicate invalid configurations or unexpected states
 * during board evolution or game setup.
 */
public class ExtendedGameOfLifeException extends RuntimeException {

    public ExtendedGameOfLifeException(String message) {
        super(message);
    }

    public ExtendedGameOfLifeException(String message, Throwable cause) {
        super(message, cause);
    }
}
