package com.mycompany.gainz.Fragments;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mycompany.gainz.Adapters.RoutineListAdapter;
import com.mycompany.gainz.Classes.EmptyRecyclerView;
import com.mycompany.gainz.Database.Helper.DBHelper;
import com.mycompany.gainz.R;
import com.mycompany.gainz.SQLiteLoader.SQLiteCursorLoader;

import java.util.ArrayList;
import java.util.List;


public class RoutineListFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "RoutineListWindow";

    private OnFragmentInteractionListener mListener;

    private DBHelper db;
    private SQLiteCursorLoader mLoader;
    private EmptyRecyclerView mRecyclerView;
    private RoutineListAdapter mAdapter;

    private List<Integer> selection;

    private boolean onCreate = true;

    // Mandatory empty constructor for the fragment manager to instantiate the
    // fragment (e.g. upon screen orientation changes).
    public RoutineListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = DBHelper.getInstance(getActivity());
        selection = new ArrayList<>();
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
        mRecyclerView = (EmptyRecyclerView) view.findViewById(R.id.routineList);
        mRecyclerView.setEmptyView(view.findViewById(R.id.routineList_empty));

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // LinearLayoutManager is used here, this will layout the elements in a similar fashion
        // to the way ListView would layout elements. The RecyclerView.LayoutManager defines how
        // elements are laid out.
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Set the adapter
        mAdapter = new RoutineListAdapter(getActivity(), null);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnSelectionListener(new RoutineListAdapter.OnSelectionListener() {

            @Override
            public void onStartSelection() {
                mListener.onStartRoutineSelection();
            }

            @Override
            public void onSelectItem(int _id) {
                selection.add(_id);
            }

            @Override
            public void onDeselectItem(int _id) {
                selection.remove(new Integer(_id));
            }

            @Override
            public void onDeselectAll() {
                selection.clear();
                mListener.onStopRoutineSelection();
            }

            @Override
            public void onClick(int _id) {
                mListener.onClickRoutine(_id);
            }
        });
    }

    public static RoutineListFragment newInstance() {
        return new RoutineListFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onStartRoutineSelection();
        void onStopRoutineSelection();
        void onClickRoutine(int _id);
    }

    public void clearSelection() {
        mAdapter.clearSelection();
    }

    public void deleteSelectedRoutines() {
        for (int i = 0; i < selection.size(); i++)
            db.dropWorkout(selection.get(i));

        mAdapter.clearSelection();
        getLoaderManager().restartLoader(0, null, this);
    }

    public List<Integer> getSelection() {
        return selection;
    }

    // Loader Callbacks
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        String fields = TextUtils.join(",", new String[]{DBHelper._ID, DBHelper.NAME});
        String query = "SELECT " + fields + " FROM " + DBHelper.WORKOUTS
                + " WHERE " + DBHelper.IS_ROUTINE + " = " + 1
                + " ORDER BY " + DBHelper.NAME + " ASC";

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


}
