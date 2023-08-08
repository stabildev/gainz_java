package com.mycompany.gains.widgets;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;



public class EmptyRecyclerView extends RecyclerView {
    // fixes scrollTo error

    private static final String TAG = "EmptyRecyclerView";

    @Nullable
    View emptyView;

    public EmptyRecyclerView(Context context) { super(context); }

    public EmptyRecyclerView(Context context, AttributeSet attrs) { super(context, attrs); }

    public EmptyRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public interface ContainsHeaders {
        int getRowOffset();
    }

    void checkIfEmpty() {
        if (emptyView != null && getAdapter() != null) {
            int offset = (getAdapter() instanceof ContainsHeaders) ? ((ContainsHeaders) getAdapter()).getRowOffset() : 0;
            emptyView.setVisibility(getAdapter().getItemCount() > offset ? GONE : VISIBLE);
        }
    }

    final @NonNull AdapterDataObserver observer = new AdapterDataObserver() {

        @Override public void onChanged() {
            super.onChanged();
            checkIfEmpty();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            super.onItemRangeInserted(positionStart, itemCount);
            checkIfEmpty();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            super.onItemRangeRemoved(positionStart, itemCount);
            checkIfEmpty();
        }
    };

    @Override public void setAdapter(@Nullable Adapter adapter) {
        final Adapter oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(observer);
        }
        super.setAdapter(adapter);
        if (adapter != null) {
            adapter.registerAdapterDataObserver(observer);
        }
        checkIfEmpty();
    }

    public void setEmptyView(@Nullable View emptyView) {
        this.emptyView = emptyView;
        checkIfEmpty();
    }

    @Override
    public void scrollTo(int x, int y) {
        // Log.e(TAG, TAG + " does not support scrolling to an absolute position.");
        // Either don't call super here or call just for some phones, or try catch it. From default implementation we have removed the Runtime Exception trown
    }
}
