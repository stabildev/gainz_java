package com.mycompany.gains.Data.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Klee on 09.07.2015.
 */
public class Superset {
    List<Row> mRows = new ArrayList<>();
    Workout mWorkout;

    public Superset() {

    }

    public void addRow(int position, Row row) {
        row.setSuperset(this);
        mRows.add(position, row);
    }

    public void addRow(Row row) {
        row.setSuperset(this);
        mRows.add(row);
    }

    public int getPosition() {
        return mWorkout.getSupersets().indexOf(this);
    }

    public Row removeRow(int position) {
        return mRows.remove(position);
    }

    public int getRowCount() {
        return mRows.size();
    }

    public Row getRow(int position) {
        return mRows.get(position);
    }

    public List<Row> getRows() {
        return new ArrayList<>(mRows);
    }

    public void moveRow(int fromPosition, int toPosition) {
        final Row row = mRows.remove(fromPosition);
        mRows.add(toPosition, row);
    }

    public void setWorkout(Workout workout) {
        mWorkout = workout;
    }

    public Workout getWorkout() {
        return mWorkout;
    }
}
