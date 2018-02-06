package com.github.vatbub.hearingaid;

import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.view.View;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Enables multiple bottom sheets to be shown one after another.
 */

public class BottomSheetQueue extends LinkedList<BottomSheetQueue.BottomSheetBehaviourWrapper> {
    private BottomSheetQueue.BottomSheetBehaviourWrapper currentBottomSheet;

    public BottomSheetQueue() {
    }

    public BottomSheetQueue(@NonNull Collection<? extends BottomSheetQueue.BottomSheetBehaviourWrapper> values) {
        super(values);
    }

    @Override
    public boolean offer(BottomSheetQueue.BottomSheetBehaviourWrapper e) {
        boolean result = super.offer(e);
        if (result) {
            showNextSheetIfApplicable();
        }
        return result;
    }

    @Override
    public boolean add(BottomSheetQueue.BottomSheetBehaviourWrapper e) {
        boolean result = super.add(e);
        if (result) {
            showNextSheetIfApplicable();
        }
        return result;
    }

    @Override
    public boolean addAll(Collection<? extends BottomSheetQueue.BottomSheetBehaviourWrapper> c) {
        boolean result = super.addAll(c);
        if (result) {
            showNextSheetIfApplicable();
        }
        return result;
    }

    @Override
    public boolean addAll(int index, Collection<? extends BottomSheetQueue.BottomSheetBehaviourWrapper> c) {
        boolean result = super.addAll(index, c);
        if (result) {
            showNextSheetIfApplicable();
        }
        return result;
    }

    @Override
    public boolean offerFirst(BottomSheetQueue.BottomSheetBehaviourWrapper bottomSheetBehavior) {
        boolean result = super.offerFirst(bottomSheetBehavior);
        if (result) {
            showNextSheetIfApplicable();
        }
        return result;
    }

    @Override
    public boolean offerLast(BottomSheetQueue.BottomSheetBehaviourWrapper bottomSheetBehavior) {
        boolean result = super.offerLast(bottomSheetBehavior);
        if (result) {
            showNextSheetIfApplicable();
        }
        return result;
    }

    @Override
    public BottomSheetQueue.BottomSheetBehaviourWrapper set(int index, BottomSheetQueue.BottomSheetBehaviourWrapper element) {
        throw new UnsupportedOperationException();
    }

    private void showNextSheetIfApplicable() {
        synchronized (this) {
            if (!isEmpty() && getCurrentBottomSheet() == null)
                showNextSheet();
        }
    }

    /**
     * Returns the {@link BottomSheetBehaviourWrapper} that describes the bottom sheet that is currently on screen
     * @return the {@link BottomSheetBehaviourWrapper} that describes the bottom sheet that is currently on screen
     */
    public BottomSheetQueue.BottomSheetBehaviourWrapper getCurrentBottomSheet() {
        return currentBottomSheet;
    }

    private void setCurrentBottomSheet(BottomSheetQueue.BottomSheetBehaviourWrapper currentBottomSheet) {
        this.currentBottomSheet = currentBottomSheet;
    }

    /**
     * Hides the bottom sheet that is currently on screen (if one is currently shown) and immediately shows the next one.
     */
    public void showNextSheet() {
        if (getCurrentBottomSheet() != null) {
            getCurrentBottomSheet().getBottomSheetBehavior().setState(BottomSheetBehavior.STATE_HIDDEN);
        }
        setCurrentBottomSheet(removeFirst());

        final BottomSheetCallbackList callbacks = new BottomSheetCallbackList();

        BottomSheetBehavior.BottomSheetCallback queueCallback = new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    setCurrentBottomSheet(null);
                    if (callbacks.contains(this))
                        callbacks.queueForRemoval(this);
                    showNextSheetIfApplicable();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        };

        callbacks.add(queueCallback);
        if (getCurrentBottomSheet().getAdditionalCallbacks() != null)
            callbacks.addAll(getCurrentBottomSheet().getAdditionalCallbacks());

        getCurrentBottomSheet().getBottomSheetBehavior().setBottomSheetCallback(new BottomSheetCallbackWrapper(callbacks));

