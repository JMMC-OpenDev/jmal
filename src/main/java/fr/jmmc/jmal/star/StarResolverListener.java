/** *****************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ***************************************************************************** */
package fr.jmmc.jmal.star;

/**
 * Listener called with resolver results (error, star)
 * @param <K> star resolver result class
 * @author bourgesl
 */
public interface StarResolverListener<K> {

    /**
     * Handle the star resolver result as String (raw http response) or StarResolverResult instance (status, error messages, stars) ...
     * @param result star resolver result
     */
    public void handleResult(final K result);

}
