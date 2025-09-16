/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.jmal.star;

import static fr.jmmc.jmcs.network.http.Http.HTTP_RETRY_HANDLER;
import fr.jmmc.jmcs.util.UrlUtils;
import java.util.List;
import java.util.Set;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

/**
 *
 * @author bourgesl
 */
public final class GetStarResolveJob extends ResolverJob {

    /** GetStar separator (multiple identifier separator) */
    public static final String GETSTAR_SEPARATOR = ",";

    // members:
    /** result */
    private String _xml;

    /**
     * @param flags optional flags associated with the query
     * @param names list of queried identifiers
     * @param progressListener callback listener with progress
     * @param listener callback listener with results
     * @param responseHandler GetStar votable handler
     */
    GetStarResolveJob(final Set<String> flags, final List<String> names,
                      final StarResolverProgressListener progressListener,
                      final StarResolverListener<Object> listener) {
        super(flags, names, progressListener, listener);
        _xml = null;
    }

    @Override
    public String getResolverName() {
        return "JMMC GetStar";
    }

    @Override
    public Object getResolverResult() {
        return _xml;
    }

    @Override
    protected String buildQuery() {
        return buildQueryString(_names.toArray(EMPTY_STRING));
    }

    @Override
    protected HttpMethodBase buildHttpMethod(final String serviceURL, final String query) {
        final GetMethod method = new GetMethod(serviceURL + query);

        final HttpMethodParams httpMethodParams = method.getParams();
        // allow http retries (GET):
        httpMethodParams.setParameter(HttpMethodParams.RETRY_HANDLER, HTTP_RETRY_HANDLER);

        return method;
    }

    @Override
    protected boolean parseResponse(final String response) throws IllegalStateException {
        this._xml = response;
        return true;
    }

    @Override
    public boolean isErrorStatus() {
        return false;
    }

    public static String buildQueryString(final String... ids) {
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
            return UrlUtils.encode(starValue);
        }
        return null;
    }
}
