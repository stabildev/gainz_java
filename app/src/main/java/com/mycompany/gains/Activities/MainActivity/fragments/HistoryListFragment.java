package com.mycompany.gains.Activities.MainActivity.fragments;

import android.app.Activity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mycompany.gains.Adapters.MSCursorRecyclerAdapter;
import com.mycompany.gains.Globals;
import com.mycompany.gains.widgets.EmptyRecyclerView;
import com.mycompany.gains.Activities.MainActivity.adapters.HistoryListAdapter;
import com.mycompany.gains.SQLiteLoader.SQLiteCursorLoader;
import com.mycompany.gains.Data.Database.DatabaseHelper;
import com.mycompany.gains.R;

import static com.mycompany.gains.Data.Database.DatabaseConstants.*;

public class HistoryListFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "HistoryListFragment";

    private MSCursorRecyclerAdapter.OnSelectionListener mListener;

    private DatabaseHelper db;
    private SQLiteCursorLoader mLoader;
    private HistoryListAdapter mAdapter;

    private boolean onCreate = true;

    // Mandatory empty constructor for the fragment manager to instantiate the
    // fragment (e.g. upon screen orientation changes).
    public HistoryListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = DatabaseHelper.getInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history_list, container, false);
        view.setTag(TAG);

        setupHistoryList(view);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
        Log.i(TAG, "initLoader()");
    }

    @Override
    public void onResume() {
        if (!onCreate) {
            getLoaderManager().restartLoader(0, null, this);
        }

        onCreate = false;
        super.onResume();
    }

    private void setupHistoryList(View view) {

        EmptyRecyclerView recyclerView = (EmptyRecyclerView) view.findViewById(R.id.historyList);
        recyclerView.setEmptyView(view.findViewById(R.id.historyList_empty));

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // LinearLayoutManager is used here, this will layout the elements in a similar fashion
        // to the way ListView would layout elements. The RecyclerView.LayoutManager defines how
        // elements are laid out.
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Set the adapter
        mAdapter = new HistoryListAdapter(getActivity(), null);
        recyclerView.setAdapter(mAdapter);

        mAdapter.setOnSelectionListener(mListener);
    }

    public static HistoryListFragment newInstance() {
        return new HistoryListFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = ((MSCursorRecyclerAdapter.SelectionListener) activity).getOnSelectionListener(this);
            if (mListener == null)
                throw new ClassCastException(activity.toString()
                        + " must implement MSCursorRecyclerAdapter.SelectionListener");
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MSCursorRecyclerAdapter.SelectionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void deleteSelectedWorkouts() {
        for (int i : mAdapter.getSelectedIds()) {
            db.dropWorkout(i);
        }

        mAdapter.clearSelection();
        getLoaderManager().restartLoader(0, null, this);
    }

    // Loader Callbacks
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        String fields = TextUtils.join(",",new String[]{_ID, NAME, DATE, NOTE});
        String query = "SELECT " + fields + " FROM " + WORKOUTS
                + " WHERE " + IS_ROUTINE + " = " + 0
                + " AND " + _ID + " != " + Globals.getInstance().getActiveWorkoutId()
                + " ORDER BY " + DATE + " DESC";

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

    public HistoryListAdapter getAdapter() {
        return mAdapter;
    }
}
