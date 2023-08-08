package com.mycompany.gainz.Database.Helper;

import com.mycompany.gainz.Database.Model.Bodypart;
import com.mycompany.gainz.Database.Model.Exercise;
import com.mycompany.gainz.Database.Model.Set;
import com.mycompany.gainz.Database.Model.Workout;

public class DummyData {
    DBHelper db;

    Exercise inclineBench, inclineFly, chestPress, pullUp, chinUp, machineRow, reverseFly, press;
    Exercise lateralRaise, shrug, scottCurl, frenchPress, abWheelRollout, kneeRaise, sideClimber;
    Exercise sideBend, squat, legPress, jumpingLunge, calfRaise, legExtension, legCurl;
    
    Workout pushRoutine, pullRoutine, coreRoutine, legsRoutine;
    
    public DummyData(DBHelper db) {
        this.db = db;
    }
    
    public void createExercises() {
        // Chest
        inclineBench = new Exercise("Incline Bench Press","Chest");
        db.addExercise(inclineBench);

        inclineFly = new Exercise("Incline Flies", "Chest");
        db.addExercise(inclineFly);
        
        chestPress = new Exercise("Chest Press", "Chest");
        db.addExercise(chestPress);
        
        // Back
        pullUp = new Exercise("Pull-up", "Back");
        db.addExercise(pullUp);
        
        chinUp = new Exercise("Chin-up", "Back, Arms");
        db.addExercise(chinUp);
        
        machineRow = new Exercise("Machine Rows", "Back");
        db.addExercise(machineRow);

        // Shoulders
        reverseFly = new Exercise("Reverse Flies", "Shoulders, Back");
        db.addExercise(reverseFly);
        
        press = new Exercise("Presses", "Shoulders");
        db.addExercise(press);
        
        lateralRaise = new Exercise("Lateral Raise", "Shoulders");
        db.addExercise(lateralRaise);
        
        shrug = new Exercise("Shrugs", "Shoulders");
        db.addExercise(shrug);
        
        // Arms
        scottCurl = new Exercise("Scott Curls", "Arms");
        db.addExercise(scottCurl);

        frenchPress = new Exercise("French Press", "Arms");
        db.addExercise(frenchPress);
        
        // Core
        abWheelRollout = new Exercise("Ab Wheel Rollout", "Core");
        db.addExercise(abWheelRollout);

        kneeRaise = new Exercise("Knee Raise", "Core");
        db.addExercise(kneeRaise);
        
        sideClimber = new Exercise("Side Climbers", "Core");
        db.addExercise(sideClimber);
        
        sideBend = new Exercise("Side Bend", "Core");
        db.addExercise(sideBend);

        // Legs
        squat = new Exercise("Squat", "Legs");
        db.addExercise(squat);
        
        legPress = new Exercise("Leg Press", "Legs");
        db.addExercise(legPress);
        
        jumpingLunge = new Exercise("Jumping Lunges", "Legs");
        db.addExercise(jumpingLunge);
        
        calfRaise = new Exercise("Calf Raise", "Legs");
        db.addExercise(calfRaise);
        
        legExtension = new Exercise("Leg Extensions", "Legs");
        db.addExercise(legExtension);
        
        legCurl = new Exercise("Leg Curl", "Legs");
        db.addExercise(legCurl);
    }
    public void createRoutines() {
        // PUSH
        pushRoutine = new Workout();
        pushRoutine.setIsRoutine(true);
        pushRoutine.setName("Push");
        
        new Set(pushRoutine, 1,1,inclineBench);
        new Set(pushRoutine, 2,2,inclineFly);
        new Set(pushRoutine, 3,3,shrug);
        new Set(pushRoutine, 3,4,frenchPress);
        new Set(pushRoutine, 4,5,press);
        new Set(pushRoutine, 4,6,chestPress);
        
        db.addWorkout(pushRoutine);

        // PULL
        pullRoutine = new Workout();
        pullRoutine.setIsRoutine(true);
        pullRoutine.setName("Pull");

        new Set(pullRoutine, 1, 1, pullUp);
        new Set(pullRoutine, 2, 2, chinUp);
        new Set(pullRoutine, 2, 3, machineRow);
        new Set(pullRoutine, 3, 4, scottCurl);
        new Set(pullRoutine, 4, 5, reverseFly);

        db.addWorkout(pullRoutine);

        // CORE
        coreRoutine = new Workout();
        coreRoutine.setIsRoutine(true);
        coreRoutine.setName("Core");

        new Set(coreRoutine, 1, 1, abWheelRollout);
        new Set(coreRoutine, 1, 2, kneeRaise);
        new Set(coreRoutine, 1, 3, sideClimber);

        db.addWorkout(coreRoutine);

        // LEGS
        legsRoutine = new Workout();
        legsRoutine.setIsRoutine(true);
        legsRoutine.setName("Legs");

        new Set(legsRoutine, 1, 1, squat);
        new Set(legsRoutine, 2, 2, legPress);
        new Set(legsRoutine, 3, 3, jumpingLunge);
        new Set(legsRoutine, 4, 4, calfRaise);
        new Set(legsRoutine, 5, 5, sideBend);
        new Set(legsRoutine, 6, 6, legExtension);
        new Set(legsRoutine, 6, 7, legCurl);

        db.addWorkout(legsRoutine);
    }

    public void createWorkouts() {

        Workout workout = new Workout();
        workout.setName("Push");
        workout.setDate(db.stringToCalendar("2015-06-08 20:00:00"));
        db.addWorkout(workout);

        workout.setName("Pull, Core");
        workout.setDate(db.stringToCalendar("2015-06-07 22:00:00"));
        workout.setNote("short");
        db.addWorkout(workout);

        workout.setName("Legs");
        workout.setDate(db.stringToCalendar("2015-06-03 18:20:00"));
        workout.setNote("after refeed, thin shoes");
        db.addWorkout(workout);

        workout.setName("Push, Bauch");
        workout.setDate(db.stringToCalendar("2015-06-01 21:00:00"));
        workout.setNote("nach Refeed");
        db.addWorkout(workout);
    }

    public void createAll() {
        createExercises();
        createRoutines();
        createWorkouts();
    }
}
