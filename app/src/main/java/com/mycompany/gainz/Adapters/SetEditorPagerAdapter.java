package com.mycompany.gainz.Adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;

import com.google.common.collect.SortedSetMultimap;
import com.mycompany.gainz.Comparators.ChronologicalComparator;
import com.mycompany.gainz.Database.Model.Set;
import com.mycompany.gainz.Fragments.SetEditorFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SetEditorPagerAdapter extends FixedFragmentStatePagerAdapter {
    private SortedSetMultimap<Integer, Set> mSets;
    private List<Set> mSetList;

    public SetEditorPagerAdapter(FragmentManager fm, SortedSetMultimap<Integer, Set> sets) {
        super(fm);
        mSets = sets;
        mSetList = new ArrayList<>(mSets.values().size());
        sortData();
    }

    private void sortData() {
        mSetList.clear();
        mSetList.addAll(mSets.values());
        Collections.sort(mSetList, new ChronologicalComparator());
    }

    @Override
    public void notifyDataSetChanged() {
        sortData();
        super.notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int pos) {
        SetEditorFragment fragment = new SetEditorFragment();
        fragment.setSet(mSetList.get(pos));
        return fragment;
    }

    public Set getSet(int pos) {
        if (mSetList.size() > 0)
            return mSetList.get(pos);
        else
            return null;
    }

    @Override
    public int getCount() {
        return mSetList.size();
    }

    // to fix the bug
    @Override
    public String getTag(int position) {
        return mSetList.get(position).getExercise().get_id() + "." + mSetList.get(position).get_id();
    }

    @Override
    public int getItemPosition(Object object) {
        return PagerAdapter.POSITION_NONE;
    }
}
