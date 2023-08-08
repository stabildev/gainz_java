package com.mycompany.gains.Data.Model;

import android.support.annotation.NonNull;
import android.util.Log;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by Klee on 09.07.2015.
 */
public class Set implements Comparable<Set> {
    int databaseId;
    int reps;
    int weight;
    boolean isDone = false;

    Row mRow;

    public static String formatWeight(int weight, boolean stripZeros) {
        BigDecimal kgs = new BigDecimal(weight).divide(new BigDecimal(1000), 2, BigDecimal.ROUND_HALF_UP);

        if (!stripZeros)
            return kgs.toPlainString();
        else {
            String temp = kgs.stripTrailingZeros().toPlainString();
            // fix striptrailingzeros bug
            if (temp.length() > 3 && temp.substring(temp.length()-2, temp.length()).equals("00"))
                temp = temp.substring(0,temp.length()-3);

            return  temp;
        }
    }

    public int getPosition() {
        return mRow.getSets().indexOf(this);
    }

    public void setWeightFormatted(String weight) {
        this.weight = normalizeWeight(weight);
    }

    public static int normalizeWeight(String weight) {
        return new BigDecimal(weight).movePointRight(3).intValue();
    }

    public String getWeightFormatted(boolean stripZeros) {
        return formatWeight(weight, stripZeros);
    }

    public void setRow(Row row) {
        mRow = row;
    }

    public Row getRow() {
        return mRow;
    }

    public void setDatabaseId(int databaseId) {
        this.databaseId = databaseId;
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public void setReps(int reps) {
        this.reps = reps;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Deprecated
    public void setRest(int rest) {
        Log.w("Set", "Deprecated method setRest() called on set");
    }

    public void setIsDone(boolean isDone) {
        this.isDone = isDone;
    }

    public int getReps() {
        return reps;
    }

    public int getWeight() {
        return weight;
    }

    @Deprecated
    public int getRest() {
        Log.w("Set", "Deprecated method getRest() called on set");
        if (mRow != null)
            return mRow.getRest();
        else
            return 60;
    }

    public boolean isDone() {
        return isDone;
    }

    public String getCoordinates() {
        return mRow.getSuperset().getPosition() + "." + mRow.getPosition() + "." + getPosition();
    }

    public boolean hasNext() {
        List<Set> allSets = mRow.getSuperset().getWorkout().getAllSets();
        return allSets.indexOf(this) < allSets.size()-2;
    }

    public Set() {
    }

    public Set(Set another) {
        this.reps = another.getReps();
        this.weight = another.getWeight();
        this.isDone = another.isDone();
        this.mRow = another.getRow();
    }

    @Override
    public int compareTo(@NonNull Set another) {
        return this.databaseId > another.databaseId ? 1 :
                this.databaseId < another.databaseId ? -1 : 0;
    }
}
