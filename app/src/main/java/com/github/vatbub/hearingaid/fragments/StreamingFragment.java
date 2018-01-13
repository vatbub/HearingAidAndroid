package com.github.vatbub.hearingaid.fragments;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.github.vatbub.hearingaid.CardDataAdapter;
import com.github.vatbub.hearingaid.R;
import com.ohoussein.playpause.PlayPauseView;

import java.util.ArrayList;

import static android.content.pm.PackageManager.PERMISSION_DENIED;

public class StreamingFragment extends Fragment {
    private static final String SUPERPOWERED_INITIALIZED_BUNDLE_KEY = "superpoweredInitialized";
    private static final String IS_STREAMING_BUNDLE_KEY = "isStreaming";

    static {
        System.loadLibrary("HearingAidAudioProcessor");
    }

    private boolean isStreaming;
    private View createdView;
    private boolean superpoweredInitialized = false;
    private ArrayList<String> countries;

    public StreamingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permissions.length == 0 || grantResults.length == 0 || grantResults[0] == PERMISSION_DENIED) {
            setStreaming(false);
            ((PlayPauseView) findViewById(R.id.mainToggleButton)).change(!isStreamingEnabled());
            return;
        }

        updateStreamingState();
    }

    public boolean isStreamingEnabled() {
        return isStreaming;
    }

    private void updateStreamingState() {
        initSuperpoweredIfNotInitialized();
        if (isStreamingEnabled()) {
            Snackbar.make(findViewById(R.id.fragment_content), R.string.fragment_streaming_snackbar_start_streaming, 3000).show();
        } else {
            Snackbar.make(findViewById(R.id.fragment_content), R.string.fragment_streaming_snackbar_stop_streaming, 3000).show();
        }
        onPlayPause(isStreamingEnabled());
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean allPermissionsGranted() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            return false;

        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            superpoweredInitialized = savedInstanceState.getBoolean(SUPERPOWERED_INITIALIZED_BUNDLE_KEY);
            setStreaming(savedInstanceState.getBoolean(IS_STREAMING_BUNDLE_KEY));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SUPERPOWERED_INITIALIZED_BUNDLE_KEY, superpoweredInitialized);
        outState.putBoolean(IS_STREAMING_BUNDLE_KEY, isStreamingEnabled());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_streaming, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        createdView = view;

        findViewById(R.id.mainToggleButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setStreaming(!isStreamingEnabled());
                ((PlayPauseView) v).change(!isStreamingEnabled());
                if (!allPermissionsGranted()) {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                } else {
                    updateStreamingState();
                }
            }
        });

        initRecyclerView();
        initPlayButtonMarginHandler();
    }

    private <T extends View> T findViewById(@IdRes int id) {
        return createdView.findViewById(id);
    }

    /**
     * Initializes the superpowered sdk and associated c++ code.
     * No-op if already initialized.
     */
    private void initSuperpoweredIfNotInitialized() {
        if (superpoweredInitialized)
            return;

        // Get the device's sample rate and buffer size to enable low-latency Android audio io, if available.
        String samplerateString = null, buffersizeString = null;
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        assert audioManager != null;
        samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        if (samplerateString == null) samplerateString = "44100";
        if (buffersizeString == null) buffersizeString = "512";

        HearingAidAudioProcessor(Integer.parseInt(samplerateString), Integer.parseInt(buffersizeString));

        superpoweredInitialized = true;
    }

    private native void HearingAidAudioProcessor(int samplerate, int buffersize);

    private native void onPlayPause(boolean play);

    public void setStreaming(boolean streaming) {
        isStreaming = streaming;
    }

    private void initPlayButtonMarginHandler() {
        final NestedScrollView nestedScrollView = findViewById(R.id.streming_nestedScrollView);
        final ConstraintLayout viewport = findViewById(R.id.fragment_content);
        final PlayPauseView mainToggleButton = findViewById(R.id.mainToggleButton);
        nestedScrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View view, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                float maxMargin = (0.6f * viewport.getHeight()) / 2;
                float scrollPercentage = ((float) scrollY) / viewport.getHeight();
                // float invertedScrollPercentage = Math.max((-1 / 0.1f) * scrollPercentage + 1.0f, 0.0f);
                float invertedScrollPercentage = Math.max(-1 * scrollPercentage + 1.0f, 0.0f);
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mainToggleButton.getLayoutParams();
                int finalMargin = Math.round(invertedScrollPercentage * maxMargin);
                System.out.println(scrollPercentage + ", " + invertedScrollPercentage);
                layoutParams.setMargins(0, finalMargin, 0, finalMargin);
                mainToggleButton.setLayoutParams(layoutParams);
            }
        });
    }

    private void initRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        // recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        countries = new ArrayList<>();
        countries.add("Australia");
        countries.add("India");
        countries.add("United States of America");
        countries.add("Germany");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("End");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("Russia");
        countries.add("Russia");
        final RecyclerView.Adapter adapter = new CardDataAdapter(countries);
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition(); //get position which is swipe

                if (direction == ItemTouchHelper.LEFT) {    //if swipe left

                    /*AlertDialog.Builder builder = new AlertDialog.Builder(getContext()); //alert for confirm to delete
                    builder.setMessage("Are you sure to delete?");    //set message

                    builder.setPositiveButton("REMOVE", new DialogInterface.OnClickListener() { //when click on DELETE
                        @Override
                        public void onClick(DialogInterface dialog, int which) {*/
                    adapter.notifyItemRemoved(position);    //item removed from recylcerview
                    // sqldatabase.execSQL("delete from " + TABLE_NAME + " where _id='" + (position + 1) + "'"); //query for delete
                    countries.remove(position);  //then remove item

                    return;
                       /* }
                    }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {  //not removing items if cancel is done
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            adapter.notifyItemRemoved(position + 1);    //notifies the RecyclerView Adapter that data in adapter has been removed at a particular position.
                            adapter.notifyItemRangeChanged(position, adapter.getItemCount());   //notifies the RecyclerView Adapter that positions of element in adapter has been changed from position(removed element index to end of list), please update it.
                            return;
                        }
                    }).show(); */ //show alert dialog
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView); //set swipe to recylcerview

        /*recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            GestureDetector gestureDetector = new GestureDetector(getActivity().getApplicationContext(), new GestureDetector.SimpleOnGestureListener() {

                @Override public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

            });
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {

                View child = rv.findChildViewUnder(e.getX(), e.getY());
                if(child != null && gestureDetector.onTouchEvent(e)) {
                    int position = rv.getChildAdapterPosition(child);
                    Toast.makeText(getActivity().getApplicationContext(), countries.get(position), Toast.LENGTH_SHORT).show();
                }

                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });*/
    }
}
