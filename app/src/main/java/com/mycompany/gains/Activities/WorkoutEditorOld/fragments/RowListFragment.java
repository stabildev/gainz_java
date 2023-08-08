package com.mycompany.gains.Activities.WorkoutEditorOld.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mycompany.gains.Activities.WorkoutEditorOld.WorkoutEditorOld;
import com.mycompany.gains.Activities.WorkoutEditorOld.adapters.ItemTouchHelper.DragSelectItemTouchHelperCallback;
import com.mycompany.gains.Activities.WorkoutEditorOld.adapters.RowListAdapter;
import com.mycompany.gains.Data.Model.Set;
import com.mycompany.gains.Data.Model.Workout;
import com.mycompany.gains.R;
import com.mycompany.gains.widgets.EmptyRecyclerView;

import org.solovyev.android.views.llm.LinearLayoutManager;


public class RowListFragment extends Fragment implements RowListAdapter.RowListListener {
    private RowListAdapter mAdapter;
    private ItemTouchHelper mItemTouchHelper;

    // Mandatory empty constructor for the fragment manager to instantiate the
    // fragment (e.g. upon screen orientation changes).
    public RowListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_row_list_view, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        EmptyRecyclerView recyclerView = (EmptyRecyclerView) view.findViewById(R.id.rowList);
        recyclerView.setEmptyView(view.findViewById(R.id.rowList_empty));

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(false);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mAdapter = new RowListAdapter(getActivity(), this, .01f, getWorkout());
        recyclerView.setAdapter(mAdapter);

        recyclerView.setItemAnimator(new DefaultItemAnimator() {
            @Override
            public long getMoveDuration() {
                return (long) (0.6 * super.getMoveDuration());
            }
        });

        ItemTouchHelper.Callback callback = new DragSelectItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        // disable haptic feedback for the start of the drag
        viewHolder.itemView.setHapticFeedbackEnabled(false);
        mItemTouchHelper.startDrag(viewHolder);
        viewHolder.itemView.setHapticFeedbackEnabled(true);
    }

    @Override
    public void onItemDismiss(int position) {
        getWorkoutEditor().deleteSuperset(position);
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        getWorkout().moveSuperset(fromPosition, toPosition);
    }

    @Override
    public void onItemMoved(int fromPosition, int toPosition) {
        getWorkoutEditor().moveSuperset(fromPosition, toPosition);
    }

    @Override
    public void onClick(int position, int subPosition) {
        getWorkoutEditor().goToRow(position, subPosition);
    }

    @Override
    public void onSelectionChanged(int selectionSize, int subSelectionSize) {
        getWorkoutEditor().onSupersetSelectionChanged(selectionSize, subSelectionSize);
    }

    @Override
    public void onMetaDataChanged() {
        getWorkoutEditor().updateMetaData();
    }

    @Override
    public void onMoveRow(int supersetPos, int rowPos, boolean up){
        getWorkoutEditor().moveRow(supersetPos, rowPos, up);
    }

    @Override
    public void onSetClicked(Set set, boolean longClick) {
        if (longClick)
            getWorkoutEditor().showSetPopup(set);
        else
            getWorkoutEditor().goToSet(set);
    }

    @Override
    public void onAddSetClicked(int supersetPosition, int rowPosition) {
        getWorkoutEditor().addSet(supersetPosition, rowPosition);
    }

    public RowListAdapter getAdapter() {
        return mAdapter;
    }

    public Workout getWorkout() {
        return ((WorkoutEditorOld) getActivity()).getWorkout();
    }

    public WorkoutEditorOld getWorkoutEditor() {
        return (WorkoutEditorOld) getActivity();
    }
}