        getCurrentBottomSheet().getBottomSheetBehavior().setState(getCurrentBottomSheet().getStateToUseForExpansion());
    }

    public static class BottomSheetBehaviourWrapper {
        private BottomSheetBehavior bottomSheetBehavior;
        private int stateToUseForExpansion;
        private BottomSheetCallbackList additionalCallbacks;

        public BottomSheetBehaviourWrapper(BottomSheetBehavior bottomSheetBehavior) {
            this(bottomSheetBehavior, BottomSheetBehavior.STATE_EXPANDED);
        }

        public BottomSheetBehaviourWrapper(BottomSheetBehavior bottomSheetBehavior, int stateToUseForExpansion) {
            this(bottomSheetBehavior, stateToUseForExpansion, new BottomSheetCallbackList());
        }

        public BottomSheetBehaviourWrapper(BottomSheetBehavior bottomSheetBehavior, int stateToUseForExpansion, BottomSheetCallbackList additionalCallbacks) {
            setBottomSheetBehavior(bottomSheetBehavior);
            setStateToUseForExpansion(stateToUseForExpansion);
            setAdditionalCallbacks(additionalCallbacks);
        }

        public BottomSheetBehavior getBottomSheetBehavior() {
            return bottomSheetBehavior;
        }

        public void setBottomSheetBehavior(BottomSheetBehavior bottomSheetBehavior) {
            this.bottomSheetBehavior = bottomSheetBehavior;
        }

        public int getStateToUseForExpansion() {
            return stateToUseForExpansion;
        }

        public void setStateToUseForExpansion(int stateToUseForExpansion) {
            this.stateToUseForExpansion = stateToUseForExpansion;
        }

        public BottomSheetCallbackList getAdditionalCallbacks() {
            return additionalCallbacks;
        }

        public void setAdditionalCallbacks(BottomSheetCallbackList additionalCallbacks) {
            this.additionalCallbacks = additionalCallbacks;
        }
    }

    public static class BottomSheetCallbackList extends ArrayList<BottomSheetBehavior.BottomSheetCallback> {
        private volatile List<BottomSheetBehavior.BottomSheetCallback> callbacksToBeRemoved;

        public BottomSheetCallbackList(int initialCapacity) {
            super(initialCapacity);
        }

        public BottomSheetCallbackList() {
        }

        public BottomSheetCallbackList(@NonNull Collection<? extends BottomSheetBehavior.BottomSheetCallback> c) {
            super(c);
        }

        public void queueForRemoval(BottomSheetBehavior.BottomSheetCallback callbackToBeRemoved) {
            getCallbacksToBeRemoved().add(callbackToBeRemoved);
        }

        public void removeQueuedCallbacks() {
            synchronized (this) {
                while (!getCallbacksToBeRemoved().isEmpty()) {
                    this.remove(getCallbacksToBeRemoved().remove(0));
                }
            }
        }

        private List<BottomSheetBehavior.BottomSheetCallback> getCallbacksToBeRemoved(){
            if (callbacksToBeRemoved==null){
                callbacksToBeRemoved = new ArrayList<>();
            }

            return callbacksToBeRemoved;
        }
    }

    /**
     * Allows a {@code BottomSheetBehaviour} to have multiple callbacks.
     */
    private class BottomSheetCallbackWrapper extends BottomSheetBehavior.BottomSheetCallback {
        private BottomSheetCallbackList callbacks;

        /**
         * Creates a wrapper for the specified callbacks.
         *
         * @param callbacks The callbacks that will be called in order.
         */
        public BottomSheetCallbackWrapper(BottomSheetCallbackList callbacks) {
            setCallbacks(callbacks);
        }

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            for (BottomSheetBehavior.BottomSheetCallback callback : getCallbacks()) {
                callback.onStateChanged(bottomSheet, newState);
            }
            callbacks.removeQueuedCallbacks();
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            for (BottomSheetBehavior.BottomSheetCallback callback : getCallbacks()) {
                callback.onSlide(bottomSheet, slideOffset);
            }
            callbacks.removeQueuedCallbacks();
        }

        public BottomSheetCallbackList getCallbacks() {
            return callbacks;
        }

        public void setCallbacks(BottomSheetCallbackList callbacks) {
            this.callbacks = callbacks;
        }
    }
}
