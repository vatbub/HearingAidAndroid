package com.github.vatbub.hearingaid.backend;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.*;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.AudioAttributesCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import com.crashlytics.android.Crashlytics;
import com.github.vatbub.hearingaid.Constants;
import com.github.vatbub.hearingaid.R;

import java.util.List;

import static com.github.vatbub.hearingaid.Constants.*;

public class HearingAidPlaybackService extends MediaBrowserServiceCompat {
    static {
        System.loadLibrary("HearingAidAudioProcessor");
    }

    private final int notificationId = 6392634;
    private MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mStateBuilder;

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
        updatePlayerState(false);

        // MySessionCallback() has methods that handle callbacks from a media controller
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

    private void updatePlayerState(boolean isPlaying) {
        if (isPlaying)
            mStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1);
        else
            mStateBuilder.setState(PlaybackStateCompat.STATE_STOPPED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);

        mMediaSession.setPlaybackState(mStateBuilder.build());
    }

    /**
     * Called to get the root information for browsing by a particular client.
     * <p>
     * The implementation should verify that the client package has permission
     * to access browse media information before returning the root id; it
     * should return null if the client is not allowed to access this
     * information.
     * </p>
     *
     * @param clientPackageName The package name of the application which is
     *                          requesting access to browse media.
     * @param clientUid         The uid of the application which is requesting access to
     *                          browse media.
     * @param rootHints         An optional bundle of service-specific arguments to send
     *                          to the media browse service when connecting and retrieving the
     *                          root id for browsing, or null if none. The contents of this
     *                          bundle may affect the information returned when browsing.
     * @return The {@link BrowserRoot} for accessing this app's content or null.
     * @see BrowserRoot#EXTRA_RECENT
     * @see BrowserRoot#EXTRA_OFFLINE
     * @see BrowserRoot#EXTRA_SUGGESTED
     */
    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot(EMPTY_MEDIA_ROOT_ID, null);
    }

    /**
     * Called to get information about the children of a media item.
     * <p>
     * Implementations must call {@link Result#sendResult result.sendResult}
     * with the list of children. If loading the children will be an expensive
     * operation that should be performed on another thread,
     * {@link Result#detach result.detach} may be called before returning from
     * this function, and then {@link Result#sendResult result.sendResult}
     * called when the loading is complete.
     * </p><p>
     * In case the media item does not have any children, call {@link Result#sendResult}
     * with an empty list. When the given {@code parentId} is invalid, implementations must
     * call {@link Result#sendResult result.sendResult} with {@code null}, which will invoke
     * {@link MediaBrowserCompat.SubscriptionCallback#onError}.
     * </p>
     *
     * @param parentId The id of the parent media item whose children are to be
     *                 queried.
     * @param result   The Result to send the list of children to.
     */
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

            String channelName = getString(R.string.fragment_streaming_playpause_notification_channel_name);
            String channelDescription = getString(R.string.fragment_streaming_playpause_notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
            notificationChannel.setDescription(channelDescription);
            notificationChannel.enableVibration(false);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(getString(R.string.app_name));

        if (isPlaying)
            notificationBuilder.setContentText(getString(R.string.fragment_streaming_playpause_notification_content_running));
        else
            notificationBuilder.setContentText(getString(R.string.fragment_streaming_playpause_notification_content_not_running));

        notificationBuilder// .setContentIntent(mMediaSession.getController().getSessionActivity())
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP))
                .setColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));

        PendingIntent pendingIntent = retrievePlaybackAction(isPlaying);
        if (isPlaying) {
            notificationBuilder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_media_pause_light,
                    getString(R.string.fragment_streaming_playpause_notification_pause_action_name),
                    pendingIntent));
        } else {
            notificationBuilder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_media_play_light,
                    getString(R.string.fragment_streaming_playpause_notification_play_action_name),
                    pendingIntent));
        }
        // Take advantage of MediaStyle features
        notificationBuilder.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mMediaSession.getSessionToken())
                .setShowActionsInCompactView(0)

                // Add a cancel button
                .setShowCancelButton(true)
                .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_STOP)));

        return notificationBuilder.build();
    }

    private PendingIntent retrievePlaybackAction(boolean isPlaying) {
        Intent action;
        PendingIntent pendingIntent;
        final ComponentName serviceName = new ComponentName(this, HearingAidPlaybackService.class);
        // Play and pause
        if (isPlaying)
            action = new Intent(ACTION_PAUSE);
        else
            action = new Intent(ACTION_PLAY);
        action.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(this, 1, action, 0);
        return pendingIntent;

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
        private AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_LOSS:
                        onStop();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        onPause();
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN:
                        onPlay();
                }
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
                Crashlytics.log(Log.WARN, LOG_TAG, "AudioManager was null, not executing MediaSession.onPlay()");
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
                result = audioManager.requestAudioFocus(onAudioFocusChangeListener,
                        // Use the music stream.
                        streamType,
                        // Request permanent focus.
                        focusGain);
            }

            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return;

            startService(new Intent(HearingAidPlaybackService.this, HearingAidPlaybackService.class));

            mMediaSession.setActive(true);
            updatePlayerState(true);

            // Superpowered
            onPlayPause(true);
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

            onPlayPause(false);
            updatePlayerState(false);

            // unregister BECOME_NOISY BroadcastReceiver
            unregisterReceiver(becomingNoisyReceiver);
            // Take the service out of the foreground, retain the notification
            stopForeground(false);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null)
                notificationManager.notify(notificationId, createPlayerNotification(false));
        }

        @Override
        public void onStop() {
            super.onStop();
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) {
                Crashlytics.log(Log.WARN, LOG_TAG, "AudioManager was null, not executing MediaSession.onStop()");
                return;
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            else
                audioManager.abandonAudioFocus(onAudioFocusChangeListener);

            try {
                unregisterReceiver(becomingNoisyReceiver);
                unregisterReceiver(actionPauseReceiver);
                unregisterReceiver(actionPlayReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                Crashlytics.logException(e);
            }

            stopSelf();
            mMediaSession.setActive(false);

            onPlayPause(false);
            onBackground();
            updatePlayerState(false);

            stopForeground(true);
        }
    }
}
