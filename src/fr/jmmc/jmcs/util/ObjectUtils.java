/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.jmcs.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The fr.jmmc.jmcs.util.ObjectUtils class is related to Object handling (equals, clone, copy, toString variants)
 * 
 * @author Laurent BOURGES.
 */
public final class ObjectUtils {

    /**
     * Forbidden constructor
     */
    private ObjectUtils() {
        super();
    }

    /* equals helper methods */
    /**
     * Utility method for <code>equals()</code> methods.
     *
     * @param o1 one object
     * @param o2 another object
     *
     * @return <code>true</code> if they're both <code>null</code> or both equal
     */
    public static boolean areEquals(final Object o1, final Object o2) {
        return (o1 == o2) || (o1 != null && o1.equals(o2));
    }

    /* copy / clone helper methods */
    /**
     * Return a deep "copy" of the list of objects (recursive call to clone() on each object instance)
     * 
     * @param <K> PublicCloneable child class
     * @param list list of objects to clone
     * @return deep "copy" of the list
     */
    @SuppressWarnings("unchecked")
    public static <K extends PublicCloneable> List<K> deepCopyList(final List<K> list) {
        if (list != null) {
            final List<K> newList = new ArrayList<K>(list.size());
            for (K o : list) {
                newList.add((K) o.clone());
            }
            return newList;
        }
        return null;
    }

    /**
     * Return a simple "copy" of the list of objects without cloning each object instance
     * 
     * @param <K> any type
     * @param list list of objects to clone
     * @return deep "copy" of the list
     */
    @SuppressWarnings("unchecked")
    public static <K> List<K> copyList(final List<K> list) {
        if (list != null) {
            final List<K> newList = new ArrayList<K>(list.size());
            for (K o : list) {
                newList.add(o);
            }
            return newList;
        }
        return null;
    }

    /* toString helper methods */
    /**
     * toString(object) implementation using ToStringable interface if possible
     * @param sb string builder to append to
     * @param full true to get complete information; false to get main information (shorter)
     * @param o any object (may implement ToStringable) 
     */
    public static void toString(final StringBuilder sb, final boolean full, final Object o) {
        if (o == null) {
            sb.append("null");
        } else if (o instanceof ToStringable) {
            ((ToStringable) o).toString(sb, full);
        } else {
            sb.append(o.toString());
        }
    }

    /**
     * toString(collection) implementation using ToStringable interface if possible
     * @param sb string builder to append to
     * @param full true to get complete information; false to get main information (shorter)
     * @param collection collection of any objects to represent
     */
    public static void toString(final StringBuilder sb, final boolean full, final Collection<?> collection) {
        if (collection == null) {
            sb.append("null");
        } else if (collection.isEmpty()) {
            sb.append("[]");
        } else {
            sb.append('[');
            for (Object o : collection) {
                toString(sb, full, o);
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
            sb.append(']');
        }
    }

    /**
     * toString(map) implementation using ToStringable interface if possible
     * @param sb string builder to append to
     * @param full true to get complete information; false to get main information (shorter)
     * @param map collection of any objects to represent
     */
    public static void toString(final StringBuilder sb, final boolean full, final Map<?, ?> map) {
        if (map == null) {
            sb.append("null");
        } else if (map.isEmpty()) {
            sb.append("[]");
        } else {
            sb.append('[');
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                toString(sb, full, entry.getKey());
                sb.append(" = ");
                toString(sb, full, entry.getValue());
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
            sb.append(']');
        }
    }

    /* identity string helper methods */
    /**
     * Return the string representation "<simple class name>#<hashCode>"
     * @param o any object
     * @return "<class name>#<hashCode>"
     */
    public static String getObjectInfo(final Object o) {
        if (o == null) {
            return "null";
        }
        final StringBuilder sb = new StringBuilder(32);
        getObjectInfo(sb, o);
        return sb.toString();
    }

    /**
     * Append the string representation "<simple class name>#<hashCode>"
     * @param sb string builder to append to
     * @param o any object
     */
    public static void getObjectInfo(final StringBuilder sb, final Object o) {
        if (o == null) {
            sb.append("null");
        } else {
            sb.append(o.getClass().getSimpleName()).append('@').append(Integer.toHexString(System.identityHashCode(o)));
        }
    }

    /**
     * Return the string representation "{<simple class name>#<hashCode>, ...}"
     * @param col any collection
     * @return "{<simple class name>#<hashCode>, ...}"
     */
    public static String getObjectInfo(final Collection<?> col) {
        if (col == null) {
            return "null";
        }
        if (col.isEmpty()) {
            return "[]";
        }
        final StringBuilder sb = new StringBuilder(256);
        getObjectInfo(sb, col);
        return sb.toString();
    }

    /**
     * Return the string representation "{<simple class name>#<hashCode>, ...}"
     * @param sb string builder to append to
     * @param col any collection
     */
    public static void getObjectInfo(final StringBuilder sb, final Collection<?> col) {
        if (col == null) {
            sb.append("null");
        } else if (col.isEmpty()) {
            sb.append("[]");
        } else {
            sb.append('[');
            for (Object o : col) {
                getObjectInfo(sb, o);
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
            sb.append(']');
        }
    }

    /**
     * Return the string representation "<full class name>#<hashCode>"
     * @param o any object
     * @return "<full class name>#<hashCode>"
     */
    public static String getFullObjectInfo(final Object o) {
        if (o == null) {
            return "null";
        }
        final StringBuilder sb = new StringBuilder(64);
        getFullObjectInfo(sb, o);
        return sb.toString();
    }

    /**
     * Append the string representation "<full class name>#<hashCode>"
     * @param sb string builder to append to
     * @param o any object
     */
    public static void getFullObjectInfo(final StringBuilder sb, final Object o) {
        if (o == null) {
            sb.append("null");
        } else {
            sb.append(o.getClass().getName()).append('@').append(Integer.toHexString(System.identityHashCode(o)));
        }
    }
}