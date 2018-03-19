package com.github.vatbub.hearingaid.profileeditor;

/**
 * Created by frede on 19.03.2018.
 */

interface OnMoveAndSwipedListener {
    boolean onItemMove(int fromPosition, int toPosition);

    void onItemDismiss(int position);
}
