/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.jmal.star;

/**
 * Listener called with resolver results (error, star)
 * @author bourgesl
 * @param <StarResolverResult> StarResolverResult type
 */
public interface StarResolverListener<StarResolverResult> {

    /**
     * Handle the star resolver result as String (raw http response) or StarResolverResult instance (status, error messages, stars) ...
     * @param result star resolver result
     */
    public void handleResult(final StarResolverResult result);

}
