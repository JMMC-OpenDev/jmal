/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.jmal.star;

import fr.jmmc.jmcs.util.CollectionUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class gathers the results of one StarResolver query: 
 * - queried names
 * - status (+ optional error message)
 * - results as Map&lt;identifier, List&lt;Star&gt;&gt;
 * @author bourgesl
 */
public final class StarListResolverResult extends StarResolverResult {

    /** map of Star */
    private Map<String, List<Star>> _starMap = null;
    /** list of queried identifier(s) having multiple matches */
    protected List<String> _multNames = null;

    /**
     * Protected Constructor
     * @param names list of queried identifiers
     */
    StarListResolverResult(final List<String> names) {
        super(names);
    }

    /**
     * @return true if no result
     */
    public boolean isEmpty() {
        return CollectionUtils.isEmpty(_starMap);
    }

    /**
     * @return the single star corresponding to the single queried name (single match); null otherwise
     */
    public Star getSingleStar() {
        if (_names != null && _names.size() == 1) {
            return getSingleStar(_names.get(0));
        }
        return null;
    }

    /**
     * @param name queried identifier present in getNames()
     * @return the single star corresponding to the given name (single match); null otherwise
     */
    public Star getSingleStar(final String name) {
        final List<Star> starList = getStars(name);
        // Check for a single match: bad identifier = 0 match; multiple matches > 1
        if ((starList != null) && starList.size() == 1) {
            return starList.get(0);
        }
        return null;
    }

    /**
     * @param name queried identifier present in getNames()
     * @return the list of stars corresponding to the given name (multiple matches)
     */
    public List<Star> getStars(final String name) {
        if (_starMap == null) {
            return Collections.emptyList();
        }
        return _starMap.get(name);
    }

    /**
     * Add a Star instance corresponding to the queried name 
     * @param name queried identifier present in getNames()
     * @param star Star instance
     */
    void addStar(final String name, final Star star) {
        if (_starMap == null) {
            _starMap = new HashMap<String, List<Star>>(_names.size());
        }
        List<Star> starList = _starMap.get(name);
        if (starList == null) {
            starList = new ArrayList<Star>(2);
            _starMap.put(name, starList);
        } else {
            _multipleMatches = true;
        }
        _logger.debug("adding star for name='{}':\n{}", name, star);
        starList.add(star);
    }

    /**
     * @return list of queried identifier(s) having multiple matches
     */
    public List<String> getNamesForMultipleMatches() {
        List<String> multNames = _multNames;
        if (multNames != null) {
            return multNames;
        }
        if (!_multipleMatches) {
            multNames = Collections.emptyList();
        } else {
            multNames = new ArrayList<String>(_names.size());
            for (String name : _names) {
                List<Star> starList = _starMap.get(name);
                if (starList != null && starList.size() > 1) {
                    multNames.add(name);
                }
            }
        }
        _multNames = multNames;
        return multNames;
    }

    @Override
    public String toString() {
        return super.toString() + ", star map: " + CollectionUtils.toString(_starMap);
    }
}
