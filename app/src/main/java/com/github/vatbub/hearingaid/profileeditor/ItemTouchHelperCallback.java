package com.github.vatbub.hearingaid.profileeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.DisplayMetrics;
import android.view.View;
import com.github.vatbub.hearingaid.R;

/**
 * Created by frede on 19.03.2018.
 */

public class ItemTouchHelperCallback extends ItemTouchHelper.Callback {
    private OnMoveAndSwipedListener moveAndSwipedListener;
    private Context callingContext;
    private Paint paintForViewUnderSwipe;
    private Bitmap deleteIconUnderSwipe;
    private Thread prepareViewsUnderSwipeThread;

    public ItemTouchHelperCallback(OnMoveAndSwipedListener listener, Context callingContext) {
        this.moveAndSwipedListener = listener;
        setCallingContext(callingContext);
        try {
            prepareViewsUnderSwipe();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public void prepareViewsUnderSwipe() throws InterruptedException {
        if (prepareViewsUnderSwipeThread != null)
            prepareViewsUnderSwipeThread.join();

        prepareViewsUnderSwipeThread = new Thread(() -> {
            paintForViewUnderSwipe = new Paint();
            paintForViewUnderSwipe.setColor(Color.RED);
            Bitmap icon = drawableToBitmap(getCallingContext().getResources().getDrawable(R.drawable.delete_profile_icon_white, getCallingContext().getTheme()));
            deleteIconUnderSwipe = Bitmap.createScaledBitmap(icon, convertDpToPx(30), convertDpToPx(30), true);
            icon.recycle();
        });

        prepareViewsUnderSwipeThread.start();
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        final int swipeFlags;
        if (recyclerView.getAdapter().getItemCount() == 1)
            swipeFlags = 0;
        else
            swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder
            viewHolder, RecyclerView.ViewHolder target) {
        // If the 2 items are not the same type, no dragging
        if (viewHolder.getItemViewType() != target.getItemViewType())
            return false;

        moveAndSwipedListener.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        moveAndSwipedListener.onItemDismiss(viewHolder.getAdapterPosition());
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            return;
        }

        // Taken from https://stackoverflow.com/a/33344173/5736633

        // Get RecyclerView item from the ViewHolder
        View itemView = ((ProfileViewHolder) viewHolder).getItemView();

        try {
            prepareViewsUnderSwipeThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (dX > 0) {
            // positive displacement --> to the right/end
            c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX,
                    (float) itemView.getBottom(), paintForViewUnderSwipe);

            c.drawBitmap(deleteIconUnderSwipe, (float) itemView.getLeft() + convertDpToPx(16), (float) itemView.getTop() + ((float) itemView.getBottom() - (float) itemView.getTop() - deleteIconUnderSwipe.getHeight()) / 2, paintForViewUnderSwipe);
        } else {
            // negative displacement --> to the left/start

            c.drawRect((float) itemView.getRight() + dX, (float) itemView.getTop(),
                    (float) itemView.getRight(), (float) itemView.getBottom(), paintForViewUnderSwipe);
            c.drawBitmap(deleteIconUnderSwipe, (float) itemView.getRight() - convertDpToPx(16) - deleteIconUnderSwipe.getWidth(), (float) itemView.getTop() + ((float) itemView.getBottom() - (float) itemView.getTop() - deleteIconUnderSwipe.getHeight()) / 2, paintForViewUnderSwipe);
        }

        itemView.setTranslationX(dX);
    }

    private int convertDpToPx(int dp) {
        return Math.round(dp * (getCallingContext().getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public Context getCallingContext() {
        return callingContext;
    }

    public void setCallingContext(Context callingContext) {
        this.callingContext = callingContext;
    }
}
