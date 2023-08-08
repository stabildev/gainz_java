package com.mycompany.gainz.Fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mycompany.gainz.Database.Model.Set;
import com.mycompany.gainz.R;

import java.math.BigDecimal;

public class SetEditorFragment extends Fragment {

    private Set mSet;

    private OnSetEditorFragmentInteractionListener mListener;

    public static SetEditorFragment newInstance(Bundle args) {
        SetEditorFragment fragment = new SetEditorFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public SetEditorFragment() {
        // Required empty public constructor
    }

    public void setSet(Set set) {
        this.mSet = set;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();

        //mSet = args.getParcelable("set");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v =  inflater.inflate(R.layout.fragment_set_editor, container, false);

        // find text views

        final TextView reps = (TextView) v.findViewById(R.id.setEditor_reps);
        final TextView weight = (TextView) v.findViewById(R.id.setEditor_weight);
        final TextView rest = (TextView) v.findViewById(R.id.setEditor_rest);

        v.findViewById(R.id.setEditor_icAdd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onSetEditorIconClick(mSet, "addSet");
            }
        });
        v.findViewById(R.id.setEditor_icNote).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onSetEditorIconClick(mSet, "addNote");
            }
        });
        v.findViewById(R.id.setEditor_icMore).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu overflow = new PopupMenu(getActivity(), v, Gravity.TOP) {
                    @Override public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
                        if (item.getItemId() == R.id.action_delete_set) {
                            mListener.onSetEditorIconClick(mSet, "deleteSet");
                            return true;
                        }
                        else if (item.getItemId() == R.id.action_skip_set) {
                            mListener.onSetEditorIconClick(mSet, "skipSet");
                            return true;
                        }
                        else
                            return super.onMenuItemSelected(menu, item);
                    }
                };
                overflow.inflate(R.menu.overflow_set_editor);
                if (mSet.getWorkout().setsInRows.get(mSet.getRow()).size() <= 1)
                    overflow.getMenu().removeItem(R.id.action_delete_set);
                overflow.show();
            }
        });

        // display information

            if (mSet.isDone()) {
                reps.setText("" + mSet.getReps());
                if (mSet.getWeight() > 0)   weight.setText(mSet.getFormattedWeight());
                if (mSet.getRest() > 0)     rest.setText("" + mSet.getRest());
            } else {
                if (mSet.getReps() > 0)     reps.setHint("" + mSet.getReps());
                if (mSet.getWeight() > 0)   weight.setHint(mSet.getFormattedWeight());
                if (mSet.getRest() > 0)     rest.setHint("" + mSet.getRest());
            }

        reps.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (reps.getText().length() > 0) {
                    int newval = Integer.parseInt(reps.getText().toString());
                    if (!hasFocus && newval != mSet.getReps()) {
                        mSet.setIsDone(true);
                        mSet.setReps(newval);
                        mListener.onSetChanged(mSet);
                    }
                }
            }
        });

        weight.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (weight.getText().length() > 0) {
                    if (!hasFocus && !weight.getText().toString().equals(mSet.getFormattedWeight())) {
                        mSet.setIsDone(true);
                        mSet.setFormattedWeight(weight.getText().toString());
                        mListener.onSetChanged(mSet);
                    }
                }
            }
        });
        rest.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (rest.getText().length() > 0) {
                    int newval = Integer.parseInt(rest.getText().toString());
                    if (!hasFocus && newval != mSet.getRest()) {
                        mSet.setIsDone(true);
                        mSet.setRest(newval);
                        mListener.onSetChanged(mSet);
                    }
                }
            }
        });
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnSetEditorFragmentInteractionListener) activity;
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

    public interface OnSetEditorFragmentInteractionListener {
        void onSetChanged(Set set);
        void onSetEditorIconClick(Set set, String s);
    }
}
