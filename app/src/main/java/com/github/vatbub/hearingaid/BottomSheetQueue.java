package com.github.vatbub.hearingaid;

import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.view.View;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Created by frede on 13.01.2018.
 */

public class BottomSheetQueue extends PriorityQueue<BottomSheetQueue.BottomSheetBehaviourWrapper> {
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

    private void showNextSheetIfApplicable() {
        synchronized (this) {
            if (isEmpty()) return;
            if (getCurrentBottomSheet() == null)
                showNextSheet();
            else if (getCurrentBottomSheet().compareTo(peek()) < 0) {
                BottomSheetBehaviourWrapper currentSheet = getCurrentBottomSheet();
                currentSheet.getBottomSheetCallback().onRescheduled();
                showNextSheet();
                // reschedule the hidden sheet
                this.add(currentSheet);
            }
        }
    }

    public BottomSheetBehaviourWrapper getCurrentBottomSheet() {
        return currentBottomSheet;
    }

    private void setCurrentBottomSheet(BottomSheetQueue.BottomSheetBehaviourWrapper currentBottomSheet) {
        this.currentBottomSheet = currentBottomSheet;
    }

    private void showNextSheet() {
        if (getCurrentBottomSheet() != null) {
            getCurrentBottomSheet().getBottomSheetBehavior().setState(BottomSheetBehavior.STATE_HIDDEN);
        }
        setCurrentBottomSheet(remove());

        final BottomSheetCallbackList callbacks = new BottomSheetCallbackList();

        CustomBottomSheetCallback queueCallback = new CustomBottomSheetCallback() {
            @Override
            public void onRescheduled() {
                if (callbacks.contains(this))
                    callbacks.queueForRemoval(this);
            }

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

        getCurrentBottomSheet().setBottomSheetCallback(new BottomSheetCallbackWrapper(callbacks));

        getCurrentBottomSheet().getBottomSheetBehavior().setState(getCurrentBottomSheet().getStateToUseForExpansion());
    }

    public enum BottomSheetPriority {
        LOW(-100), NORMAL(0), HIGH(100);

        private int numericPriority;

        BottomSheetPriority(int numericPriority) {
            this.numericPriority = numericPriority;
        }

        public int getNumericPriority() {
            return numericPriority;
        }
    }

    public static class BottomSheetBehaviourWrapper implements Comparable<BottomSheetBehaviourWrapper> {
        private BottomSheetBehavior bottomSheetBehavior;
        private int stateToUseForExpansion;
        private BottomSheetCallbackList additionalCallbacks;
        private BottomSheetPriority priority;
        private CustomBottomSheetCallback bottomSheetCallback;

        public BottomSheetBehaviourWrapper(BottomSheetBehavior bottomSheetBehavior) {
            this(bottomSheetBehavior, BottomSheetBehavior.STATE_EXPANDED);
        }

        public BottomSheetBehaviourWrapper(BottomSheetBehavior bottomSheetBehavior, @SuppressWarnings("SameParameterValue") int stateToUseForExpansion) {
            this(bottomSheetBehavior, stateToUseForExpansion, BottomSheetPriority.NORMAL);
        }

        public BottomSheetBehaviourWrapper(BottomSheetBehavior bottomSheetBehavior, @SuppressWarnings("SameParameterValue") int stateToUseForExpansion, BottomSheetPriority priority) {
            this(bottomSheetBehavior, stateToUseForExpansion, priority, new BottomSheetCallbackList());
        }

        public BottomSheetBehaviourWrapper(BottomSheetBehavior bottomSheetBehavior, int stateToUseForExpansion, BottomSheetPriority priority, BottomSheetCallbackList additionalCallbacks) {
            setBottomSheetBehavior(bottomSheetBehavior);
            setStateToUseForExpansion(stateToUseForExpansion);
            setAdditionalCallbacks(additionalCallbacks);
            setPriority(priority);
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

        /**
         * Compares this object with the specified object for order.  Returns a
         * negative integer, zero, or a positive integer as this object is less
         * than, equal to, or greater than the specified object.
         * <p>
         * <p>The implementor must ensure <tt>sgn(x.compareTo(y)) ==
         * -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
         * implies that <tt>x.compareTo(y)</tt> must throw an exception iff
         * <tt>y.compareTo(x)</tt> throws an exception.)
         * <p>
         * <p>The implementor must also ensure that the relation is transitive:
         * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt> implies
         * <tt>x.compareTo(z)&gt;0</tt>.
         * <p>
         * <p>Finally, the implementor must ensure that <tt>x.compareTo(y)==0</tt>
         * implies that <tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>, for
         * all <tt>z</tt>.
         * <p>
         * <p>It is strongly recommended, but <i>not</i> strictly required that
         * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally speaking, any
         * class that implements the <tt>Comparable</tt> interface and violates
         * this condition should clearly indicate this fact.  The recommended
         * language is "Note: this class has a natural ordering that is
         * inconsistent with equals."
         * <p>
         * <p>In the foregoing description, the notation
         * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
         * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
         * <tt>0</tt>, or <tt>1</tt> according to whether the value of
         * <i>expression</i> is negative, zero or positive.
         *
         * @param that the object to be compared.
         * @return a negative integer, zero, or a positive integer as this object
         * is less than, equal to, or greater than the specified object.
         * @throws NullPointerException if the specified object is null
         * @throws ClassCastException   if the specified object's type prevents it
         *                              from being compared to this object.
         */
        @Override
        public int compareTo(@NonNull BottomSheetBehaviourWrapper that) {
            if (this.getPriority().getNumericPriority() < that.getPriority().getNumericPriority())
                return -1;
            else if (this.getPriority().getNumericPriority() == that.getPriority().getNumericPriority())
                return 0;

            return 1;
        }

        public BottomSheetPriority getPriority() {
            return priority;
        }

        public void setPriority(BottomSheetPriority priority) {
            this.priority = priority;
        }

        private CustomBottomSheetCallback getBottomSheetCallback() {
            return bottomSheetCallback;
        }

        private void setBottomSheetCallback(CustomBottomSheetCallback bottomSheetCallback) {
            this.bottomSheetCallback = bottomSheetCallback;
            getBottomSheetBehavior().setBottomSheetCallback(bottomSheetCallback);
        }
    }

    public static class BottomSheetCallbackList extends ArrayList<CustomBottomSheetCallback> {
        private volatile List<BottomSheetBehavior.BottomSheetCallback> callbacksToBeRemoved;

        public BottomSheetCallbackList(int initialCapacity) {
            super(initialCapacity);
        }

        public BottomSheetCallbackList() {
        }

        public BottomSheetCallbackList(@NonNull Collection<? extends CustomBottomSheetCallback> c) {
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

        private List<BottomSheetBehavior.BottomSheetCallback> getCallbacksToBeRemoved() {
            if (callbacksToBeRemoved == null) {
                callbacksToBeRemoved = new ArrayList<>();
            }

            return callbacksToBeRemoved;
        }
    }

    public abstract static class CustomBottomSheetCallback extends BottomSheetBehavior.BottomSheetCallback{
        public abstract void onRescheduled();
    }

    /**
     * Allows a {@code BottomSheetBehaviour} to have multiple callbacks.
     */
    private class BottomSheetCallbackWrapper extends CustomBottomSheetCallback {
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
            for (CustomBottomSheetCallback callback : getCallbacks()) {
                callback.onStateChanged(bottomSheet, newState);
            }
            callbacks.removeQueuedCallbacks();
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            for (CustomBottomSheetCallback callback : getCallbacks()) {
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

        @Override
        public void onRescheduled() {
            for (CustomBottomSheetCallback callback : getCallbacks()) {
                callback.onRescheduled();
            }
            callbacks.removeQueuedCallbacks();
        }
    }
}
