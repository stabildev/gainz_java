package com.mycompany.gains.Data.Database;

import com.mycompany.gains.Data.Model.Exercise;
import com.mycompany.gains.Data.Model.Row;
import com.mycompany.gains.Data.Model.Set;
import com.mycompany.gains.Data.Model.Superset;
import com.mycompany.gains.Data.Model.Workout;

public class DummyData {
    DatabaseHelper db;

    Exercise inclineBench, inclineFly, chestPress, pullUp, chinUp, machineRow, reverseFly, press;
    Exercise lateralRaise, shrug, scottCurl, frenchPress, abWheelRollout, kneeRaise, sideClimber;
    Exercise sideBend, squat, legPress, jumpingLunge, calfRaise, legExtension, legCurl, militaryPress;
    
    Workout pushRoutine, pullRoutine, coreRoutine, legsRoutine;
    
    public DummyData(DatabaseHelper db) {
        this.db = db;
    }
    
    public void createExercises() {
        // Chest
        inclineBench = new Exercise("Incline Bench Press","Chest", 2500);
        db.addExercise(inclineBench);

        inclineFly = new Exercise("Incline Flies", "Chest", 1000);
        db.addExercise(inclineFly);
        
        chestPress = new Exercise("Chest Press", "Chest", 5000);
        db.addExercise(chestPress);
        
        // Back
        pullUp = new Exercise("Pull-up", "Back");
        db.addExercise(pullUp);
        
        chinUp = new Exercise("Chin-up", "Back, Arms");
        db.addExercise(chinUp);
        
        machineRow = new Exercise("Machine Rows", "Back", 5000);
        db.addExercise(machineRow);

        // Shoulders
        reverseFly = new Exercise("Reverse Flies", "Shoulders, Back", 1000);
        db.addExercise(reverseFly);
        
        press = new Exercise("Presses", "Shoulders", 1000);
        db.addExercise(press);
        
        lateralRaise = new Exercise("Lateral Raise", "Shoulders", 1000);
        db.addExercise(lateralRaise);
        
        shrug = new Exercise("Shrugs", "Shoulders", 2500);
        db.addExercise(shrug);
        
        // Arms
        scottCurl = new Exercise("Scott Curls", "Arms", 1000);
        db.addExercise(scottCurl);

        frenchPress = new Exercise("French Press", "Arms", 2500);
        db.addExercise(frenchPress);

        
        // Core
        abWheelRollout = new Exercise("Ab Wheel Rollout", "Core");
        db.addExercise(abWheelRollout);

        kneeRaise = new Exercise("Knee Raise", "Core");
        db.addExercise(kneeRaise);
        
        sideClimber = new Exercise("Side Climbers", "Core");
        db.addExercise(sideClimber);
        
        sideBend = new Exercise("Side Bend", "Core", 2500);
        db.addExercise(sideBend);

        // Legs
        squat = new Exercise("Squat", "Legs", 5000);
        db.addExercise(squat);
        
        legPress = new Exercise("Leg Press", "Legs", 10000);
        db.addExercise(legPress);
        
        jumpingLunge = new Exercise("Jumping Lunges", "Legs", 2500);
        db.addExercise(jumpingLunge);
        
        calfRaise = new Exercise("Calf Raise", "Legs", 5000);
        db.addExercise(calfRaise);
        
        legExtension = new Exercise("Leg Extensions", "Legs", 2500);
        db.addExercise(legExtension);
        
        legCurl = new Exercise("Leg Curl", "Legs", 2500);
        db.addExercise(legCurl);

        // Recently added
        militaryPress = new Exercise("Military Press", "Shoulders", 2500);
        db.addExercise(militaryPress);
    }
    public void createRoutines() {
        // PUSH
        pushRoutine = new Workout();
        pushRoutine.setIsRoutine(true);
        pushRoutine.setName("Push");

        Superset superset;
        Row row;

        superset = new Superset();
        row = new Row(inclineBench);
        superset.addRow(row);
        row.addSet(new Set());
        pushRoutine.addSuperset(superset);

        superset = new Superset();
        row = new Row(inclineFly);
        superset.addRow(row);
        row.addSet(new Set());
        pushRoutine.addSuperset(superset);

        superset = new Superset();
        row = new Row(shrug);
        superset.addRow(row);
        row.addSet(new Set());

        row = new Row(frenchPress);
        superset.addRow(row);
        row.addSet(new Set());
        pushRoutine.addSuperset(superset);

        superset = new Superset();
        row = new Row(press);
        row.addSet(new Set());
        superset.addRow(row);

        row = new Row(chestPress);
        row.addSet(new Set());
        superset.addRow(row);
        pushRoutine.addSuperset(superset);
        
        db.addWorkout(pushRoutine);

        // PULL
        pullRoutine = new Workout();
        pullRoutine.setIsRoutine(true);
        pullRoutine.setName("Pull");

        superset = new Superset();
        row = new Row(pullUp);
        superset.addRow(row);
        row.addSet(new Set());
        pullRoutine.addSuperset(superset);

        superset = new Superset();
        row = new Row(chinUp);
        row.addSet(new Set());
        superset.addRow(row);

        row = new Row(machineRow);
        row.addSet(new Set());
        superset.addRow(row);
        pullRoutine.addSuperset(superset);

        superset = new Superset();
        row = new Row(scottCurl);
        row.addSet(new Set());
        superset.addRow(row);
        pullRoutine.addSuperset(superset);

        superset = new Superset();
        row = new Row(reverseFly);
        row.addSet(new Set());
        superset.addRow(row);
        pullRoutine.addSuperset(superset);

        db.addWorkout(pullRoutine);

        // CORE
        coreRoutine = new Workout();
        coreRoutine.setIsRoutine(true);
        coreRoutine.setName("Core");

        superset = new Superset();
        row = new Row(abWheelRollout);
        row.addSet(new Set());
        superset.addRow(row);

        row = new Row(kneeRaise);
        row.addSet(new Set());
        superset.addRow(row);

        row = new Row(sideClimber);
        row.addSet(new Set());
        superset.addRow(row);
        coreRoutine.addSuperset(superset);

        db.addWorkout(coreRoutine);

        // LEGS
        legsRoutine = new Workout();
        legsRoutine.setIsRoutine(true);
        legsRoutine.setName("Legs");

        superset = new Superset();
        row = new Row(squat);
        row.addSet(new Set());
        superset.addRow(row);
        legsRoutine.addSuperset(superset);

        superset = new Superset();
        row = new Row(legPress);
        row.addSet(new Set());
        superset.addRow(row);
        legsRoutine.addSuperset(superset);

        superset = new Superset();
        row = new Row(jumpingLunge);
        row.addSet(new Set());
        superset.addRow(row);
        legsRoutine.addSuperset(superset);

        superset = new Superset();
        row = new Row(calfRaise);
        row.addSet(new Set());
        superset.addRow(row);
        legsRoutine.addSuperset(superset);

        superset = new Superset();
        row = new Row(sideBend);
        row.addSet(new Set());
        superset.addRow(row);
        legsRoutine.addSuperset(superset);

        superset = new Superset();
        row = new Row(legExtension);
        row.addSet(new Set());
        superset.addRow(row);

        row = new Row(legCurl);
        row.addSet(new Set());
        superset.addRow(row);
        legsRoutine.addSuperset(superset);

        db.addWorkout(legsRoutine);
    }

    public void createWorkouts() {

        Workout workout = new Workout();
        workout.setName("Push");
        workout.setDate(DatabaseHelper.stringToCalendar("2015-06-08 20:00:00"));
        db.addWorkout(workout);

        workout.setName("Pull, Core");
        workout.setDate(DatabaseHelper.stringToCalendar("2015-06-07 22:00:00"));
        workout.setNote("short");
        db.addWorkout(workout);

        workout.setName("Legs");
        workout.setDate(DatabaseHelper.stringToCalendar("2015-06-03 18:20:00"));
        workout.setNote("after refeed, thin shoes");
        db.addWorkout(workout);

        workout.setName("Push, Bauch");
        workout.setDate(DatabaseHelper.stringToCalendar("2015-06-01 21:00:00"));
        workout.setNote("nach Refeed");
        db.addWorkout(workout);
    }

    public void createAll() {
        createExercises();
        createRoutines();
        //createWorkouts();
    }
}
