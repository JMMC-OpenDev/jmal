/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.jmal.star;

/**
 * StarResolverProgressListener called with progress messages
 * @author bourgesl
 */
public interface StarResolverProgressListener {

    /**
     * Handle the given progress message (using server mirror, error, done) ...
     * @param message progress message
     */
    public void handleProgressMessage(final String message);

}
