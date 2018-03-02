package com.cpjd.robluscouter.ui.checkouts;

import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.Toast;

import com.cpjd.robluscouter.R;
import com.cpjd.robluscouter.io.IO;
import com.cpjd.robluscouter.models.RCheckout;
import com.cpjd.robluscouter.models.RSettings;
import com.cpjd.robluscouter.ui.dialogs.FastDialogBuilder;
import com.cpjd.robluscouter.utils.Constants;
import com.cpjd.robluscouter.utils.HandoffStatus;
import com.cpjd.robluscouter.utils.Utils;

/**
 * Defines gestures & other attributes available to the RCheckout recycler view cards
 *
 * @author Will Davies
 * @version 3
 * @since 1.0.0
 */
public class RecyclerViewTouchHelper extends ItemTouchHelper.SimpleCallback {
    /**
     * References settings model
     */
    private RSettings settings;
    /**
     * References our backend checkouts adapter adapter
     */
    private final RecyclerViewAdapter checkoutsAdapter;
    /**
     * Stores the mode to operate under, defined by "RecyclerViewAdapter" constants in
     * @see com.cpjd.robluscouter.utils.Constants
     */
    private int mode;

    /*
     * Helper variables for UI icons
     */
    private final Drawable xMark, cMark;
    private final int xMarkMargin;

    RecyclerViewTouchHelper(RecyclerViewAdapter checkoutsAdapter, RSettings settings, int mode) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.checkoutsAdapter = checkoutsAdapter;
        this.settings = settings;
        this.mode = mode;

        xMark = ContextCompat.getDrawable(checkoutsAdapter.getContext(), R.drawable.add_small);
        if(xMark != null) xMark.setColorFilter(settings.getRui().getButtons(), PorterDuff.Mode.SRC_ATOP);
        xMarkMargin = 100;

        cMark = ContextCompat.getDrawable(checkoutsAdapter.getContext(), R.drawable.confirm);
        if(cMark != null) cMark.setColorFilter(settings.getRui().getButtons(), PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
        // Load IO, we'll need it for file system access
        final IO io = new IO(checkoutsAdapter.getContext());
        settings = io.loadSettings();
        // Get the checkout model that was swiped
        final RCheckout checkout = checkoutsAdapter.getCheckouts().get(viewHolder.getAdapterPosition());

        // The checkout model was swiped to the LEFT, that means the user wants to checkout the checkout for themselves
        if(direction == ItemTouchHelper.LEFT) {
            // Update status to let other scouters know we're checking this one out
            checkout.setStatus(HandoffStatus.CHECKED_OUT);
            checkout.setNameTag(settings.getName());
            checkout.setTime(System.currentTimeMillis());
            io.saveMyCheckout(checkout);
            io.saveCheckout(checkout);
            io.savePendingCheckout(checkout);
            // If the user doesn't want to see currently checked out items, remove this from the list
            if(mode == Constants.CHECKOUTS) {
                if(!settings.isShowCheckedOut()) checkoutsAdapter.remove(viewHolder.getAdapterPosition());
                else checkoutsAdapter.reAdd(checkout);
            }
            checkoutsAdapter.notifyDataSetChanged();
            Utils.requestUIRefresh(checkoutsAdapter.getContext(), true, false);
        }
        // The checkout model was swiped to the RIGHT, that means the user wants to upload the checkout, they're done with it
        else if(direction == ItemTouchHelper.RIGHT) {
            /*
             * Disallow checking out checkouts without a team code
             */
            if(settings.getCode() == null || settings.getCode().equals("")) {
                Toast.makeText(checkoutsAdapter.getContext(), "Unable to complete a checkout without a team code specified in settings.", Toast.LENGTH_LONG).show();
                checkoutsAdapter.reAdd(checkout);
                return;
            }

            /*
             * Run a verification check - the user shouldn't be able to check out items greater than index > 0 in the adapter,
             * unless the items before it are upload pending
             */
            if(viewHolder.getAdapterPosition() > 0) {
                boolean allCompletedBefore = true;
                for(int i = 0; i < viewHolder.getAdapterPosition(); i++) {
                    if(checkoutsAdapter.getCheckouts().get(i).getStatus() == HandoffStatus.CHECKED_OUT) allCompletedBefore = false;
                }
                if(!allCompletedBefore) {
                    /*
                     * The user tried to complete a match that wasn't at index 0,
                     * deny them access
                     */
                    new FastDialogBuilder()
                            .setTitle("Error")
                            .setMessage("You can't complete this match before you've completed earlier matches before it.")
                            .setPositiveButtonText("Ok")
                            .setNeutralButtonText("Override")
                            .setFastDialogListener(new FastDialogBuilder.FastDialogListener() {
                                @Override
                                public void accepted() {
                                    checkoutsAdapter.reAdd(checkout);
                                }

                                @Override
                                public void denied() {

                                }

                                @Override
                                public void neutral() {
                                    // User decided to overwrite, checkout the thing then
                                    uploadCheckout(io, checkout);
                                }
                            }).build(checkoutsAdapter.getContext());
                } else {
                    uploadCheckout(io, checkout);
                }
            } else {
                uploadCheckout(io, checkout);
            }

        }
    }

    private void uploadCheckout(IO io, RCheckout checkout) {
        checkout.setStatus(HandoffStatus.COMPLETED);
        checkout.setNameTag(settings.getName());
        checkout.setTime(System.currentTimeMillis());
        io.saveMyCheckout(checkout);
        io.savePendingCheckout(checkout);
        // The checkouts adapter will need to reload status in the my checkouts tab
        checkoutsAdapter.reAdd(checkout);
    }

    /**
     * This is an important method. It locks/unlocks swipe directions which depends on what tab we are in.
     */
    @Override
    public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        if(checkoutsAdapter.getCheckouts().get(viewHolder.getAdapterPosition()).getStatus() == HandoffStatus.COMPLETED && mode == Constants.MY_CHECKOUTS) return 0; // disable swiping
        else if(mode == Constants.CHECKOUTS) return ItemTouchHelper.LEFT;
        else if(mode == Constants.MY_CHECKOUTS) return ItemTouchHelper.RIGHT;
        else return 0;
    }

    /*
     * The rest of this class just manages drawing for the cards and some other basic settings
     */
    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;

        if(viewHolder.getAdapterPosition() == -1) return;

        int itemHeight = itemView.getBottom() - itemView.getTop();
        int intrinsicWidth = xMark.getIntrinsicWidth();
        int intrinsicHeight = xMark.getIntrinsicWidth();

        int xMarkLeft = itemView.getRight() - xMarkMargin - intrinsicWidth;
        int xMarkRight = itemView.getRight() - xMarkMargin;
        int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
        int xMarkBottom = xMarkTop + intrinsicHeight;
        xMark.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);
        xMarkLeft = itemView.getLeft() + xMarkMargin;
        xMarkRight = itemView.getLeft() + intrinsicWidth + xMarkMargin;
        cMark.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);
        if(dX > 0) cMark.draw(c);
        else if(dX < 0) xMark.draw(c);

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return super.getMovementFlags(recyclerView, viewHolder);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }
}
