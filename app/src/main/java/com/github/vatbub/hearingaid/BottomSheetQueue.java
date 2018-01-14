package com.github.vatbub.hearingaid;

import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.view.View;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by frede on 13.01.2018.
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

    public BottomSheetQueue.BottomSheetBehaviourWrapper getCurrentBottomSheet() {
        return currentBottomSheet;
    }

    private void setCurrentBottomSheet(BottomSheetQueue.BottomSheetBehaviourWrapper currentBottomSheet) {
        this.currentBottomSheet = currentBottomSheet;
    }

    public void showNextSheet() {
        if (getCurrentBottomSheet() != null) {
            getCurrentBottomSheet().getBottomSheetBehavior().setState(BottomSheetBehavior.STATE_HIDDEN);
        }
        setCurrentBottomSheet(removeFirst());

        getCurrentBottomSheet().getBottomSheetBehavior().setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                for(BottomSheetBehavior.BottomSheetCallback callback:getCurrentBottomSheet().getAdditionalCallbacks()){
                    callback.onStateChanged(bottomSheet, newState);
                }

                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    setCurrentBottomSheet(null);
                    showNextSheetIfApplicable();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                for(BottomSheetBehavior.BottomSheetCallback callback:getCurrentBottomSheet().getAdditionalCallbacks()){
                    callback.onSlide(bottomSheet, slideOffset);
                }
            }
        });

        getCurrentBottomSheet().getBottomSheetBehavior().setState(getCurrentBottomSheet().getStateToUseForExpansion());
    }

    public static class BottomSheetBehaviourWrapper {
        private BottomSheetBehavior bottomSheetBehavior;
        private int stateToUseForExpansion;
        private List<BottomSheetBehavior.BottomSheetCallback> additionalCallbacks;

        public BottomSheetBehaviourWrapper(BottomSheetBehavior bottomSheetBehavior) {
            this(bottomSheetBehavior, BottomSheetBehavior.STATE_EXPANDED);
        }

        public BottomSheetBehaviourWrapper(BottomSheetBehavior bottomSheetBehavior, int stateToUseForExpansion) {
            this(bottomSheetBehavior, stateToUseForExpansion, new ArrayList<BottomSheetBehavior.BottomSheetCallback>());
        }

        public BottomSheetBehaviourWrapper(BottomSheetBehavior bottomSheetBehavior, int stateToUseForExpansion, List<BottomSheetBehavior.BottomSheetCallback> additionalCallbacks) {
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

        public List<BottomSheetBehavior.BottomSheetCallback> getAdditionalCallbacks() {
            return additionalCallbacks;
        }

        public void setAdditionalCallbacks(List<BottomSheetBehavior.BottomSheetCallback> additionalCallbacks) {
            this.additionalCallbacks = additionalCallbacks;
        }
    }
}
