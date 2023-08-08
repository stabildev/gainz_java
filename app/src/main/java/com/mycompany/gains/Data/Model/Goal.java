package com.mycompany.gains.Data.Model;

/**
 * Created by Klee on 06.09.2015.
 */
public class Goal {
    public int sets = 1;
    public int reps;
    public int weight;
    public int rest;

    public Exercise exercise;
    public String note = "";

    public Goal() {
    }

    public Goal(Exercise exercise) {
        this.exercise = exercise;
    }
}
