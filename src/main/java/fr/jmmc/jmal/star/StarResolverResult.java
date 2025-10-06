/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.jmal.star;

import fr.jmmc.jmcs.util.CollectionUtils;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author bourgesl
 */
public class StarResolverResult {

    /** Logger */
    protected static final Logger _logger = LoggerFactory.getLogger(StarResolverResult.class.getName());
    /* members */
    /** list of queried identifiers */
    protected final List<String> _names;
    /** status */
    protected StarResolverStatus _status = StarResolverStatus.IN_PROGRESS;
    /** (optional) server error message */
    protected String _serverErrorMessage = null;
    /** (optional) error message */
    protected String _errorMessage = null;
    /** flag indicating that a queried identifier got multiple matches */
    protected boolean _multipleMatches = false;

    StarResolverResult(final List<String> names) {
        _names = Collections.unmodifiableList(names);
    }

    /**
     * @return list of queried identifiers (read only)
     */
    public final List<String> getNames() {
        return _names;
    }

    /**
     * @return true if any error occured
     */
    public final boolean isErrorStatus() {
        return (_status != StarResolverStatus.IN_PROGRESS) && (_status != StarResolverStatus.OK);
    }

    /**
     * @return status
     */
    public final StarResolverStatus getStatus() {
        return _status;
    }

    /**
     * Define the status (but do not override any error status)
     * @param status status
     */
    final void setStatus(final StarResolverStatus status) {
        if (!isErrorStatus()) {
            this._status = status;
        }
    }

    /**
     * @return (optional) server error message
     */
    public final String getServerErrorMessage() {
        return _serverErrorMessage;
    }

    /**
     * Set the status to ERROR_SERVER and the server error message
     * @param serverErrorMessage server error message
     */
    public final void setServerErrorMessage(String serverErrorMessage) {
        setStatus(StarResolverStatus.ERROR_SERVER);
        this._serverErrorMessage = serverErrorMessage;
    }

    /**
     * @return (optional) error message
     */
    public final String getErrorMessage() {
        return _errorMessage;
    }

    /**
     * Set the status to the given status (error expected) and the error message
     * @param status any status (error expected)
     * @param errorMessage error message
     */
    final void setErrorMessage(final StarResolverStatus status, final String errorMessage) {
        setStatus(status);
        this._errorMessage = errorMessage;
    }

    /**
     * @return flag indicating that queried identifier(s) have multiple matches
     */
    public final boolean isMultipleMatches() {
        return _multipleMatches;
    }

    @Override
    public String toString() {
        return "status: " + _status
                + ((_serverErrorMessage != null) ? (", server error: \"" + _serverErrorMessage + "\"") : "")
                + ((_errorMessage != null) ? (", error: \"" + _errorMessage + "\"") : "")
                + ", ids: " + CollectionUtils.toString(_names)
                + ", isMultipleMatches: " + _multipleMatches;
    }

}
