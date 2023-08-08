/*
 * Copyright (C) 2015 Paul Burke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mycompany.gains.Activities.WorkoutEditorOld.adapters.ItemTouchHelper;

import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import com.mycompany.gains.Adapters.DragSelectRecyclerAdapter2;
import com.mycompany.gains.widgets.EmptyRecyclerView;


/**
 * An implementation of {@link ItemTouchHelper.Callback} that enables basic drag & drop and
 * swipe-to-dismiss. Drag events are automatically started by an item long-press.<br/>
 * </br/>
 * Expects the <code>RecyclerView.Adapter</code> to listen for {@link
 * ItemTouchHelperAdapter} callbacks and the <code>RecyclerView.ViewHolder</code> to implement
 * {@link ItemTouchHelperViewHolder}.
 *
 * @author Paul Burke (ipaulpro)
 */
public class DragSelectItemTouchHelperCallback extends ItemTouchHelper.Callback {

    public static final float ALPHA_FULL = 1.0f;
    private int oldPos = -1;
    private int newPos = -1;

    private final ItemTouchHelperAdapter mAdapter;

    public DragSelectItemTouchHelperCallback(ItemTouchHelperAdapter adapter) {
        mAdapter = adapter;
    }

    // long press drag is disabled here but enabled manually in the viewholder
    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        final int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean canDropOver (RecyclerView recyclerView, RecyclerView.ViewHolder current, RecyclerView.ViewHolder target) {
        return true;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
        if (!((DragSelectRecyclerAdapter2.BaseViewHolder)source).isDragDropSelectEnabled()
                || !((DragSelectRecyclerAdapter2.BaseViewHolder)target).isDragDropSelectEnabled()) {
            return false;
        }

        // Notify the adapter of the move
        newPos = target.getAdapterPosition();
        mAdapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
        // Notify the adapter of the dismissal
        mAdapter.onItemDismiss(viewHolder.getAdapterPosition());
    }

/*    @Override
    public long getAnimationDuration(RecyclerView recyclerView, int animationType, float animateDx, float animateDy) {
        switch (animationType) {
            case ItemTouchHelper.ANIMATION_TYPE_DRAG:
                return (long) (0.5 * DEFAULT_DRAG_ANIMATION_DURATION);
            default:
                return DEFAULT_SWIPE_ANIMATION_DURATION;
        }
    }*/


    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

        if (recyclerView.getAdapter() instanceof EmptyRecyclerView.ContainsHeaders) {
            int offset = ((EmptyRecyclerView.ContainsHeaders) recyclerView.getAdapter()).getRowOffset();
            if (viewHolder.itemView.getY() < recyclerView.getChildAt(offset-1).getBottom())
                viewHolder.itemView.setY((float) recyclerView.getChildAt(offset-1).getBottom());
        }

        // Fade out the view as it is swiped out of the parent's bounds
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            View itemView = viewHolder.itemView;
            final float alpha = ALPHA_FULL - Math.abs(dX) / (float) itemView.getWidth();
            itemView.setAlpha(alpha);
        }
    }

/*    @Override
    public float getMoveThreshold (RecyclerView.ViewHolder viewHolder) {
        return 0;
    }*/

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {

        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            // Let the view holder know that this item is being moved or dragged
            ItemTouchHelperViewHolder itemViewHolder = (ItemTouchHelperViewHolder) viewHolder;
            itemViewHolder.onItemSelected();
        } else
            mAdapter.onActionFinished();
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            oldPos = viewHolder.getAdapterPosition();
        }
        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {

        if (oldPos != -1 && newPos != -1) {
            if (oldPos != newPos) {
                mAdapter.onItemMoved(oldPos, newPos);

                oldPos = -1;
                newPos = -1;
            }
        }

        super.clearView(recyclerView, viewHolder);

        viewHolder.itemView.setAlpha(ALPHA_FULL);

        // Tell the view holder it's time to restore the idle state
        ItemTouchHelperViewHolder itemViewHolder = (ItemTouchHelperViewHolder) viewHolder;
        itemViewHolder.onItemClear();
    }
}
