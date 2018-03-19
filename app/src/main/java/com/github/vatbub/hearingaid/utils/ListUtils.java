package com.github.vatbub.hearingaid.utils;

import java.util.List;

/**
 * Created by frede on 19.03.2018.
 */

public class ListUtils {
    private static ListUtils instance;

    private ListUtils() {
    }

    public static ListUtils getInstance() {
        if (instance == null)
            instance = new ListUtils();

        return instance;
    }

    public <T> boolean isListEqualsWithoutOrder(List<T> l1, List<T> l2) {
        return l1.containsAll(l2) && l2.containsAll(l1);
    }

    /**
     * Finds out what elements were swapped in a list. Only two elements must be swapped in the supplied lists. If more than two elements or no elements have been swapped, an IllegalArgumentException will be thrown.
     *
     * @param beforeSwap The list in its state before the swap
     * @param afterSwap  The list in its state after the swap. Must contain the same elements as beforeSwap.
     * @param <T>        The list type
     * @return AN array of two {@code int}s. Those ints represent the indices of the swapped elements.
     */
    public <T> int[] findSwappedIndices(List<T> beforeSwap, List<T> afterSwap) {
        if (!isListEqualsWithoutOrder(beforeSwap, afterSwap))
            throw new IllegalArgumentException("beforeSwap and afterSwap must contain the same elements");

        int[] res = new int[2];
        int diffCount = 0;
        for (int i = 0; i < beforeSwap.size(); i++) {
            if (beforeSwap.get(i).equals(afterSwap.get(i))) {
                if (diffCount > 2)
                    throw new IllegalArgumentException("The lists differ in more than two element");

                res[diffCount] = i;
                diffCount++;
            }
        }

        if (diffCount < 2)
            throw new IllegalArgumentException("The lists differ in less than two elements");

        return res;
    }
}
