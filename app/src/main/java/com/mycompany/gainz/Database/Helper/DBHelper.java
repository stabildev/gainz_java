package com.mycompany.gainz.Database.Helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.mycompany.gainz.Database.Model.Exercise;
import com.mycompany.gainz.Database.Model.Set;
import com.mycompany.gainz.Database.Model.Workout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DBHelper extends SQLiteOpenHelper {

    private static DBHelper mInstance = null;
    private Context mContext;

    // Logcat tag
    private static final String LOG = "DatabaseHelper";

    // SQLite time format
    public static final String SQL_DATETIME = "yyyy-MM-dd HH:mm:ss";

    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "Workouts";

    // Keys
    public static final String _ID = "_id";
    public static final String NAME = "name";
    public static final String DATE = "date";
    public static final String NOTE = "note";
    public static final String IS_ROUTINE = "is_routine";

    public static final String WORKOUT_ID = "workout_id";
    public static final String SUPERSET = "superset";
    public static final String EXERCISE_ID = "exercise_id";

    public static final String REPS = "reps";
    public static final String WEIGHT = "weight";
    public static final String REST = "rest";
    public static final String IS_DONE = "is_done";

    public static final String BODYPART = "bodypart";
    public static final String DEFAULT_INCREMENT = "default_increment";

    public static final String ROW = "row";

    public static final String SET_ID = "set_id";

    // Tables
    public static final String WORKOUTS = "Workouts";
    public static final String NOTES = "Notes";
    public static final String SETS = "Sets";
    public static final String EXERCISES = "Exercises";

    // Table Create Statements
    // WORKOUTS table create statement
    private static final String CREATE_TABLE_WORKOUTS = "CREATE TABLE " + WORKOUTS + "("
            + _ID + " INTEGER PRIMARY KEY,"
            + IS_ROUTINE + " INTEGER NOT NULL DEFAULT 0,"
            + DATE + " DATETIME,"
            + NOTE + " TEXT,"
            + NAME + " TEXT)";

    // EXERCISES table create statement
    private static final String CREATE_TABLE_EXERCISES = "CREATE TABLE " + EXERCISES + "("
            + _ID + " INTEGER PRIMARY KEY,"
            + NAME + " TEXT,"
            + BODYPART + " TEXT,"
            + DEFAULT_INCREMENT + " INTEGER)";

    // SETS table create statement
    private static final String CREATE_TABLE_SETS = "CREATE TABLE " + SETS + "("
            + _ID + " INTEGER PRIMARY KEY,"
            + REPS + " INTEGER,"
            + WEIGHT + " INTEGER,"
            + REST + " INTEGER,"
            + IS_DONE + " INTEGER,"
            + SUPERSET + " INTEGER,"
            + ROW + " INTEGER,"
            + EXERCISE_ID + " INTEGER,"
            + WORKOUT_ID +  " INTEGER,"
            + "FOREIGN KEY(" + EXERCISE_ID + ") REFERENCES " + EXERCISES + "(" + _ID + "),"
            + "FOREIGN KEY(" + WORKOUT_ID + ") REFERENCES " + WORKOUTS + "(" + _ID + "))";

    private static final String CREATE_TABLE_NOTES = "CREATE TABLE " + NOTES + "("
            + _ID + " INTEGER PRIMARY KEY,"
            + WORKOUT_ID + " INTEGER,"
            + SUPERSET + " INTEGER,"
            + NOTE + " TEXT,"
            + "FOREIGN KEY(" + WORKOUT_ID + ") REFERENCES " + WORKOUTS + "(" + _ID + "),"
            + "FOREIGN KEY(" + SUPERSET + ") REFERENCES " + SETS + "(" + SUPERSET + "))";


    public static DBHelper getInstance(Context context) {
//        use the application context as suggested by CommonsWare.
//        this will ensure that you dont accidentally leak an Activity's
//        context
        if (mInstance == null) {
            mInstance = new DBHelper(context.getApplicationContext());
        }
        return mInstance;
    }

    //    constructor should be private to prevent direct instantiation.
//    make call to static factory method "getInstance()" instead.
    private DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // creating required tables
        db.execSQL(CREATE_TABLE_WORKOUTS);
        db.execSQL(CREATE_TABLE_EXERCISES);
        db.execSQL(CREATE_TABLE_SETS);
        db.execSQL(CREATE_TABLE_NOTES);

        // creating dummy data
        new DummyData(this).createAll();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(LOG, "Upgrading database, which will destroy all old data");

        // on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS " + WORKOUTS);
        db.execSQL("DROP TABLE IF EXISTS " + NOTES);
        db.execSQL("DROP TABLE IF EXISTS " + EXERCISES);
        db.execSQL("DROP TABLE IF EXISTS " + SETS);

        // create new tables
        onCreate(db);
    }

    // closing database
    public void closeDB() {
        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null && db.isOpen())
            db.close();
    }

    public static String dateToString(Calendar calendar) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(SQL_DATETIME);
        dateFormat.setTimeZone(calendar.getTimeZone());
        return dateFormat.format(calendar.getTime());
    }

    public static Calendar stringToCalendar(@Nullable String datetime) {
        Calendar calendar = new GregorianCalendar();
        SimpleDateFormat dateFormat = new SimpleDateFormat(SQL_DATETIME);
        if (datetime != null) {
            try {
                calendar.setTime(dateFormat.parse(datetime));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return calendar;
    }

    public static Bundle cursorToBundle(Cursor cursor, String[] keys) {
        int column;
        Bundle bundle = new Bundle();

        for (String s : keys) {
            column = cursor.getColumnIndex(s);
            if (!cursor.isNull(column)) {
                switch (cursor.getType(column)) {
                    case Cursor.FIELD_TYPE_INTEGER:
                        bundle.putInt(s, cursor.getInt(column));
                        break;
                    case Cursor.FIELD_TYPE_STRING:
                        bundle.putString(s, cursor.getString(column));
                        break;
                    default:
                        break;
                }
            }
        }
        return bundle;
    }

    public void addWorkout(Workout workout) { // adds workout including all rows and sets to database
        ContentValues cv = new ContentValues();
        if (!workout.getName().equals(""))
            cv.put(NAME, workout.getName());
        cv.put(DATE, dateToString(workout.getDate()));
        if (!workout.getNote().equals(""))
            cv.put(NOTE, workout.getNote());
        cv.put(IS_ROUTINE, workout.isRoutine() ? 1 : 0);

        // add workout to database and set workout object's _id
        // this changes also the rows' workout_ids (-> Workout.class)
        int workout_id = (int) getWritableDatabase().insert(WORKOUTS, null, cv);
        Log.i("SQL", "INSERT Workout with _id " + workout_id);
        workout.set_id(workout_id);

        // adding corresponding supersets to database
        for (Set s : workout.setsInRows.values())
                addSet(s);

        // adding notes to database
        Iterator it = workout.supersetNotes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, String> pair = (Map.Entry) it.next();
            cv.clear();
            cv.put(NOTE, pair.getValue());
            cv.put(SUPERSET, pair.getKey());
            getWritableDatabase().insert(NOTES, null, cv);
            it.remove();
        }
    }

    // updates workout without supersets and sets
    public void updateWorkout(Workout workout) {
        ContentValues cv = new ContentValues();
        cv.put(NAME, workout.getName());
        cv.put(DATE, dateToString(workout.getDate()));
        if (!workout.getNote().equals(""))
            cv.put(NOTE, workout.getNote());
        cv.put(IS_ROUTINE, workout.isRoutine() ? 1 : 0);

        getWritableDatabase().update(WORKOUTS, cv, _ID + " = " + workout.get_id(), null);
    }

    public Workout getWorkout(int workout_id) {
        Workout workout = new Workout();

        String fields = TextUtils.join(",", new String[]{
                "*", SETS+"."+_ID + " AS " + SET_ID});

        String query = "SELECT " + fields + " FROM " + WORKOUTS
                + " LEFT JOIN " + SETS + " ON " + WORKOUTS + "." + _ID + " = " + SETS + "." + WORKOUT_ID
                + " WHERE " + WORKOUTS + "." + _ID + " = " + workout_id
                + " ORDER BY " + SUPERSET + "," + ROW + "," + SET_ID + " ASC";
        Log.i("SQL", query);
        Cursor c = getReadableDatabase().rawQuery(query, null);

        if (!c.moveToFirst()) {
            workout.set_id(-1);
            return workout;
        }

        workout.set_id(workout_id);
        if (!c.isNull(c.getColumnIndex(NAME)))
            workout.setName(c.getString(c.getColumnIndex(NAME)));
        if (!c.isNull(c.getColumnIndex(DATE)))
            workout.setDate(stringToCalendar(c.getString(c.getColumnIndex(DATE))));
        if (c.getColumnIndex(NOTE) >= 0 && !c.isNull(c.getColumnIndex(NOTE)))
            workout.setNote(c.getString(c.getColumnIndex(NOTE)));
        workout.setIsRoutine(c.getInt(c.getColumnIndex(IS_ROUTINE)) == 1);

        // get Exercises
        Map<Integer, Exercise> exercises = getExercises();

        int curSuperset, curRow;
        Set set;

        // check if Workout contains sets
        if (c.getColumnIndex(SET_ID) >= 0) {

            do {
                // create new set
                if (!c.isNull(c.getColumnIndex(SET_ID)) && c.getColumnIndex(SET_ID) != -1) {
                    curSuperset = c.getInt(c.getColumnIndex(SUPERSET));
                    curRow = c.getInt(c.getColumnIndex(ROW));

                    set = new Set(
                            workout,
                            curSuperset,
                            curRow,
                            exercises.get(c.getInt(c.getColumnIndex(EXERCISE_ID))));
                    set.set_id(c.getInt(c.getColumnIndex(SET_ID)));

                    // get set data if exists
                    if (!c.isNull(c.getColumnIndex(REPS)))
                        set.setReps(c.getInt(c.getColumnIndex(REPS)));
                    if (!c.isNull(c.getColumnIndex(WEIGHT)))
                        set.setWeight(c.getInt(c.getColumnIndex(WEIGHT)));
                    if (!c.isNull(c.getColumnIndex(REST)))
                        set.setRest(c.getInt(c.getColumnIndex(REST)));
                    set.setIsDone(c.getInt(c.getColumnIndex(IS_DONE)) == 1);
                }

            } while (c.moveToNext());
        } c.close();

        getSupersetNotes(workout);

        return workout;
    }

    public void getSupersetNotes(Workout workout) {
        String query = "SELECT * FROM " + NOTES + " WHERE " + WORKOUT_ID + " = " + workout.get_id();
        Cursor c = getReadableDatabase().rawQuery(query, null);

        if (!c.moveToFirst())
            return;

        do {
            workout.supersetNotes.put(
                    c.getInt(c.getColumnIndex(SUPERSET)),
                    c.getString(c.getColumnIndex(NOTE)));
        } while (c.moveToNext());

        c.close();
    }

    public void addSupersetNote(Set set) {
        ContentValues cv = new ContentValues();
        cv.put(WORKOUT_ID, set.getWorkout().get_id());
        cv.put(SUPERSET, set.getSuperset());
        getWritableDatabase().insert(NOTES, null, cv);
    }

    public void updateSupersetNote(Set set) {
        ContentValues cv = new ContentValues();
        cv.put(WORKOUT_ID, set.getWorkout().get_id());
        getWritableDatabase().update(NOTES, cv, SUPERSET + " = " + set.getSuperset(), null);
    }

    // deletes workout including all supersets and sets from database
    public void dropWorkout(int workout_id) {
        // delete notes
        String query = "DELETE FROM " + NOTES + " WHERE " + WORKOUT_ID + " = " + workout_id;
        Log.i("SQL", query);
        getWritableDatabase().execSQL(query);

        // delete sets
        query = "DELETE FROM " + SETS + " WHERE " + WORKOUT_ID + " = " + workout_id;
        Log.i("SQL", query);
        getWritableDatabase().execSQL(query);

        // delete workout
        query = "DELETE FROM " + WORKOUTS + " WHERE " + _ID + " = " + workout_id;
        Log.i("SQL", query);
        getWritableDatabase().execSQL(query);
    }

    // add superset note to database
    public void addNote(int workout_id, int superset, String note) {
        ContentValues cv = new ContentValues();
        cv.put(WORKOUT_ID, workout_id);
        cv.put(SUPERSET, superset);
        cv.put(NOTE, note);
        getWritableDatabase().insert(NOTES, null, cv);
    }

    // add set to database
    public void addSet(Set set) {
        ContentValues cv = new ContentValues();
        cv.put(EXERCISE_ID, set.getExercise().get_id());

        cv.put(WORKOUT_ID, set.getWorkout().get_id());
        cv.put(SUPERSET, set.getSuperset());
        cv.put(ROW, set.getRow());

        cv.put(REPS, set.getReps());
        cv.put(WEIGHT, set.getWeight());
        cv.put(REST, set.getRest());
        cv.put(IS_DONE, 0);

        // add set to database and assign new id
        set.set_id((int) getWritableDatabase().insert(SETS, null, cv));
        Log.i("SQL", "INSERT Set with _id " + set.get_id());
    }

    public void updateSet(Set set) {
        ContentValues cv = new ContentValues();
        cv.put(SUPERSET, set.getSuperset());
        cv.put(ROW, set.getRow());

        cv.put(REPS, set.getReps());
        cv.put(WEIGHT, set.getWeight());
        cv.put(REST, set.getRest());
        cv.put(IS_DONE, set.isDone() ? 1 : 0);

        getWritableDatabase().update(SETS, cv, _ID + " = " + set.get_id(), null);
    }

    public void dropSet(int set_id) {
        String query = "DELETE FROM " + SETS + " WHERE " + _ID + " = " + set_id;
        Log.i("SQL", query);
        getWritableDatabase().execSQL(query);
    }

    public Map <Integer, Exercise> getExercises() {
        Map <Integer, Exercise> exercises = new HashMap<>();
        Exercise exercise;

        String query = "SELECT * FROM " + EXERCISES + " ORDER BY " + NAME + " ASC";
        Cursor c = getReadableDatabase().rawQuery(query, null);

        if (c.moveToFirst()) {
            do {
                exercise = new Exercise(c.getString(c.getColumnIndex(NAME)));
                exercise.set_id(c.getInt(c.getColumnIndex(_ID)));

                if (!c.isNull(c.getColumnIndex(BODYPART)))
                    exercise.setBodypart(c.getString(c.getColumnIndex(BODYPART)));

                if (!c.isNull(c.getColumnIndex(DEFAULT_INCREMENT)))
                    exercise.setDefaultIncrement(c.getInt(c.getColumnIndex(DEFAULT_INCREMENT)));

                exercises.put(exercise.get_id(), exercise);
            } while (c.moveToNext());
            c.close();
        }

        return exercises;
    }

    public void addExercise(Exercise exercise) {
        ContentValues cv = new ContentValues();
        if (exercise.getName().equals(""))
            return;
        cv.put(NAME, exercise.getName());
        if (!exercise.getBodypart().equals(""))
            cv.put(BODYPART, exercise.getBodypart());
        cv.put(DEFAULT_INCREMENT, exercise.getDefaultIncrement());

        // add exercise to database and set exercise object's _id
        exercise.set_id((int) getWritableDatabase().insert(EXERCISES, null, cv));
    }
}