package com.mycompany.gainz.Database.Model;

import android.support.annotation.NonNull;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;


public class Set implements Comparable<Set> {

    int _id;
    int reps;
    int weight;
    int rest;
    boolean isDone = false;
    DecimalFormat df = new DecimalFormat("#.##");

    int superset, row;
    Workout workout;
    Exercise exercise;

    int posInRow;

    public Set(Workout workout, int superset, int row, Exercise exercise) {
        this.workout = workout;
        this.superset = superset;
        this.row = row;
        this.exercise = exercise;

        this.workout.setsInRows.put(this.row, this);

        this.posInRow = this.workout.setsInRows.get(this.row).size();
    }

    public void set_id(int _id) {
        this._id = _id;
    }


    public void setReps(int reps) {
        this.reps = reps;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public void setRest(int rest) {
        this.rest = rest;
    }

    public void setIsDone(boolean isDone) {
        this.isDone = isDone;
    }

    public void setWorkout(Workout workout) {
        this.workout = workout;
    }


    public int get_id() {
        return _id;
    }

    public int getReps() {
        return reps;
    }

    public String getFormattedWeight() {
        return weight == 0 ? "0" : new BigDecimal(weight).divide(new BigDecimal(1000), 2, BigDecimal.ROUND_HALF_UP)
                .stripTrailingZeros().toPlainString();
    }

    public int getWeight() {
        return weight;
    }

    public int getRest() {
        return rest;
    }

    public int getSuperset() {
        return superset;
    }

    public int getRow() {
        return row;
    }

    public boolean isDone() {
        return isDone;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public Workout getWorkout() {
        return workout;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public void setSuperset(int superset) {
        this.superset = superset;
    }

    public void setFormattedWeight(String weight) {
        this. weight = new BigDecimal(weight).movePointRight(3).intValue();
    }

    @Override
    public int compareTo(@NonNull Set another) {
        if (this._id == another._id)
            return 0;
        else
            return this._id > another._id ? -1 : 1;
    }

    public void setPosInRow(int posInRow) {
        this.posInRow = posInRow;
    }

    public int posInRow() {
        return posInRow;
    }
}
