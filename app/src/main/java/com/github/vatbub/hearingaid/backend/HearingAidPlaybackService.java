package com.github.vatbub.hearingaid.backend;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.ResultReceiver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.AudioAttributesCompat;
import android.support.v4.media.MediaBrowserCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.github.vatbub.hearingaid.BugsnagWrapper;
import com.github.vatbub.hearingaid.Constants;
import com.github.vatbub.hearingaid.MainActivity;
import com.github.vatbub.hearingaid.R;

import java.util.List;

import static com.github.vatbub.hearingaid.Constants.ACTION_PAUSE;
import static com.github.vatbub.hearingaid.Constants.ACTION_PLAY;
import static com.github.vatbub.hearingaid.Constants.EMPTY_MEDIA_ROOT_ID;
import static com.github.vatbub.hearingaid.Constants.LOG_TAG;

public class HearingAidPlaybackService extends MediaBrowserServiceCompat {
    static {
        System.loadLibrary("HearingAidAudioProcessor");
    }

    private final int notificationId = 6392634;
    private MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mStateBuilder;
    private AudioFocusLossInformation audioFocusLossInformation;
    private boolean playing;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create a MediaSessionCompat
        mMediaSession = new MediaSessionCompat(this, LOG_TAG);

        // Enable callbacks from MediaButtons and TransportControls
        mMediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        mStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_STOP);
        setPlaying(false, true);

        mMediaSession.setCallback(new HearingAidMediaSessionCallback());

        // Set the session's token so that client activities can communicate with it.
        setSessionToken(mMediaSession.getSessionToken());

        // initialize Superpowered
        // Get the device's sample rate and buffer size to enable low-latency Android audio io, if available.
        String samplerateString, buffersizeString;
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        assert audioManager != null;
        samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        if (samplerateString == null) samplerateString = "44100";
        if (buffersizeString == null) buffersizeString = "512";

        HearingAidAudioProcessor(Integer.parseInt(samplerateString), Integer.parseInt(buffersizeString));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mMediaSession, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot(EMPTY_MEDIA_ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        //  Browsing not allowed
        result.sendResult(null);
    }

    private native void HearingAidAudioProcessor(int samplerate, int buffersize);

    private native void onPlayPause(boolean play);

    private native void onBackground();

    private native void onForeground();

    private native void eqEnabled(boolean eqEnabled);

    private Notification createPlayerNotification(boolean isPlaying) {
        String channelId = "hearingAidPlayPauseNotificationChannel";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager == null) {
                BugsnagWrapper.leaveBreadcrumb("notificationManager is null, not creating the notification channel...");
            } else {
                String channelName = getString(R.string.fragment_streaming_playpause_notification_channel_name);
                String channelDescription = getString(R.string.fragment_streaming_playpause_notification_channel_description);
                int importance = NotificationManager.IMPORTANCE_LOW;

                NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
                notificationChannel.setDescription(channelDescription);
                notificationChannel.enableVibration(false);
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(getString(R.string.app_name));

        if (isPlaying)
            notificationBuilder.setContentText(getString(R.string.fragment_streaming_playpause_notification_content_running));
        else
            notificationBuilder.setContentText(getString(R.string.fragment_streaming_playpause_notification_content_not_running));

        Intent contentIntent = new Intent(this, MainActivity.class);
        contentIntent.putExtra(Constants.INTENT_COMING_FROM_NOTIFICATION_EXTRA_KEY, true);
        PendingIntent pendingContextIntent = PendingIntent.getActivity(this, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingContextIntent)
                // .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP))
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary));

        if (isPlaying) {
            notificationBuilder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_media_pause_light,
                    getString(R.string.fragment_streaming_playpause_notification_pause_action_name),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE)));
        } else {
            notificationBuilder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_media_play_light,
                    getString(R.string.fragment_streaming_playpause_notification_play_action_name),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY)));
        }
        // Take advantage of MediaStyle features
        notificationBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mMediaSession.getSessionToken())
                .setShowActionsInCompactView(0)

                // Add a cancel button
                .setShowCancelButton(true)
                .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_STOP)));

        return notificationBuilder.build();
    }

    public AudioFocusLossInformation getAudioFocusLossInformation() {
        return audioFocusLossInformation;
    }

    private void setAudioFocusLossInformation(AudioFocusLossInformation audioFocusLossInformation) {
        this.audioFocusLossInformation = audioFocusLossInformation;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        setPlaying(playing, false);
    }

    private void setPlaying(boolean playing, boolean omitNativeCall) {
        this.playing = playing;

        if (playing)
            mStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1);
        else
            mStateBuilder.setState(PlaybackStateCompat.STATE_STOPPED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);

        mMediaSession.setPlaybackState(mStateBuilder.build());
        if (!omitNativeCall)
            onPlayPause(playing);
    }

    public static class AudioFocusLossInformation {
        private int audioFocusLossType;
        private boolean playedBeforeLoss;

        public AudioFocusLossInformation(int audioFocusLossType, boolean wasPlayingBeforeLoss) {
            setAudioFocusLossType(audioFocusLossType);
            setWasPlayingBeforeLoss(wasPlayingBeforeLoss);
        }

        public int getAudioFocusLossType() {
            return audioFocusLossType;
        }

        public void setAudioFocusLossType(int audioFocusLossType) {
            this.audioFocusLossType = audioFocusLossType;
        }

        public boolean wasPlayingBeforeLoss() {
            return playedBeforeLoss;
        }

        public void setWasPlayingBeforeLoss(boolean playedBeforeLoss) {
            this.playedBeforeLoss = playedBeforeLoss;
        }
    }

    private class HearingAidMediaSessionCallback extends MediaSessionCompat.Callback {
        private AudioFocusRequest audioFocusRequest;
        private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onPause();
            }
        };
        private BroadcastReceiver actionPauseReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onPause();
            }
        };
        private BroadcastReceiver actionPlayReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onPlay();
            }
        };
        private AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = focusChange -> {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    setAudioFocusLossInformation(new AudioFocusLossInformation(focusChange, isPlaying()));
                    onStop();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    setAudioFocusLossInformation(new AudioFocusLossInformation(focusChange, isPlaying()));
                    if (isPlaying())
                        onPause();
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (getAudioFocusLossInformation() == null || getAudioFocusLossInformation().wasPlayingBeforeLoss())
                        onPlay();
            }
        };

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            super.onCommand(command, extras, cb);

            if (Constants.CUSTOM_COMMAND_NOTIFY_EQ_ENABLED_CHANGED.equalsIgnoreCase(command) && extras != null && extras.containsKey(Constants.EQ_CHANGED_RESULT))
                eqEnabled(extras.getBoolean(Constants.EQ_CHANGED_RESULT));
        }

        @Override
        public void onPlay() {
            super.onPlay();
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) {
                BugsnagWrapper.leaveBreadcrumb("AudioManager was null, not executing MediaSession.onPlay()");
                return;
            }
            // Request audio focus for playback, this registers the afChangeListener
            int result;
            final int streamType = AudioManager.STREAM_MUSIC;
            final int focusGain = AudioManager.AUDIOFOCUS_GAIN;
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setFlags(AudioAttributesCompat.FLAG_AUDIBILITY_ENFORCED)
                    .setLegacyStreamType(streamType)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .build();

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                audioFocusRequest = new AudioFocusRequest.Builder(focusGain)
                        .setAudioAttributes(audioAttributes)
                        .setWillPauseWhenDucked(true)
                        .setOnAudioFocusChangeListener(onAudioFocusChangeListener)
                        .build();

                result = audioManager.requestAudioFocus(audioFocusRequest);
            } else {
                // ignore deprecation as this branch is only executed on SDK levels below 26
                //noinspection deprecation
                result = audioManager.requestAudioFocus(onAudioFocusChangeListener,
                        // Use the music stream.
                        streamType,
                        // Request permanent focus.
                        focusGain);
            }

            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return;

            startService(new Intent(HearingAidPlaybackService.this, HearingAidPlaybackService.class));

            mMediaSession.setActive(true);
            setPlaying(true);

            // Superpowered
            onForeground();

            IntentFilter becomingNoisyIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            IntentFilter pauseIntentFilter = new IntentFilter(ACTION_PAUSE);
            IntentFilter playIntentFilter = new IntentFilter(ACTION_PLAY);

            registerReceiver(becomingNoisyReceiver, becomingNoisyIntentFilter);
            registerReceiver(actionPauseReceiver, pauseIntentFilter);
            registerReceiver(actionPlayReceiver, playIntentFilter);

            startForeground(notificationId, createPlayerNotification(true));
        }

        @Override
        public void onPause() {
            super.onPause();

            setPlaying(false);

            try {
                unregisterReceiver(becomingNoisyReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                BugsnagWrapper.notify(e);
            }

            stopForeground(false);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null)
                notificationManager.notify(notificationId, createPlayerNotification(false));
            else
                BugsnagWrapper.leaveBreadcrumb("notificationManager was null, not updating the notification in onPause()");
        }

        @Override
        public void onStop() {
            super.onStop();
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) {
                BugsnagWrapper.leaveBreadcrumb("AudioManager was null, not executing MediaSession.onStop()");
                return;
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                if (audioFocusRequest != null)
                    audioManager.abandonAudioFocusRequest(audioFocusRequest);
                else
                    // ignore deprecation as this branch is only executed on SDK levels below 26
                    //noinspection deprecation
                    audioManager.abandonAudioFocus(onAudioFocusChangeListener);

            try {
                unregisterReceiver(becomingNoisyReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                BugsnagWrapper.notify(e);
            }

            try {
                unregisterReceiver(actionPauseReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                BugsnagWrapper.notify(e);
            }

            try {
                unregisterReceiver(actionPlayReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                BugsnagWrapper.notify(e);
            }

            stopSelf();
            mMediaSession.setActive(false);

            setPlaying(false);
            onBackground();

            stopForeground(true);
        }
    }
}
