/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.jmal.star;

import static fr.jmmc.jmal.star.StarResolver.GETSTAR_ALLOW_SCENARIO;
import fr.jmmc.jmcs.network.http.HttpResult;
import fr.jmmc.jmcs.util.StringUtils;
import fr.jmmc.jmcs.util.UrlUtils;
import java.util.List;
import java.util.Set;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 *
 * @author bourgesl
 */
public final class GetStarResolveJob extends ResolverJob {

    /** GetStar separator (multiple identifier separator) */
    public static final String GETSTAR_SEPARATOR = ",";

    // members:
    /** result */
    private final GetStarResolverResult _result;

    /**
     * @param flags optional flags associated with the query
     * @param names list of queried identifiers
     * @param progressListener callback listener with progress
     * @param listener callback listener with results
     */
    GetStarResolveJob(final Set<String> flags, final List<String> names,
                      final StarResolverProgressListener progressListener,
                      final StarResolverListener<Object> listener) {
        super(flags, names, progressListener, listener);
        _result = new GetStarResolverResult(names);
    }

    @Override
    public String getResolverName() {
        return "JMMC GetStar";
    }

    @Override
    public Object getResolverResult() {
        return _result;
    }

    @Override
    protected String buildQuery() {
        final String queryParameters = ((_names.size() == 1) ? GETSTAR_ALLOW_SCENARIO : null);
        return buildQueryString(queryParameters, _names.toArray(EMPTY_STRING));
    }

    public static String buildQueryString(final String queryParameters, final String... ids) {
        if (ids != null && ids.length != 0) {
            final String starValue;
            if (ids.length == 1) {
                starValue = ids[0];
            } else {
                final StringBuilder sb = new StringBuilder(ids.length * 20);
                for (String id : ids) {
                    sb.append(id).append(GETSTAR_SEPARATOR);
                }
                sb.deleteCharAt(sb.length() - 1);
                starValue = sb.toString();
            }
            return (StringUtils.isEmpty(queryParameters))
                    ? UrlUtils.encode(starValue)
                    : UrlUtils.encode(starValue) + queryParameters;
        }
        return null;
    }

    @Override
    protected HttpMethodBase buildHttpMethod(final String serviceURL, final String query) {
        return new GetMethod(serviceURL + query);
    }

    @Override
    public boolean isErrorStatus() {
        return _result.isErrorStatus();
    }

    @Override
    protected void handleError(final StarResolverStatus status, final String errorMessage) {
        if (status == StarResolverStatus.ERROR_SERVER) {
            _result.setServerErrorMessage(errorMessage);
        } else {
            _result.setErrorMessage(status, errorMessage);
        }
        super.handleError(status, errorMessage);
    }

    @Override
    protected void parseResponse(final HttpResult httpResult) throws IllegalStateException {
        _logger.debug("GetStar raw response:\n{}", httpResult);

        final String response = (httpResult != null) ? httpResult.getResponse() : null;
        // If the response is null (when simbad server fails)
        if (response == null) {
            throw new IllegalStateException("No data for star(s) " + _result.getNames() + ", Simbad service may be off or unreachable.");
        }
        // If the response string is empty
        if (response.length() == 0) {
            throw new IllegalStateException("No data for star(s) " + _result.getNames() + ".");
        }
        if (!httpResult.isHttpResultOK()) {
            final String serverMessage = StringUtils.removeTags(response);
            _result.setServerErrorMessage("GetStar failed [" + httpResult.getHttpResultCode() + "]:\n" + serverMessage);
        }
        this._result.setXml(response);
    }

}
