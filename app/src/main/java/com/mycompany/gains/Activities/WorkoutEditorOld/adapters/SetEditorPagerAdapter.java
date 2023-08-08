package com.mycompany.gains.Activities.WorkoutEditorOld.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.ViewGroup;

import com.mycompany.gains.Adapters.FixedFragmentStatePagerAdapter;
import com.mycompany.gains.Data.Model.Workout;
import com.mycompany.gains.Data.Model.Set;
import com.mycompany.gains.Activities.WorkoutEditorOld.fragments.SetEditorFragment;

import java.util.ArrayList;
import java.util.List;

public class SetEditorPagerAdapter extends FixedFragmentStatePagerAdapter {
    private List<Set> mSetList;
    private Workout workout;
    private SparseArray<SetEditorFragment> registeredFragments = new SparseArray<>();

    public SetEditorPagerAdapter(FragmentManager fm, Workout workout) {
        super(fm);
        this.workout = workout;
        mSetList = this.workout.getAllSets();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Log.d("LEL", "instantiateItem(container, position = " + position + ")");
        SetEditorFragment fragment = (SetEditorFragment) super.instantiateItem(container, position);
        registeredFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        registeredFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    public SetEditorFragment getRegisteredFragment(int position) {
        return registeredFragments.get(position);
    }

    public List<SetEditorFragment> getRegisteredFragments() {
        List<SetEditorFragment> fragments = new ArrayList<>(registeredFragments.size());
        for (int i = 0; i < registeredFragments.size(); i++) {
            fragments.add(registeredFragments.get(registeredFragments.keyAt(i)));
            Log.d("LEL", "i = " + i + ", key = " + registeredFragments.keyAt(i));
        }
        return fragments;
    }

    @Override
    public Fragment getItem(int pos) {
        SetEditorFragment fragment = SetEditorFragment.newInstance(pos);
        fragment.setSet(mSetList.get(pos));
        return fragment;
    }

    @Override
    public void notifyDataSetChanged() {
        mSetList = workout.getAllSets();
        super.notifyDataSetChanged();
    }

    public Set getSet(int pos) {
        if (mSetList.size() > 0)
            return mSetList.get(pos);
        else
            return null;
    }

    public void setData(List<Set> setList) {
        mSetList = setList;
    }

    @Override
    public int getCount() {
        return mSetList.size();
    }

    // to fix the bug
    @Override
    public String getTag(int position) {
        final Set set = mSetList.get(position);
        return set.getRow().getDatabaseId() + "." + set.getDatabaseId()
                + "." + set.getRow().getExercise().get_id();
    }

    @Override
    public int getItemPosition(Object object) {
        return PagerAdapter.POSITION_NONE;
    }

    public List<Set> getData() {
        return mSetList;
    }
}
