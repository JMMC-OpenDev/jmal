/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.jmal.star;

import fr.jmmc.jmcs.util.StringUtils;
import java.util.List;

/**
 * This class gathers the results of one StarResolver query: 
 * - queried names
 * - status (+ optional error message)
 * - results as String;
 * @author bourgesl
 */
public final class GetStarResolverResult extends StarResolverResult {

    /** GetStar response */
    private String _xml = null;

    /**
     * Protected Constructor
     * @param names list of queried identifiers
     */
    GetStarResolverResult(final List<String> names) {
        super(names);
    }

    /**
     * @return true if no result
     */
    public boolean isEmpty() {
        return StringUtils.isEmpty(_xml);
    }

    public String getXml() {
        return _xml;
    }

    public void setXml(final String xml) {
        this._xml = xml;
    }

    @Override
    public String toString() {
        return super.toString() + ", xml: " + _xml;
    }
}
