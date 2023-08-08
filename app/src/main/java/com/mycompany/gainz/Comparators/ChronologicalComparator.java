package com.mycompany.gainz.Comparators;

import com.mycompany.gainz.Database.Model.Set;

import java.util.Comparator;

public class ChronologicalComparator implements Comparator<Set> {
    @Override public int compare(Set a, Set b) {
        return a.getSuperset() > b.getSuperset() ? 1 : a.getSuperset() < b.getSuperset() ? -1 :
                a.posInRow() > b.posInRow() ? 1 : a.posInRow() < b.posInRow() ? -1 :
                        a.getRow() > b.getRow() ? 1 : a.getRow() < b.getRow() ? -1 : 0;
    }
}