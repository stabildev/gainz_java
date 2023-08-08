package com.mycompany.gains.Activities.MainActivity.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;

import com.mycompany.gains.Activities.MainActivity.fragments.HistoryListFragment;
import com.mycompany.gains.Activities.MainActivity.fragments.RoutineListFragment;
import com.mycompany.gains.R;

public class MainPagerAdapter extends FragmentPagerAdapter {

    private Context context;
    private String[] title;
    private SparseArray<Fragment> registeredFragments = new SparseArray<>();
    private int activeWorkout;

    private static final int PAGE_COUNT = 2;

    //public static final int START_FRAGMENT = 0;
    public static final int ROUTINES_FRAGMENT = 0;
    public static final int HISTORY_FRAGMENT = 1;

    public MainPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;

        title = new String[]{
                context.getString(R.string.tab_routines),
                context.getString(R.string.tab_history) };
    }

    // Returns total number of pages
    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    // Returns the fragment to display for that page
    @Override
    public Fragment getItem(int pos) {
        switch (pos) {
            case HISTORY_FRAGMENT:
                return HistoryListFragment.newInstance();
            case ROUTINES_FRAGMENT:
                return RoutineListFragment.newInstance();
            default:
                return null;
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        registeredFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        registeredFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    // Returns the page title for the top indicator
    @Override
    public CharSequence getPageTitle(int pos) {
        switch (pos) {
        }
        if (0 <= pos && pos <= (title.length-1) ) {
            return title[pos];
        } else {
            return "null";
        }
    }

    public Fragment getRegisteredFragment(int position) {
        return registeredFragments.get(position);
    }
}