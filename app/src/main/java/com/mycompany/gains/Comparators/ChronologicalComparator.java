package com.mycompany.gains.Comparators;

import com.mycompany.gains.Data.Model.Set;

import java.util.Comparator;

public class ChronologicalComparator implements Comparator<Set> {
    @Override public int compare(Set a, Set b) {
        return a.getRow().getSuperset().getPosition() > b.getRow().getSuperset().getPosition() ? 1
                : a.getRow().getSuperset().getPosition() < b.getRow().getSuperset().getPosition() ? -1 :
                a.getPosition() > b.getPosition() ? 1 : a.getPosition() < b.getPosition() ? -1 :
                        a.getRow().getPosition() > b.getRow().getPosition() ? 1
                                : a.getRow().getPosition() < b.getRow().getPosition() ? -1 : 0;
    }
}