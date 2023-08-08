package com.mycompany.gains.Data.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.mycompany.gains.Data.Model.Exercise;
import com.mycompany.gains.Data.Model.Goal;
import com.mycompany.gains.Data.Model.Row;
import com.mycompany.gains.Data.Model.Set;
import com.mycompany.gains.Data.Model.Superset;
import com.mycompany.gains.Data.Model.Workout;
import com.mycompany.gains.R;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mycompany.gains.Data.Database.DatabaseConstants.*;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static DatabaseHelper mInstance = null;
    private Context mContext;

    private boolean createDatabase = false, upgradeDatabase = false;

    // Logcat tag
    private static final String LOG = "DatabaseHelper";

    // SQLite time format
    public static final String SQL_DATETIME = "yyyy-MM-dd HH:mm:ss";

    // Database Name
    private static final String DATABASE_NAME = "Workouts";

    private static boolean fallBackToOtherRoutines = false;

    /*private static int[] keepIds =  {1,2};
    private List<Workout> keepWorkouts;*/


    // Table Create Statements
    // WORKOUTS table create statement
    private static final String CREATE_TABLE_WORKOUTS = "CREATE TABLE " + WORKOUTS + "("
            + _ID + " INTEGER PRIMARY KEY,"
            + ROUTINE_ID + " INTEGER DEFAULT -1,"
            + IS_ROUTINE + " INTEGER NOT NULL DEFAULT 0,"
            + IS_STARRED + " INTEGER DEFAULT 0,"
            + DATE + " DATETIME,"
            + NOTE + " TEXT,"
            + DURATION + " INTEGER DEFAULT 0,"
            + NAME + " TEXT)";

    // EXERCISES table create statement
    private static final String CREATE_TABLE_EXERCISES = "CREATE TABLE " + EXERCISES + "("
            + _ID + " INTEGER PRIMARY KEY,"
            + NAME + " TEXT,"
            + BODYPART + " TEXT,"
            + DEFAULT_INCREMENT + " INTEGER)";

    // ROWS table create statement
    private static final String CREATE_TABLE_ROWS = "CREATE TABLE " + ROWS + "("
            + _ID + " INTEGER PRIMARY KEY,"
            + SUPERSET + " INTEGER,"
            + WORKOUT_ID + " INTEGER,"
            + ORDER + " INTEGER,"
            + EXERCISE_ID + " INTEGER,"
            + REST + " INTEGER DEFAULT 60,"
            + NOTE + " TEXT,"
            + "FOREIGN KEY(" + EXERCISE_ID + ") REFERENCES " + EXERCISES + "(" + _ID + "),"
            + "FOREIGN KEY(" + WORKOUT_ID + ") REFERENCES " + WORKOUTS + "(" + _ID + "))";


    // SETS table create statement
    private static final String CREATE_TABLE_SETS = "CREATE TABLE " + SETS + "("
            + _ID + " INTEGER PRIMARY KEY,"
            + REPS + " INTEGER,"
            + WEIGHT + " INTEGER,"
            + IS_DONE + " INTEGER,"
            + ROW_ID + " INTEGER,"
            + "FOREIGN KEY(" + ROW_ID + ") REFERENCES " + ROWS + "(" + _ID + "))";

    public static DatabaseHelper getInstance(Context context) {
//        use the application context as suggested by CommonsWare.
//        this will ensure that you dont accidentally leak an Activity's
//        context
        if (mInstance == null) {
            mInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return mInstance;
    }

    //    constructor should be private to prevent direct instantiation.
//    make call to static factory method "getInstance()" instead.
    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, context.getResources().getInteger(R.integer.databaseVersion));
        this.mContext = context;
    }

    public void initialize() {
        // Creates or updates the database in internal storage if it is needed
        // before opening the database.
        getWritableDatabase();

        if (createDatabase || upgradeDatabase) {
            // create dummy content
            new DummyData(this).createAll();
            createDatabase = upgradeDatabase = false;
        }

        // Close SQLiteOpenHelper so it will commit the created empty database
        // to internal storage.
        close();

        // Access the copied database so SQLiteHelper will cache it and mark it
        // as created. [seems unnecessary]
        //getWritableDatabase().close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createDatabase = true;

        // creating required tables
        db.execSQL(CREATE_TABLE_WORKOUTS);
        db.execSQL(CREATE_TABLE_EXERCISES);
        db.execSQL(CREATE_TABLE_ROWS);
        db.execSQL(CREATE_TABLE_SETS);

        // re-add kept workouts
        /*for (Workout w : keepWorkouts)
            addWorkout(w);*/
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // keep selected workouts
        /*keepWorkouts = new ArrayList<>(keepIds.length);
        for (int i : keepIds)
            keepWorkouts.add(getWorkout(i, true));*/

        Log.w(LOG, "Upgrading database, which will destroy all old data");
        upgradeDatabase = true;

        // on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS " + WORKOUTS);
        db.execSQL("DROP TABLE IF EXISTS " + ROWS);
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

    public static boolean doesDatabaseExist(Context context) {
        File dbFile = context.getDatabasePath(DATABASE_NAME);
        return dbFile.exists();
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
        cv.put(IS_STARRED, workout.isStarred() ? 1 : 0);
        cv.put(ROUTINE_ID, workout.getRoutineId());
        cv.put(DURATION, workout.getDuration());

        // add workout to database and set workout object's _id
        // this changes also the rows' workout_ids (-> Workout.class)
        workout.setDatabaseId((int) getWritableDatabase().insert(WORKOUTS, null, cv));
        Log.i("SQL", "INSERT Workout with _id " + workout.getDatabaseId());

        // adding corresponding supersets, rows and sets to database
        for (Superset superset : workout.getSupersets())
                for (Row row : superset.getRows())
                    addRow(row, true);
    }

    // updates workout without supersets and sets
    public void updateWorkout(Workout workout) {
        ContentValues cv = new ContentValues();
        cv.put(NAME, workout.getName());
        cv.put(DATE, dateToString(workout.getDate()));
        cv.put(NOTE, workout.getNote());
        cv.put(IS_ROUTINE, workout.isRoutine() ? 1 : 0);
        cv.put(IS_STARRED, workout.isStarred() ? 1 : 0);
        cv.put(ROUTINE_ID, workout.getRoutineId());
        cv.put(DURATION, workout.getDuration());

        getWritableDatabase().update(WORKOUTS, cv, _ID + " = " + workout.getDatabaseId(), null);
    }

    public void starWorkout(int workoutId, boolean star) {
        ContentValues cv = new ContentValues();
        cv.put(IS_STARRED, star ? 1 : 0);
        getWritableDatabase().update(WORKOUTS, cv, _ID + " = " + workoutId, null);
        Log.i("LEL", "star workout " + workoutId + ": " + (star ? "true" : "false"));
    }

    public Workout getWorkoutFromRoutine(int routineId) {
        Workout workout = getWorkout(routineId);
        workout.setIsRoutine(false);
        workout.setRoutineId(routineId);
        workout.setDatabaseId(-1);

        for (Superset superset : workout.getSupersets()) {
            for (Row row : superset.getRows()) {
                List<Goal> goals = getGoals(row.getExercise(), routineId, null);
                for (int i = 0; i < row.getSetCount() && i < goals.size(); i++) {
                    Set set = row.getSet(i);
                    Goal goal = goals.get(i);
                    set.setReps(goal.reps);
                    set.setWeight(goal.weight);
                    set.setIsDone(false);
                }
                row.setRest(goals.get(0).rest);
            }
        }
        return workout;
    }

    @Deprecated // TODO change rest to row
    public List<Goal> getGoals(Exercise exercise, @Nullable Integer routineId, @Nullable Integer depth) {
        Log.i("LEL", "getGoals("+exercise.getName()+", "+(routineId != null?routineId:"null")+");");
        // date 1
            // ss.row 1
                // [1] [2] [3]
            // ss.row 2
                // [-] [-]
        // other date
            // ss.row 3
                // [-] [-] [-]
            // ss.row 4
                // [-] [-] [-] [4]

        List<Goal> goals = new ArrayList<>();
        String fields = TextUtils.join(",", new String[]{
                SETS + ".*", DATE, ORDER, SUPERSET,
                SETS + "." + _ID + " AS " + SET_ID,
                ROWS + "." + NOTE + " AS " + ROW_NOTE});

        boolean routineOnly = routineId != null && routineId != -1;

        // all sets for the same exercise within the same routine
        String query = "SELECT " + fields + " FROM " + SETS
                + " INNER JOIN " + ROWS + " ON " + ROWS + "." + _ID + " = " + SETS + "." + ROW_ID
                + " INNER JOIN " + WORKOUTS + " ON " + WORKOUTS + "." + _ID + " = " + ROWS + "." + WORKOUT_ID
                + " WHERE " + (routineOnly ? (ROUTINE_ID + " = " + routineId + " AND ") : "" )
                + IS_ROUTINE + " = 0 AND " + EXERCISE_ID + " = " + exercise.get_id()
                + " ORDER BY " + DATE + ", " + WORKOUT_ID + ", " + SUPERSET + "," + ORDER + " DESC," + SET_ID + " ASC";
        Log.i("SQL", query);
        Cursor c = getReadableDatabase().rawQuery(query, null);

        // check if Workout contains sets
        if (!c.moveToFirst() || c.getColumnIndex(SET_ID) < 0) {
            if (fallBackToOtherRoutines && (routineId == null || routineId == -1))
                return getGoals(exercise, null, depth);
            else
                return goals; // empty list
        }

        List<List<Goal>> goalsInRows = new ArrayList<>();
        goalsInRows.add(new ArrayList<Goal>());

//        List<Row> rowsWithExercise = new ArrayList<>();
//        Row temp = new Row();
//        rowsWithExercise.add(temp);
        int prevSuperset = -1;
        int prevRow = -1;
        Calendar prevDate = null;

        do {
            int curSuperset = c.getInt(c.getColumnIndex(SUPERSET));
            int curRow = c.getInt(c.getColumnIndex(ORDER));
            Calendar curDate = stringToCalendar(c.getString(c.getColumnIndex(DATE)));

            if (prevDate != null && (!curDate.equals(prevDate) || curSuperset != prevSuperset || curRow != prevRow)) {
                goalsInRows.add(new ArrayList<Goal>());
            }
            Goal goal = new Goal(exercise);
            goal.reps = c.getInt(c.getColumnIndex(REPS));
            goal.weight = c.getInt(c.getColumnIndex(WEIGHT));
            goal.rest = c.getInt(c.getColumnIndex(REST));

            // add row note if exists
            if (c.getColumnIndex(ROW_NOTE) >= 0 && !c.isNull(c.getColumnIndex(ROW_NOTE)))
                goal.note = c.getString(c.getColumnIndex(ROW_NOTE));

            goalsInRows.get(goalsInRows.size()-1).add(goal);

            prevSuperset = curSuperset;
            prevRow = curRow;
            prevDate = curDate;
        } while (c.moveToNext());
        c.close();

        {int setPos = 0;
            for (List<Goal> row : goalsInRows) {
                if (depth != null && goalsInRows.indexOf(row) >= depth)
                    break;
                for ( ; setPos < row.size(); setPos++) {
                    goals.add(row.get(setPos));
                }
            }}

        return goals;
    }

    public static Goal compressGoals(List<Goal> uncompressed) {
        Goal compressed = new Goal();
        List<String> notes = new ArrayList<>(uncompressed.size());

        if (uncompressed.size() > 0) {
            compressed.sets = uncompressed.size(); // else 1
            compressed.exercise = uncompressed.get(0).exercise;
        }

        for (Goal goal : uncompressed) {
            compressed.reps = Math.max(goal.reps, compressed.reps);
            compressed.weight = Math.max(goal.weight, compressed.weight);

            compressed.rest += goal.rest;

            if (uncompressed.indexOf(goal) >= uncompressed.size()-2 && !notes.contains(goal.note)) {
                notes.add(goal.note);
            }
        }
        compressed.rest = (int) (float) compressed.rest/compressed.sets;
        compressed.note = TextUtils.join(",\n", notes);

        return compressed;
    }

    public Workout getWorkout(int workoutId) {
        return getWorkout(workoutId, true);
    }

    public Workout getWorkout(int workoutId, boolean complete) {
        Workout workout = new Workout();

        String fields = TextUtils.join(",", new String[]{
                "*",
                SETS + "." + _ID + " AS " + SET_ID,
                WORKOUTS + "." + NOTE + " AS " + WORKOUT_NOTE,
                ROWS + "." + NOTE + " AS " + ROW_NOTE});

        String query = "SELECT " + fields + " FROM " + WORKOUTS
                + " LEFT JOIN " + ROWS + " ON " + WORKOUTS + "." + _ID + " = " + ROWS + "." + WORKOUT_ID
                + " LEFT JOIN " + SETS + " ON " + ROWS + "." + _ID + " = " + SETS + "." + ROW_ID
                + " WHERE " + WORKOUTS + "." + _ID + " = " + workoutId
                + " ORDER BY " + SUPERSET + "," + ORDER + "," + SET_ID + " ASC";
        Log.i("SQL", query);
        Cursor c = getReadableDatabase().rawQuery(query, null);

        if (!c.moveToFirst()) {
            workout.setDatabaseId(-1);
            return workout;
        }

        workout.setDatabaseId(workoutId);
        if (!c.isNull(c.getColumnIndex(NAME)))
            workout.setName(c.getString(c.getColumnIndex(NAME)));
        if (!c.isNull(c.getColumnIndex(DATE)))
            workout.setDate(stringToCalendar(c.getString(c.getColumnIndex(DATE))));
        if (c.getColumnIndex(WORKOUT_NOTE) >= 0 && !c.isNull(c.getColumnIndex(WORKOUT_NOTE)))
            workout.setNote(c.getString(c.getColumnIndex(WORKOUT_NOTE)));
        workout.setIsRoutine(c.getInt(c.getColumnIndex(IS_ROUTINE)) == 1);
        workout.setIsStarred(c.getInt(c.getColumnIndex(IS_STARRED)) == 1);
        workout.setRoutineId(c.getInt(c.getColumnIndex(ROUTINE_ID)));
        workout.setDuration(c.getInt(c.getColumnIndex(DURATION)));

        if (!complete)
            return workout;

        // get Exercises
        Map<Integer, Exercise> exercises = getExercises();

        // check if Workout contains sets
        if (c.getColumnIndex(SET_ID) >= 0) {
            int curSupersetPos, prevSupersetPos = -1, curRowPos, prevRowPos = -1;
            Superset curSuperset = new Superset(); Row curRow = new Row(); Set curSet;

            do {
                if (!c.isNull(c.getColumnIndex(SET_ID)) && c.getColumnIndex(SET_ID) != -1) {
                    curSupersetPos = c.getInt(c.getColumnIndex(SUPERSET));
                    curRowPos = c.getInt(c.getColumnIndex(ORDER));

                    // create new superset if necessary
                    if (curSupersetPos != prevSupersetPos) {
                        curSuperset = new Superset();
                        workout.addSuperset(curSuperset);
                    }
                    // create new row if necessary
                    if (curRowPos != prevRowPos || curSupersetPos != prevSupersetPos) {
                        curRow = new Row();
                        curSuperset.addRow(curRow);
                        curRow.setDatabaseId(c.getInt(c.getColumnIndex(ROW_ID)));
                        curRow.setExercise(exercises.get(c.getInt(c.getColumnIndex(EXERCISE_ID))));

                        // check for deleted exercises
                        if (curRow.getExercise() == null) {
                            curRow.setExercise(new Exercise(mContext.getString(R.string.deleted_exercise)));
                            updateRow(curRow);
                        }

                        // add row note if exists
                        if (c.getColumnIndex(ROW_NOTE) >= 0 && !c.isNull(c.getColumnIndex(ROW_NOTE)))
                            curRow.setNote(c.getString(c.getColumnIndex(ROW_NOTE)));

                        // set rest
                        curRow.setRest(c.getInt(c.getColumnIndex(REST)));
                    }

                    // create new set
                    curSet = new Set();
                    curRow.addSet(curSet);

                    curSet.setDatabaseId(c.getInt(c.getColumnIndex(SET_ID)));
                    // get set data if exists
                    if (!c.isNull(c.getColumnIndex(REPS)))
                        curSet.setReps(c.getInt(c.getColumnIndex(REPS)));
                    if (!c.isNull(c.getColumnIndex(WEIGHT)))
                        curSet.setWeight(c.getInt(c.getColumnIndex(WEIGHT)));
                    curSet.setIsDone(c.getInt(c.getColumnIndex(IS_DONE)) == 1);

                    prevRowPos = curRowPos;
                    prevSupersetPos = curSupersetPos;
                }
            } while (c.moveToNext());
        } c.close();

        return workout;
    }

    // deletes workout including all supersets and sets from database
    public void dropWorkout(int workoutId) {
        // delete sets
        String query = "DELETE FROM " + SETS + " WHERE " + ROW_ID + " IN("
                + "SELECT " + _ID + " FROM " + ROWS
                + " WHERE " + WORKOUT_ID + " = " + workoutId + ")";
        Log.i("SQL", query);
        getWritableDatabase().execSQL(query);

        // delete rows
        query = "DELETE FROM " + ROWS + " WHERE " + WORKOUT_ID + " = " + workoutId;
        Log.i("SQL", query);
        getWritableDatabase().execSQL(query);

        // delete workout
        query = "DELETE FROM " + WORKOUTS + " WHERE " + _ID + " = " + workoutId;
        Log.i("SQL", query);
        getWritableDatabase().execSQL(query);
    }

    public void deleteUnusedExercises() {
        String query = "DELETE FROM " + EXERCISES + " WHERE " + _ID + " NOT IN("
                + "SELECT " + EXERCISE_ID + " FROM " + EXERCISES + " INNER JOIN "
                + ROWS + " ON " + EXERCISES+"."+_ID + " = " + ROWS+"."+EXERCISE_ID +")";
        Log.i("SQL", query);
        getWritableDatabase().execSQL(query);
    }

    // add row to database
    public void addRow(Row row, boolean includeSets) {
        ContentValues cv = new ContentValues();

        cv.put(ORDER, row.getSuperset().getRows().indexOf(row));
        cv.put(SUPERSET, row.getSuperset().getWorkout().getSupersets().indexOf(
                row.getSuperset()));
        cv.put(WORKOUT_ID, row.getSuperset().getWorkout().getDatabaseId());
        cv.put(EXERCISE_ID, row.getExercise().get_id());
        if (!row.getNote().equals(""))
            cv.put(NOTE, row.getNote());
        cv.put(REST, row.getRest());

        row.setDatabaseId((int) getWritableDatabase().insert(ROWS, null, cv));
        Log.i("SQL", "INSERT Row with _id " + row.getDatabaseId());
        if (includeSets)
            for (Set set : row.getSets())
                addSet(set);
    }

    public void updateRow(Row row) {
        ContentValues cv = new ContentValues();

        cv.put(ORDER, row.getPosition());
        cv.put(SUPERSET, row.getSuperset().getPosition());
        cv.put(EXERCISE_ID, row.getExercise().get_id());
        cv.put(NOTE, row.getNote());
        cv.put(REST, row.getRest());

        getWritableDatabase().update(ROWS, cv, _ID + " = " + row.getDatabaseId(), null);
    }

    public void dropRow(int rowId) {
        // delete sets
        String query = "DELETE FROM " + SETS + " WHERE " + ROW_ID + " = " + rowId;
        Log.i("SQL", query);
        getWritableDatabase().execSQL(query);

        // delete row
        query = "DELETE FROM " + ROWS + " WHERE " + _ID + " = " + rowId;
        Log.i("SQL", query);
        getWritableDatabase().execSQL(query);
    }

    // add set to database
    public void addSet(Set set) {
        ContentValues cv = new ContentValues();

        cv.put(ROW_ID, set.getRow().getDatabaseId());
        cv.put(REPS, set.getReps());
        cv.put(WEIGHT, set.getWeight());
        cv.put(IS_DONE, 0);

        // add set to database and assign new id
        set.setDatabaseId((int) getWritableDatabase().insert(SETS, null, cv));
        Log.i("SQL", "INSERT Set with _id " + set.getDatabaseId());
    }

    public void updateSet(Set set) {
        ContentValues cv = new ContentValues();
        cv.put(REPS, set.getReps());
        cv.put(WEIGHT, set.getWeight());
        cv.put(IS_DONE, set.isDone() ? 1 : 0);

        getWritableDatabase().update(SETS, cv, _ID + " = " + set.getDatabaseId(), null);
    }

    public void dropSet(int setId) {
        String query = "DELETE FROM " + SETS + " WHERE " + _ID + " = " + setId;
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

                if (!c.isNull(c.getColumnIndex(DEFAULT_INCREMENT)) && c.getInt(c.getColumnIndex(DEFAULT_INCREMENT)) > 0)
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
