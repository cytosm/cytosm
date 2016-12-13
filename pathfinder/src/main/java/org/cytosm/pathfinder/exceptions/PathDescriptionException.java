package org.cytosm.pathfinder.exceptions;

/***
 * Exception thrown when there's no possible gtop route that would satisfy the conditions.
 *
 *
 */
@SuppressWarnings("serial")
public class PathDescriptionException extends PathFinderException {

    /***
     * Route does not match gtop.
     *
     * @param notFoundInputHint additional information
     */
    public PathDescriptionException(final String notFoundInputHint) {
        super("The following hint set does not match anything on Gtop: " + notFoundInputHint);
    }

    /***
     * Route does not match gtop.
     *
     * @param message additional information
     * @param cause original exception
     */
    public PathDescriptionException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /***
     * Route does not match gtop.
     *
     * @param cause original exception.
     */
    public PathDescriptionException(final Throwable cause) {
        super(cause);
    }
}
