package com.mycompany.gains.Data.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Klee on 09.07.2015.
 */
public class Row {
    private int databaseId;
    private String note = "";
    private Exercise exercise;
    private int rest;

    Superset mSuperset;
    List<Set> mSets = new ArrayList<>();

    public Row() {
        mSets = new ArrayList<>();
    }

    public Row(Exercise exercise) {
        this.exercise = exercise;
    }

    public int getSetCount() {
        return mSets.size();
    }

    public int getPosition() {
        return mSuperset.getRows().indexOf(this);
    }

    public String getCoordinates() {
        return mSuperset.getPosition() + "." + getPosition();
    }

    public int getRest() {
        return rest;
    }

    public Set getSet(int position) {
        return mSets.get(position);
    }

    public List<Set> getSets() {
        return new ArrayList<>(mSets);
    }

    public void addSet(Set set) {
        set.setRow(this);
        mSets.add(set);
    }

    public Set removeSet(int position) {
        return mSets.remove(position);
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getNote() {
        return note;
    }

    public void setSuperset(Superset superset) {
        mSuperset = superset;
    }

    public void setRest(int rest) {
        this.rest = rest;
    }

    public void setExercise(Exercise exercise) {
        this.exercise = exercise;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public Superset getSuperset() {
        return mSuperset;
    }

    public void setDatabaseId(int databaseId) {
        this.databaseId = databaseId;
    }

    public int getDatabaseId() {
        return databaseId;
    }
}
