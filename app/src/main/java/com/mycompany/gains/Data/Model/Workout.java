package com.mycompany.gains.Data.Model;

import com.mycompany.gains.Comparators.ChronologicalComparator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;


public class Workout {
    int databaseId;
    int routineId = -1;
    Calendar date = new GregorianCalendar();
    String name = "";
    String note = "";
    boolean isRoutine = false;
    boolean isStarred = false;
    long duration;

    List<Superset> mSupersets = new ArrayList<>();

    public Workout() {
        mSupersets = new ArrayList<>();
    }

    public void setDatabaseId(int databaseId) {
        this.databaseId = databaseId;
    }

    public void setDate(Calendar date) {
        this.date = date;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setRoutineId(int routineId) {
        this.routineId = routineId;
    }

    public void addSuperset(Superset superset) {
        superset.setWorkout(this);
        mSupersets.add(superset);
    }

    public void addSuperset(int position, Superset superset) {
        superset.setWorkout(this);
        mSupersets.add(position, superset);
    }

    public void setIsRoutine(boolean isRoutine) {
        this.isRoutine = isRoutine;
    }

    public void setIsStarred(boolean isStarred) {
        this.isStarred = isStarred;
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public Calendar getDate() {
        return date;
    }

    public long getDuration() {
        return duration;
    }

    public String getName() {
        return name;
    }

    public int getRoutineId() {
        return routineId;
    }

    public String getNote() {
        return note;
    }

    public boolean isRoutine() {
        return isRoutine;
    }

    public boolean isStarred() {
        return isStarred;
    }

    public Superset getSuperset(int position) {
        return mSupersets.get(position);
    }

    public List<Superset> getSupersets() {
        return new ArrayList<>(mSupersets);
    }

    public int getSupersetCount() {
        return mSupersets.size();
    }

    public Superset removeSuperset(int position) {
        return mSupersets.remove(position);
    }

    public void moveSuperset(int fromPosition, int toPosition) {
        final Superset superset = mSupersets.remove(fromPosition);
        mSupersets.add(toPosition, superset);
    }

    public void moveRow(int fromSuperset, int fromRow, int toSuperset, int toRow, boolean newSuperset) {
        Superset ssFrom = mSupersets.get(fromSuperset);
        Superset ssTo;
        if (newSuperset) {
            ssTo = new Superset();
            addSuperset(toSuperset, ssTo);
        } else {
            ssTo = mSupersets.get(toSuperset);
            if (fromSuperset != toSuperset)
                moveSuperset(fromSuperset, toSuperset);
        }
        ssTo.addRow(toRow, ssFrom.removeRow(fromRow));
    }

    public void mergeSupersets(List<Integer> positions) {
        if (positions.size() < 2)
            return;

        Collections.sort(positions);

        final Superset first = mSupersets.get(positions.get(0));

        List<Superset> others = new ArrayList<>(positions.size()-1);

        for (int i = 1; i < positions.size(); i++) {
            others.add(mSupersets.get(positions.get(i)));
        }

        // iterate through rows in reverse order to get all rows
        for (Superset s : others) {
            for (Row r : s.getRows()) {
                first.addRow(s.removeRow(r.getPosition()));
            }
            mSupersets.remove(s.getPosition());
        }
    }

    public void splitSuperset(int supersetPosition) {
        final Superset superset = mSupersets.get(supersetPosition);
        // abort if less than two rows in superset
        if (superset.getRowCount() < 2)
            return;

        // for all but the first row(s)
        for (int i = superset.getRowCount() -1; i > 0; i--) {
            Superset newSuperset = new Superset();
            newSuperset.addRow(superset.removeRow(i));
            this.addSuperset(supersetPosition + 1, newSuperset);
        }
    }

    public void separateRowFromSuperset(int supersetPosition, int rowPosition) {
        addSuperset(supersetPosition+1, new Superset());
        mSupersets.get(supersetPosition+1).addRow(
                mSupersets.get(supersetPosition).removeRow(rowPosition)
        );
    }

    public List<Set> getAllSets() {
        List<Set> allSets = new ArrayList<>();

        for (Superset s : mSupersets)
            for (Row r : s.getRows())
                allSets.addAll(r.getSets());

        Collections.sort(allSets, new ChronologicalComparator());
        return allSets;
    }

    public Row getRowFromCoordinates(String rowCoordinates) {
        String[] split = rowCoordinates.split("\\.");
        int superset = Integer.parseInt(split[0]);
        int row = Integer.parseInt(split[1]);
        if (mSupersets.size() > superset) {
            if (mSupersets.get(superset).getRowCount() > row)
                return mSupersets.get(superset).getRow(row);
        }
        return null;
    }
}