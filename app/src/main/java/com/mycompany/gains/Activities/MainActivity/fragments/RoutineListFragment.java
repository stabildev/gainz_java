package com.mycompany.gains.Activities.MainActivity.fragments;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mycompany.gains.Activities.MainActivity.MainActivity;
import com.mycompany.gains.widgets.EmptyRecyclerView;
import com.mycompany.gains.Data.Database.DatabaseHelper;
import com.mycompany.gains.Activities.MainActivity.adapters.RoutineListAdapter;
import com.mycompany.gains.R;
import com.mycompany.gains.SQLiteLoader.SQLiteCursorLoader;

import static com.mycompany.gains.Data.Database.DatabaseConstants.*;


public class RoutineListFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "RoutineListWindow";

    private RoutineListAdapter.RoutineAdapterListener mListener;

    private DatabaseHelper db;
    private SQLiteCursorLoader mLoader;
    private RoutineListAdapter mAdapter;

    private boolean onCreate = true;

    private static boolean orderByDate = true;

    // Mandatory empty constructor for the fragment manager to instantiate the
    // fragment (e.g. upon screen orientation changes).
    public RoutineListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = DatabaseHelper.getInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routine_list, container, false);
        view.setTag(TAG);

        setupRoutineList(view);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onResume() {
        if (!onCreate) {
            getLoaderManager().restartLoader(0, null, this);
            Log.i(TAG, "restartLoader()");
        }

        onCreate = false;
        super.onResume();
    }

    private void setupRoutineList(View view) {
        // get RecyclerView and set empty view
        EmptyRecyclerView recyclerView = (EmptyRecyclerView) view.findViewById(R.id.routineList);
        recyclerView.setEmptyView(view.findViewById(R.id.routineList_empty));

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // LinearLayoutManager is used here, this will layout the elements in a similar fashion
        // to the way ListView would layout elements. The RecyclerView.LayoutManager defines how
        // elements are laid out.
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Set the adapter
        mAdapter = new RoutineListAdapter(getActivity(), null);
        recyclerView.setAdapter(mAdapter);

        mAdapter.setRoutineAdapterListener(mListener);
    }

    public static RoutineListFragment newInstance() {
        return new RoutineListFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = ((MainActivity) activity).getRoutineListListener();
            if (mListener == null)
                throw new ClassCastException(activity.toString()
                        + " must provide RoutineListListener");
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must provide RoutineListListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void deleteSelectedRoutines() {
        for (int id : mAdapter.getSelectedIds())
            db.dropWorkout(id);

        mAdapter.clearSelection();
        getLoaderManager().restartLoader(0, null, this);
    }

    // Loader Callbacks
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {

        final String r = "r", w = "w";

        String query = "SELECT " + r + ".*"
                + " FROM " + WORKOUTS + " " + r
                + " LEFT JOIN " + WORKOUTS + " " + w
                + " ON " + r + "." + _ID + " = " + w + "." + ROUTINE_ID
                + " WHERE " + r + "." + IS_ROUTINE + " = " + 1
                + " GROUP BY " + r + "." + _ID
                + " ORDER BY " + (orderByDate ? ("MAX("+w+"."+DATE+") DESC, "): "") + r + "." + NAME + " ASC";

        /*
        *   SELECT r.*
        *       FROM WORKOUTS r
        *       LEFT JOIN WORKOUTS w ON r._ID = w.ROUTINE_ID
        *       WHERE r.IS_ROUTINE = 1
        *       GROUP BY r._ID
        *       ORDER BY MAX(w.DATE) DESC, r.NAME ASC
        */

        mLoader = new SQLiteCursorLoader(getActivity(), db, query, null);
        return mLoader;
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        this.mLoader = (SQLiteCursorLoader) loader;
        mAdapter.changeCursor(cursor);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }

    public RoutineListAdapter getAdapter() {
        return mAdapter;
    }

}
