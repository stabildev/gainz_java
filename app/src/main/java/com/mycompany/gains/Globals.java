package com.mycompany.gains;

/**
 * Created by henri on 24.10.2015.
 */
public class Globals {
    private static Globals instance;

    // Global variables
    private int activeWorkoutId = -1;

    // Restrict constructor from being instantiated
    private Globals() {}

    public void setActiveWorkoutId(int id) {
        activeWorkoutId = id;
    }

    public int getActiveWorkoutId() {
        return activeWorkoutId;
    }

    public boolean isWorkoutActive() {
        return activeWorkoutId != -1;
    }

    public static synchronized Globals getInstance() {
        if (instance == null)
            instance = new Globals();

        return instance;
    }
}
