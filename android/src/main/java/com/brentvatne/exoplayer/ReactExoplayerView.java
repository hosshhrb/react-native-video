package com.brentvatne.exoplayer;

import static androidx.media3.common.C.CONTENT_TYPE_DASH;
import static androidx.media3.common.C.CONTENT_TYPE_HLS;
import static androidx.media3.common.C.CONTENT_TYPE_OTHER;
import static androidx.media3.common.C.CONTENT_TYPE_RTSP;
import static androidx.media3.common.C.CONTENT_TYPE_SS;
import static androidx.media3.common.C.TIME_END_OF_SOURCE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.CaptioningManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.StreamKey;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.dash.DashUtil;
import androidx.media3.exoplayer.dash.DefaultDashChunkSource;
import androidx.media3.exoplayer.dash.manifest.AdaptationSet;
import androidx.media3.exoplayer.dash.manifest.DashManifest;
import androidx.media3.exoplayer.dash.manifest.Period;
import androidx.media3.exoplayer.dash.manifest.Representation;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider;
import androidx.media3.exoplayer.drm.DrmSessionEventListener;
import androidx.media3.exoplayer.drm.DrmSessionManager;
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider;
import androidx.media3.exoplayer.drm.FrameworkMediaDrm;
import androidx.media3.exoplayer.drm.HttpMediaDrmCallback;
import androidx.media3.exoplayer.drm.UnsupportedDrmException;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.ima.ImaAdsLoader;
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo;
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.exoplayer.smoothstreaming.DefaultSsChunkSource;
import androidx.media3.exoplayer.smoothstreaming.SsMediaSource;
import androidx.media3.exoplayer.source.ClippingMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.MergingMediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.source.ads.AdsMediaSource;
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelection;
import androidx.media3.exoplayer.trackselection.TrackSelectionArray;
import androidx.media3.exoplayer.upstream.BandwidthMeter;
import androidx.media3.exoplayer.upstream.CmcdConfiguration;
import androidx.media3.exoplayer.upstream.DefaultAllocator;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;
import androidx.media3.exoplayer.util.EventLogger;
import androidx.media3.extractor.metadata.emsg.EventMessage;
import androidx.media3.extractor.metadata.id3.Id3Frame;
import androidx.media3.extractor.metadata.id3.TextInformationFrame;
import androidx.media3.session.MediaSessionService;
import androidx.media3.ui.LegacyPlayerControlView;

import com.brentvatne.common.api.AdsProps;
import com.brentvatne.common.api.BufferConfig;
import com.brentvatne.common.api.BufferingStrategy;
import com.brentvatne.common.api.ControlsConfig;
import com.brentvatne.common.api.DRMProps;
import com.brentvatne.common.api.ResizeMode;
import com.brentvatne.common.api.SideLoadedTextTrack;
import com.brentvatne.common.api.Source;
import com.brentvatne.common.api.SubtitleStyle;
import com.brentvatne.common.api.TimedMetadata;
import com.brentvatne.common.api.Track;
import com.brentvatne.common.api.VideoTrack;
import com.brentvatne.common.react.VideoEventEmitter;
import com.brentvatne.common.toolbox.DebugLog;
import com.brentvatne.common.toolbox.ReactBridgeUtils;
import com.brentvatne.react.BuildConfig;
import com.brentvatne.react.R;
import com.brentvatne.react.ReactNativeVideoManager;
import com.brentvatne.receiver.AudioBecomingNoisyReceiver;
import com.brentvatne.receiver.BecomingNoisyListener;
import com.brentvatne.receiver.PictureInPictureReceiver;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.uimanager.ThemedReactContext;
import com.google.ads.interactivemedia.v3.api.AdError;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.google.common.collect.ImmutableList;
import com.brentvatne.exoplayer.AdAnalyticsLogger;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@SuppressLint("ViewConstructor")
public class ReactExoplayerView extends FrameLayout implements
        LifecycleEventListener,
        Player.Listener,
        BandwidthMeter.EventListener,
        BecomingNoisyListener,
        DrmSessionEventListener,
        AdEvent.AdEventListener,
        AdErrorEvent.AdErrorListener {

    public static final double DEFAULT_MAX_HEAP_ALLOCATION_PERCENT = 1;
    public static final double DEFAULT_MIN_BUFFER_MEMORY_RESERVE = 0;

    private static final String TAG = "ReactExoplayerView";

    private static final CookieManager DEFAULT_COOKIE_MANAGER;
    private static final int SHOW_PROGRESS = 1;

    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    protected final VideoEventEmitter eventEmitter;
    private final ReactExoplayerConfig config;
    private final DefaultBandwidthMeter bandwidthMeter;
    private LegacyPlayerControlView playerControlView;
    private View playPauseControlContainer;
    private Player.Listener eventListener;

    private ExoPlayerView exoPlayerView;
    private FullScreenPlayerView fullScreenPlayerView;
    private ImaAdsLoader adsLoader;

    private DataSource.Factory mediaDataSourceFactory;
    private ExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private boolean playerNeedsSource;
    private ServiceConnection playbackServiceConnection;
    private PlaybackServiceBinder playbackServiceBinder;

    // logger to be enable by props
    private EventLogger debugEventLogger = null;
    private boolean enableDebug = false;
    private static final String TAG_EVENT_LOGGER = "RNVExoplayer";

    private int resumeWindow;
    private long resumePosition;
    private boolean loadVideoStarted;
    private boolean isFullscreen;
    private boolean isInBackground;
    private boolean isPaused;
    private boolean isBuffering;
    private boolean muted = false;
    public boolean enterPictureInPictureOnLeave = false;
    private PictureInPictureParams.Builder pictureInPictureParamsBuilder;
    private boolean hasAudioFocus = false;
    private float rate = 1f;
    private AudioOutput audioOutput = AudioOutput.SPEAKER;
    private float audioVolume = 1f;
    private int maxBitRate = 0;
    private boolean hasDrmFailed = false;
    private boolean isUsingContentResolution = false;
    private boolean selectTrackWhenReady = false;
    private final Handler mainHandler;
    private Runnable mainRunnable;
    private Runnable pipListenerUnsubscribe;
    private boolean useCache = false;
    private ControlsConfig controlsConfig = new ControlsConfig();
    private ArrayList<Integer> rootViewChildrenOriginalVisibility = new ArrayList<Integer>();

    /*
    * When user is seeking first called is on onPositionDiscontinuity -> DISCONTINUITY_REASON_SEEK
    * Then we set if to false when playback is back in onIsPlayingChanged -> true
    */
    private boolean isSeeking = false;
    private long seekPosition = -1;

    // Props from React
    private Source source = new Source();
    private boolean repeat;
    private String audioTrackType;
    private String audioTrackValue;
    private String videoTrackType;
    private String videoTrackValue;
    private String textTrackType = "disabled";
    private String textTrackValue;
    private boolean disableFocus;
    private boolean focusable = true;
    private BufferingStrategy.BufferingStrategyEnum bufferingStrategy;
    private boolean disableDisconnectError;
    private boolean preventsDisplaySleepDuringVideoPlayback = true;
    private float mProgressUpdateInterval = 250.0f;
    protected boolean playInBackground = false;
    private boolean mReportBandwidth = false;
    private boolean controls;

    private boolean showNotificationControls = false;
    // \ End props

    // React
    private final ThemedReactContext themedReactContext;
    private final AudioManager audioManager;
    private final AudioBecomingNoisyReceiver audioBecomingNoisyReceiver;
    private final PictureInPictureReceiver pictureInPictureReceiver;
    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;

    // store last progress event values to avoid sending unnecessary messages
    private long lastPos = -1;
    private long lastBufferDuration = -1;
    private long lastDuration = -1;

    private boolean viewHasDropped = false;
    private int selectedSpeedIndex = 1; // Default is 1.0x

    private final String instanceId = String.valueOf(UUID.randomUUID());

    private CmcdConfiguration.Factory cmcdConfigurationFactory;

    private LocalAdMediaSourceFactory localAdMediaSourceFactory;

    public void setCmcdConfigurationFactory(CmcdConfiguration.Factory factory) {
        this.cmcdConfigurationFactory = factory;
    }

    private void updateProgress() {
        if (player != null) {
            if (playerControlView != null && isPlayingAd() && controls) {
                playerControlView.hide();
            }
            long bufferedDuration = player.getBufferedPercentage() * player.getDuration() / 100;
            long duration = player.getDuration();
            long pos = player.getCurrentPosition();
            if (pos > duration) {
                pos = duration;
            }

            if (lastPos != pos
                    || lastBufferDuration != bufferedDuration
                    || lastDuration != duration) {
                lastPos = pos;
                lastBufferDuration = bufferedDuration;
                lastDuration = duration;
                eventEmitter.onVideoProgress.invoke(pos, bufferedDuration, player.getDuration(), getPositionInFirstPeriodMsForCurrentWindow(pos));
            }
        }
    }

    private final Handler progressHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == SHOW_PROGRESS) {
                updateProgress();
                msg = obtainMessage(SHOW_PROGRESS);
                sendMessageDelayed(msg, Math.round(mProgressUpdateInterval));
            }
        }
    };

    public double getPositionInFirstPeriodMsForCurrentWindow(long currentPosition) {
        Timeline.Window window = new Timeline.Window();
        if(!player.getCurrentTimeline().isEmpty()) {
            player.getCurrentTimeline().getWindow(player.getCurrentMediaItemIndex(), window);
        }
        return window.windowStartTimeMs + currentPosition;
    }

    public ReactExoplayerView(ThemedReactContext context, ReactExoplayerConfig config) {
        super(context);
        this.themedReactContext = context;
        this.eventEmitter = new VideoEventEmitter();
        this.config = config;
        this.bandwidthMeter = config.getBandwidthMeter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pictureInPictureParamsBuilder == null) {
            this.pictureInPictureParamsBuilder = new PictureInPictureParams.Builder();
        }
        mainHandler = new Handler();

        createViews();

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        themedReactContext.addLifecycleEventListener(this);
        pipListenerUnsubscribe = PictureInPictureUtil.addLifecycleEventListener(context, this);
        audioBecomingNoisyReceiver = new AudioBecomingNoisyReceiver(themedReactContext);
        audioFocusChangeListener = new OnAudioFocusChangedListener(this, themedReactContext);
        pictureInPictureReceiver = new PictureInPictureReceiver(this, themedReactContext);
    }

    private boolean isPlayingAd() {
        return player != null && player.isPlayingAd();
    }

    private void createViews() {
        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }

        LayoutParams layoutParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        exoPlayerView = new ExoPlayerView(getContext());
        exoPlayerView.addOnLayoutChangeListener( (View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) ->
                PictureInPictureUtil.applySourceRectHint(themedReactContext, pictureInPictureParamsBuilder, exoPlayerView)
        );
        exoPlayerView.setLayoutParams(layoutParams);
        addView(exoPlayerView, 0, layoutParams);

        exoPlayerView.setFocusable(this.focusable);
    }

    @Override
    protected void onDetachedFromWindow() {
        cleanupPlaybackService();
        super.onDetachedFromWindow();
    }

    // LifecycleEventListener implementation
    @Override
    public void onHostResume() {
        if (!playInBackground || !isInBackground) {
            setPlayWhenReady(!isPaused);
        }
        isInBackground = false;
    }

    @Override
    public void onHostPause() {
        isInBackground = true;
        Activity activity = themedReactContext.getCurrentActivity();
        boolean isInPictureInPicture = Util.SDK_INT >= Build.VERSION_CODES.N && activity != null && activity.isInPictureInPictureMode();
        boolean isInMultiWindowMode = Util.SDK_INT >= Build.VERSION_CODES.N && activity != null && activity.isInMultiWindowMode();
        if (playInBackground || isInPictureInPicture || isInMultiWindowMode) {
            return;
        }
        setPlayWhenReady(false);
    }

    @Override
    public void onHostDestroy() {
        cleanUpResources();
    }

    public void cleanUpResources() {
        stopPlayback();
        themedReactContext.removeLifecycleEventListener(this);
        releasePlayer();
        viewHasDropped = true;
    }

    //BandwidthMeter.EventListener implementation
    @Override
    public void onBandwidthSample(int elapsedMs, long bytes, long bitrate) {
        if (mReportBandwidth) {
            if (player == null) {
                eventEmitter.onVideoBandwidthUpdate.invoke(bitrate, 0, 0, null);
            } else {
                Format videoFormat = player.getVideoFormat();
                boolean isRotatedContent = videoFormat != null && (videoFormat.rotationDegrees == 90 || videoFormat.rotationDegrees == 270);
                int width = videoFormat != null ? (isRotatedContent ? videoFormat.height : videoFormat.width) : 0;
                int height = videoFormat != null ? (isRotatedContent ? videoFormat.width : videoFormat.height) : 0;
                String trackId = videoFormat != null ? videoFormat.id : null;
                eventEmitter.onVideoBandwidthUpdate.invoke(bitrate, height, width, trackId);
            }
        }
    }

    // Internal methods

    /**
     * Toggling the visibility of the player control view
     */
    private void togglePlayerControlVisibility() {
        if (player == null) return;
        reLayoutControls();
        if (playerControlView.isVisible()) {
            playerControlView.hide();
        } else {
            playerControlView.show();
        }
    }

    /**
     * Initializing Player control
     */
    private void initializePlayerControl() {
        if (playerControlView == null) {
            playerControlView = new LegacyPlayerControlView(getContext());
            playerControlView.addVisibilityListener(new LegacyPlayerControlView.VisibilityListener() {
                @Override
                public void onVisibilityChange(int visibility) {
                    eventEmitter.onControlsVisibilityChange.invoke(visibility == View.VISIBLE);
                }
            });
        }

        // Setting the player for the playerControlView
        playerControlView.setPlayer(player);
        playPauseControlContainer = playerControlView.findViewById(R.id.exo_play_pause_container);

        // Invoking onClick event for exoplayerView
        exoPlayerView.setOnClickListener((View v) -> {
            if (!isPlayingAd()) {
                togglePlayerControlVisibility();
            }
        });

        //Handling the playButton click event
        ImageButton playButton = playerControlView.findViewById(R.id.exo_play);

        playButton.setOnClickListener((View v) -> {
            if (player != null && player.getPlaybackState() == Player.STATE_ENDED) {
                player.seekTo(0);
            }
            setPausedModifier(false);
        });

        //Handling the rewind and forward button click events
        ImageButton exoRewind = playerControlView.findViewById(R.id.exo_rew);
        ImageButton exoForward = playerControlView.findViewById(R.id.exo_ffwd);
        exoRewind.setOnClickListener((View v) -> {
            seekTo(player.getCurrentPosition() - controlsConfig.getSeekIncrementMS());
        });

        exoForward.setOnClickListener((View v) -> {
            seekTo(player.getCurrentPosition() + controlsConfig.getSeekIncrementMS());
        });

        //Handling the pauseButton click event
        ImageButton pauseButton = playerControlView.findViewById(R.id.exo_pause);
        pauseButton.setOnClickListener((View v) ->
                setPausedModifier(true)
        );

        //Handling the settingButton click event
        final ImageButton settingButton = playerControlView.findViewById(R.id.exo_settings);
        settingButton.setOnClickListener(v -> openSettings());

        //Handling the fullScreenButton click event
        final ImageButton fullScreenButton = playerControlView.findViewById(R.id.exo_fullscreen);
        fullScreenButton.setOnClickListener(v -> setFullscreen(!isFullscreen));
        updateFullScreenButtonVisibility();
        refreshControlsStyles();

        // Invoking onPlaybackStateChanged and onPlayWhenReadyChanged events for Player
        eventListener = new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                View playButton = playerControlView.findViewById(R.id.exo_play);
                View pauseButton = playerControlView.findViewById(R.id.exo_pause);
                if (playButton != null && playButton.getVisibility() == GONE) {
                    playButton.setVisibility(INVISIBLE);
                }
                if (pauseButton != null && pauseButton.getVisibility() == GONE) {
                    pauseButton.setVisibility(INVISIBLE);
                }

                reLayout(playPauseControlContainer);
                //Remove this eventListener once its executed. since UI will work fine once after the reLayout is done
                player.removeListener(eventListener);
            }

            @Override
            public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                reLayout(playPauseControlContainer);
                //Remove this eventListener once its executed. since UI will work fine once after the reLayout is done
                player.removeListener(eventListener);
            }
        };
        player.addListener(eventListener);
    }
    private void openSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(themedReactContext);
        builder.setTitle(R.string.settings);
        String[] settingsOptions = {themedReactContext.getString(R.string.playback_speed)};
        builder.setItems(settingsOptions, (dialog, which) -> {
            if (which == 0) {
                showPlaybackSpeedOptions();
            }
        });
        builder.show();
    }

    private void showPlaybackSpeedOptions() {
        String[] speedOptions = {"0.5x", "1.0x", "1.5x", "2.0x"};
        AlertDialog.Builder builder = new AlertDialog.Builder(themedReactContext);
        builder.setTitle(R.string.select_playback_speed);

        builder.setSingleChoiceItems(speedOptions, selectedSpeedIndex, (dialog, which) -> {
            selectedSpeedIndex = which;
            float speed = switch (which) {
                case 0 -> 0.5f;
                case 2 -> 1.5f;
                case 3 -> 2.0f;
                default -> 1.0f;
            };
            setRateModifier(speed);
        });
        builder.show();
    }

    /**
     * Adding Player control to the frame layout
     */
    private void addPlayerControl() {
        if (playerControlView == null) return;
        LayoutParams layoutParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        playerControlView.setLayoutParams(layoutParams);
        int indexOfPC = indexOfChild(playerControlView);
        if (indexOfPC != -1) {
            removeViewAt(indexOfPC);
        }
        addView(playerControlView, 1, layoutParams);
        reLayout(playerControlView);
    }

    /**
     * Update the layout
     * @param view  view needs to update layout
     *
     * This is a workaround for the open bug in react-native: <a href="https://github.com/facebook/react-native/issues/17968">...</a>
     */
    private void reLayout(View view) {
        if (view == null) return;
        view.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
        view.layout(view.getLeft(), view.getTop(), view.getMeasuredWidth(), view.getMeasuredHeight());
    }

    private void refreshControlsStyles() {
        if (playerControlView == null || player == null || !controls) return;
        updateLiveContent();
        updatePlayPauseButtons();
        updateButtonVisibility(controlsConfig.getHideForward(), R.id.exo_ffwd);
        updateButtonVisibility(controlsConfig.getHideRewind(), R.id.exo_rew);
        updateButtonVisibility(controlsConfig.getHideNext(), R.id.exo_next);
        updateButtonVisibility(controlsConfig.getHidePrevious(), R.id.exo_prev);
        updateViewVisibility(playerControlView.findViewById(R.id.exo_fullscreen), controlsConfig.getHideFullscreen(), GONE);
        updateViewVisibility(playerControlView.findViewById(R.id.exo_position), controlsConfig.getHidePosition(), GONE);
        updateViewVisibility(playerControlView.findViewById(R.id.exo_progress), controlsConfig.getHideSeekBar(), INVISIBLE);
        updateViewVisibility(playerControlView.findViewById(R.id.exo_duration), controlsConfig.getHideDuration(), GONE);
        updateViewVisibility(playerControlView.findViewById(R.id.exo_settings), controlsConfig.getHideSettingButton(), GONE );
    }

    private void updateLiveContent() {
        LinearLayout exoLiveContainer = playerControlView.findViewById(R.id.exo_live_container);
        TextView exoLiveLabel = playerControlView.findViewById(R.id.exo_live_label);

        boolean isLive = false;
        Timeline timeline = player.getCurrentTimeline();

        // Determine if the content is live
        if (!timeline.isEmpty()) {
            Timeline.Window window = new Timeline.Window();
            timeline.getWindow(player.getCurrentMediaItemIndex(), window);
            isLive = window.isLive();
        }

        if (isLive && controlsConfig.getLiveLabel() != null) {
            exoLiveLabel.setText(controlsConfig.getLiveLabel());
            exoLiveContainer.setVisibility(VISIBLE);
        } else {
            exoLiveContainer.setVisibility(GONE);
        }
    }

    private void updatePlayPauseButtons() {
        final ImageButton playButton = playerControlView.findViewById(R.id.exo_play);
        final ImageButton pauseButton = playerControlView.findViewById(R.id.exo_pause);

        if (controlsConfig.getHidePlayPause()) {
            playPauseControlContainer.setAlpha(0);
            playButton.setClickable(false);
            pauseButton.setClickable(false);
        } else {
            playPauseControlContainer.setAlpha(1.0f);
            playButton.setClickable(true);
            pauseButton.setClickable(true);
        }
    }

    private void updateButtonVisibility(boolean hide, int buttonID) {
        ImageButton button = playerControlView.findViewById(buttonID);
        if (hide) {
            button.setImageAlpha(0);
            button.setClickable(false);
        } else {
            button.setImageAlpha(255);
            button.setClickable(true);
        }
    }

    private void updateViewVisibility(View view, boolean hide, int hideVisibility) {
        if (hide) {
            view.setVisibility(hideVisibility);
        } else if (view.getVisibility() == hideVisibility) {
            view.setVisibility(VISIBLE);
        }
    }



    private void reLayoutControls() {
        reLayout(exoPlayerView);
        reLayout(playerControlView);
    }

    /// returns true is adaptive bitrate shall be used
    public boolean isUsingVideoABR() {
        return videoTrackType == null || "auto".equals(videoTrackType);
    }

    public void setDebug(boolean enableDebug) {
        this.enableDebug = enableDebug;
        refreshDebugState();
    }

    private void refreshDebugState() {
        if (player == null) {
            return;
        }
        if (enableDebug) {
            debugEventLogger = new EventLogger(TAG_EVENT_LOGGER);
            player.addAnalyticsListener(debugEventLogger);
        } else if (debugEventLogger != null) {
            player.removeAnalyticsListener(debugEventLogger);
            debugEventLogger = null;
        }
    }

    public void setViewType(int viewType) {
        exoPlayerView.updateSurfaceView(viewType);
    }

    private class RNVLoadControl extends DefaultLoadControl {
        private final int availableHeapInBytes;
        private final Runtime runtime;
        public RNVLoadControl(DefaultAllocator allocator, BufferConfig config) {
            super(allocator,
                    config.getMinBufferMs() != BufferConfig.Companion.getBufferConfigPropUnsetInt()
                            ? config.getMinBufferMs()
                            : DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                    config.getMaxBufferMs() != BufferConfig.Companion.getBufferConfigPropUnsetInt()
                            ? config.getMaxBufferMs()
                            : DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                    config.getBufferForPlaybackMs() != BufferConfig.Companion.getBufferConfigPropUnsetInt()
                            ? config.getBufferForPlaybackMs()
                            : DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS ,
                    config.getBufferForPlaybackAfterRebufferMs() != BufferConfig.Companion.getBufferConfigPropUnsetInt()
                            ? config.getBufferForPlaybackAfterRebufferMs()
                            : DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                    -1,
                    true,
                    config.getBackBufferDurationMs() != BufferConfig.Companion.getBufferConfigPropUnsetInt()
                            ? config.getBackBufferDurationMs()
                            : DefaultLoadControl.DEFAULT_BACK_BUFFER_DURATION_MS,
                    DefaultLoadControl.DEFAULT_RETAIN_BACK_BUFFER_FROM_KEYFRAME);
            runtime = Runtime.getRuntime();
            ActivityManager activityManager = (ActivityManager) themedReactContext.getSystemService(ThemedReactContext.ACTIVITY_SERVICE);
            double maxHeap = config.getMaxHeapAllocationPercent() != BufferConfig.Companion.getBufferConfigPropUnsetDouble()
                    ? config.getMaxHeapAllocationPercent()
                    : DEFAULT_MAX_HEAP_ALLOCATION_PERCENT;
            availableHeapInBytes = (int) Math.floor(activityManager.getMemoryClass() * maxHeap * 1024 * 1024);
        }

        @Override
        public boolean shouldContinueLoading(long playbackPositionUs, long bufferedDurationUs, float playbackSpeed) {
            if (bufferingStrategy == BufferingStrategy.BufferingStrategyEnum.DisableBuffering) {
                return false;
            } else if (bufferingStrategy == BufferingStrategy.BufferingStrategyEnum.DependingOnMemory) {
                // The goal of this algorithm is to pause video loading (increasing the buffer)
                // when available memory on device become low.
                int loadedBytes = getAllocator().getTotalBytesAllocated();
                boolean isHeapReached = availableHeapInBytes > 0 && loadedBytes >= availableHeapInBytes;
                if (isHeapReached) {
                    return false;
                }
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                long freeMemory = runtime.maxMemory() - usedMemory;
                double minBufferMemoryReservePercent = source.getBufferConfig().getMinBufferMemoryReservePercent() != BufferConfig.Companion.getBufferConfigPropUnsetDouble()
                        ? source.getBufferConfig().getMinBufferMemoryReservePercent()
                        : ReactExoplayerView.DEFAULT_MIN_BUFFER_MEMORY_RESERVE;
                long reserveMemory = (long) minBufferMemoryReservePercent * runtime.maxMemory();
                long bufferedMs = bufferedDurationUs / (long) 1000;
                if (reserveMemory > freeMemory && bufferedMs > 2000) {
                    // We don't have enough memory in reserve so we stop buffering to allow other components to use it instead
                    return false;
                }
                if (runtime.freeMemory() == 0) {
                    DebugLog.w(TAG, "Free memory reached 0, forcing garbage collection");
                    runtime.gc();
                    return false;
                }
            }
            // "default" case or normal case for "DependingOnMemory"
            return super.shouldContinueLoading(playbackPositionUs, bufferedDurationUs, playbackSpeed);
        }
    }

    private void initializePlayer() {
        ReactExoplayerView self = this;
        Activity activity = themedReactContext.getCurrentActivity();
        // This ensures all props have been settled, to avoid async racing conditions.
        Source runningSource = source;
        mainRunnable = () -> {
            if (viewHasDropped && runningSource == source) {
                return;
            }
            try {
                if (runningSource.getUri() == null) {
                    return;
                }

                if (player == null) {
                    // Initialize core configuration and listeners
                    initializePlayerCore(self);
                }
                if (!source.isLocalAssetFile() && !source.isAsset() && source.getBufferConfig().getCacheSize() > 0) {
                    RNVSimpleCache.INSTANCE.setSimpleCache(
                            this.getContext(),
                            source.getBufferConfig().getCacheSize()
                    );
                    useCache = true;
                } else {
                    useCache = false;
                }
                if (playerNeedsSource) {
                    // Will force display of shutter view if needed
                    exoPlayerView.updateShutterViewVisibility();
                    exoPlayerView.invalidateAspectRatio();
                    // DRM session manager creation must be done on a different thread to prevent crashes so we start a new thread
                    ExecutorService es = Executors.newSingleThreadExecutor();
                    es.execute(() -> {
                        // DRM initialization must run on a different thread
                        if (viewHasDropped && runningSource == source) {
                            return;
                        }
                        if (activity == null) {
                            DebugLog.e(TAG, "Failed to initialize Player!, null activity");
                            eventEmitter.onVideoError.invoke("Failed to initialize Player!", new Exception("Current Activity is null!"), "1001");
                            return;
                        }

                        // Initialize handler to run on the main thread
                        activity.runOnUiThread(() -> {
                            if (viewHasDropped && runningSource == source) {
                                return;
                            }
                            try {
                                // Source initialization must run on the main thread
                                initializePlayerSource(runningSource);
                            } catch (Exception ex) {
                                self.playerNeedsSource = true;
                                DebugLog.e(TAG, "Failed to initialize Player! 1");
                                DebugLog.e(TAG, ex.toString());
                                ex.printStackTrace();
                                eventEmitter.onVideoError.invoke(ex.toString(), ex, "1001");
                            }
                        });
                    });
                } else if (runningSource == source) {
                    initializePlayerSource(runningSource);
                }
            } catch (Exception ex) {
                self.playerNeedsSource = true;
                DebugLog.e(TAG, "Failed to initialize Player! 2");
                DebugLog.e(TAG, ex.toString());
                ex.printStackTrace();
                eventEmitter.onVideoError.invoke(ex.toString(), ex, "1001");
            }
        };
        mainHandler.postDelayed(mainRunnable, 1);
    }

    public void getCurrentPosition(Promise promise) {
        if (player != null) {
            float currentPosition = player.getCurrentPosition() / 1000.0f;
            promise.resolve(currentPosition);
        } else {
            promise.reject("PLAYER_NOT_AVAILABLE", "Player is not initialized.");
        }
    }

    private void initializePlayerCore(ReactExoplayerView self) {
        ExoTrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();
        self.trackSelector = new DefaultTrackSelector(getContext(), videoTrackSelectionFactory);
        self.trackSelector.setParameters(trackSelector.buildUponParameters()
                .setMaxVideoBitrate(maxBitRate == 0 ? Integer.MAX_VALUE : maxBitRate));

        DefaultAllocator allocator = new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE);
        RNVLoadControl loadControl = new RNVLoadControl(
                allocator,
                source.getBufferConfig()
        );
        DefaultRenderersFactory renderersFactory =
                new DefaultRenderersFactory(getContext())
                        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
                        .setEnableDecoderFallback(true)
                        .forceEnableMediaCodecAsynchronousQueueing();

        DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(mediaDataSourceFactory);
        if (useCache) {
            mediaSourceFactory.setDataSourceFactory(RNVSimpleCache.INSTANCE.getCacheFactory(buildHttpDataSourceFactory(true)));
        }

        mediaSourceFactory.setLocalAdInsertionComponents(unusedAdTagUri -> adsLoader, exoPlayerView);

        player = new ExoPlayer.Builder(getContext(), renderersFactory)
                .setTrackSelector(self.trackSelector)
                .setBandwidthMeter(bandwidthMeter)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(mediaSourceFactory)
                .build();
        ReactNativeVideoManager.Companion.getInstance().onInstanceCreated(instanceId, player);
        refreshDebugState();
        player.addListener(self);
        player.setVolume(muted ? 0.f : audioVolume * 1);
        exoPlayerView.setPlayer(player);

        audioBecomingNoisyReceiver.setListener(self);
        pictureInPictureReceiver.setListener();
        bandwidthMeter.addEventListener(new Handler(), self);
        setPlayWhenReady(!isPaused);
        playerNeedsSource = true;

        PlaybackParameters params = new PlaybackParameters(rate, 1f);
        player.setPlaybackParameters(params);
        changeAudioOutput(this.audioOutput);

        if(showNotificationControls) {
            setupPlaybackService();
        }
    }

    private AdsMediaSource initializeAds(MediaSource videoSource, Source runningSource) {
        AdsProps adProps = runningSource.getAdsProps();
        Uri uri = runningSource.getUri();
        if (adProps != null && uri != null) {
            Uri adTagUrl = adProps.getAdTagUrl();
            if (adTagUrl != null) {
                exoPlayerView.showAds();
                // Create an AdsLoader.
                ImaAdsLoader.Builder imaLoaderBuilder = new ImaAdsLoader
                        .Builder(themedReactContext)
                        .setAdEventListener(this)
                        .setAdErrorListener(this);

                if (adProps.getAdLanguage() != null) {
                    ImaSdkSettings imaSdkSettings = ImaSdkFactory.getInstance().createImaSdkSettings();
                    imaSdkSettings.setLanguage(adProps.getAdLanguage());
                    imaLoaderBuilder.setImaSdkSettings(imaSdkSettings);
                }
                adsLoader = imaLoaderBuilder.build();
                adsLoader.setPlayer(player);
                if (adsLoader != null) {
                    // Create a default media source factory with ad insertion
                    DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(mediaDataSourceFactory)
                            .setLocalAdInsertionComponents(unusedAdTagUri -> adsLoader, exoPlayerView);
                    
                    // Set up ad interceptor to replace ad content
                    setupAdReplacement();
                    
                    DataSpec adTagDataSpec = new DataSpec(adTagUrl);
                    return new AdsMediaSource(videoSource,
                            adTagDataSpec,
                            ImmutableList.of(uri, adTagUrl),
                            mediaSourceFactory, adsLoader, exoPlayerView);
                }
            }
        }
        exoPlayerView.hideAds();
        return null;
    }
    
    /**
     * Set up ad replacement by intercepting the ad loading
     * This approach uses event listeners instead of extending classes
     */
    private void setupAdReplacement() {
        // Make sure we have a valid player
        if (player == null) return;
        
        // Add a listener to detect when ads start playing
        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying && player.isPlayingAd()) {
                    Log.d(TAG, "Ad playback detected - replacing with local ad");
                    tryReplaceAdWithLocalVideo();
                }
            }
        });
    }
    
    /**
     * Try to replace the currently playing ad with a local video
     * This is called when an ad starts playing
     */
    private void tryReplaceAdWithLocalVideo() {
        try {
            // Save the current position and state
            boolean wasPlaying = player.getPlayWhenReady();
            
            // Prepare local ad media source
            Uri localAdUri = getLocalAdUri();
            Log.d(TAG, "Replacing ad with local video: " + localAdUri);
            
            // Create a media item for the local ad
            MediaItem localAdMediaItem = new MediaItem.Builder()
                    .setUri(localAdUri)
                    .build();
            
            // Create a media source for the local ad
            MediaSource localAdSource;
            if (localAdUri.toString().startsWith("asset:")) {
                // For assets, we need a custom asset data source factory
                localAdSource = new ProgressiveMediaSource.Factory(
                        new AssetDataSourceFactory(themedReactContext))
                        .createMediaSource(localAdMediaItem);
            } else {
                // For regular files
                localAdSource = new ProgressiveMediaSource.Factory(mediaDataSourceFactory)
                        .createMediaSource(localAdMediaItem);
            }

            // Get the current window index
            int adWindowIndex = player.getCurrentWindowIndex();
            
            // Load the local ad
            player.addMediaSource(adWindowIndex + 1, localAdSource);
            player.seekTo(adWindowIndex + 1, 0);
            player.setPlayWhenReady(wasPlaying);
            
            Log.d(TAG, "Local ad replacement complete");
        } catch (Exception e) {
            Log.e(TAG, "Error replacing ad with local video", e);
        }
    }
    
    /**
     * Get the URI for the local ad video to play
     */
    private Uri getLocalAdUri() {
        // HARDCODED PATH - MODIFY AS NEEDED
        
        // Path for an asset in the assets folder (e.g., place your video in android/src/main/assets/)
        // You can use this line for assets:
        return Uri.parse("asset:///local_ad.mp4");
        
        // Or use this line for a file on the file system (absolute path):
        // return Uri.parse("file:///data/data/" + themedReactContext.getPackageName() + "/files/local_ad.mp4");
    }

    private void releasePlayer() {
        if (player != null) {
            if(playbackServiceBinder != null) {
                playbackServiceBinder.getService().unregisterPlayer(player);
                themedReactContext.unbindService(playbackServiceConnection);
            }

            updateResumePosition();
            player.release();
            player.removeListener(this);
            PictureInPictureUtil.applyAutoEnterEnabled(themedReactContext, pictureInPictureParamsBuilder, false);
            if (pipListenerUnsubscribe != null) {
                new Handler().post(pipListenerUnsubscribe);
            }
            trackSelector = null;

            ReactNativeVideoManager.Companion.getInstance().onInstanceRemoved(instanceId, player);
            player = null;
        }

        if (adsLoader != null) {
            adsLoader.release();
            adsLoader = null;
        }
        progressHandler.removeMessages(SHOW_PROGRESS);
        audioBecomingNoisyReceiver.removeListener();
        pictureInPictureReceiver.removeListener();
        bandwidthMeter.removeEventListener(this);

        if (mainHandler != null && mainRunnable != null) {
            mainHandler.removeCallbacks(mainRunnable);
            mainRunnable = null;
        }
    }

    private static class OnAudioFocusChangedListener implements AudioManager.OnAudioFocusChangeListener {
        private final ReactExoplayerView view;
        private final ThemedReactContext themedReactContext;

        private OnAudioFocusChangedListener(ReactExoplayerView view, ThemedReactContext themedReactContext) {
            this.view = view;
            this.themedReactContext = themedReactContext;
        }

        @Override
        public void onAudioFocusChange(int focusChange) {
            Activity activity = themedReactContext.getCurrentActivity();

            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    view.hasAudioFocus = false;
                    view.eventEmitter.onAudioFocusChanged.invoke(false);
                    // FIXME this pause can cause issue if content doesn't have pause capability (can happen on live channel)
                    if (activity != null) {
                        activity.runOnUiThread(view::pausePlayback);
                    }
                    view.audioManager.abandonAudioFocus(this);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    view.eventEmitter.onAudioFocusChanged.invoke(false);
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    view.hasAudioFocus = true;
                    view.eventEmitter.onAudioFocusChanged.invoke(true);
                    break;
                default:
                    break;
            }

            if (view.player != null && activity != null) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    // Lower the volume
                    if (!view.muted) {
                        activity.runOnUiThread(() ->
                                view.player.setVolume(view.audioVolume * 0.8f)
                        );
                    }
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    // Raise it back to normal
                    if (!view.muted) {
                        activity.runOnUiThread(() ->
                                view.player.setVolume(view.audioVolume * 1)
                        );
                    }
                }
            }
        }
    }

    private boolean requestAudioFocus() {
        if (disableFocus || source.getUri() == null || this.hasAudioFocus) {
            return true;
        }
        int result = audioManager.requestAudioFocus(audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void setPlayWhenReady(boolean playWhenReady) {
        if (player == null) {
            return;
        }

        if (playWhenReady) {
            this.hasAudioFocus = requestAudioFocus();
            if (this.hasAudioFocus) {
                player.setPlayWhenReady(true);
            }
        } else {
            player.setPlayWhenReady(false);
        }
    }

    private void resumePlayback() {
        if (player != null) {
            if (!player.getPlayWhenReady()) {
                setPlayWhenReady(true);
            }
            setKeepScreenOn(preventsDisplaySleepDuringVideoPlayback);
        }
    }

    private void pausePlayback() {
        if (player != null) {
            if (player.getPlayWhenReady()) {
                setPlayWhenReady(false);
            }
        }
        setKeepScreenOn(false);
    }

    private void stopPlayback() {
        onStopPlayback();
        releasePlayer();
    }

    private void onStopPlayback() {
        audioManager.abandonAudioFocus(audioFocusChangeListener);
    }

    private void updateResumePosition() {
        resumeWindow = player.getCurrentMediaItemIndex();
        resumePosition = player.isCurrentMediaItemSeekable() ? Math.max(0, player.getCurrentPosition())
                : C.TIME_UNSET;
    }

    private void clearResumePosition() {
        resumeWindow = C.INDEX_UNSET;
        resumePosition = C.TIME_UNSET;
    }

    /**
     * Returns a new DataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #bandwidthMeter} as a listener to the new
     *                          DataSource factory.
     * @return A new DataSource factory.
     */
    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return DataSourceUtil.getDefaultDataSourceFactory(this.themedReactContext,
                useBandwidthMeter ? bandwidthMeter : null, source.getHeaders());
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #bandwidthMeter} as a listener to the new
     *     DataSource factory.
     * @return A new HttpDataSource factory.
     */
    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
        return DataSourceUtil.getDefaultHttpDataSourceFactory(this.themedReactContext, useBandwidthMeter ? bandwidthMeter : null, source.getHeaders());
    }

    // AudioBecomingNoisyListener implementation
    @Override
    public void onAudioBecomingNoisy() {
        eventEmitter.onVideoAudioBecomingNoisy.invoke();
    }

    // Player.Listener implementation
    @Override
    public void onIsLoadingChanged(boolean isLoading) {
        // Do nothing.
    }

    @Override
    public void onEvents(@NonNull Player player, Player.Events events) {
        if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) || events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED)) {
            int playbackState = player.getPlaybackState();
            boolean playWhenReady = player.getPlayWhenReady();
            String text = "onStateChanged: playWhenReady=" + playWhenReady + ", playbackState=";
            eventEmitter.onPlaybackRateChange.invoke(playWhenReady && playbackState == ExoPlayer.STATE_READY ? 1.0f : 0.0f);
            switch (playbackState) {
                case Player.STATE_IDLE:
                    text += "idle";
                    eventEmitter.onVideoIdle.invoke();
                    clearProgressMessageHandler();
                    if (!player.getPlayWhenReady()) {
                        setKeepScreenOn(false);
                    }
                    break;
                case Player.STATE_BUFFERING:
                    text += "buffering";
                    onBuffering(true);
                    clearProgressMessageHandler();
                    setKeepScreenOn(preventsDisplaySleepDuringVideoPlayback);
                    break;
                case Player.STATE_READY:
                    text += "ready";
                    eventEmitter.onReadyForDisplay.invoke();
                    onBuffering(false);
                    clearProgressMessageHandler(); // ensure there is no other message
                    startProgressHandler();
                    videoLoaded();
                    if (selectTrackWhenReady && isUsingContentResolution) {
                        selectTrackWhenReady = false;
                        setSelectedTrack(C.TRACK_TYPE_VIDEO, videoTrackType, videoTrackValue);
                    }
                    // Setting the visibility for the playerControlView
                    if (playerControlView != null) {
                        playerControlView.show();
                    }
                    setKeepScreenOn(preventsDisplaySleepDuringVideoPlayback);
                    break;
                case Player.STATE_ENDED:
                    text += "ended";
                    updateProgress();
                    eventEmitter.onVideoEnd.invoke();
                    onStopPlayback();
                    setKeepScreenOn(false);
                    break;
                default:
                    text += "unknown";
                    break;
            }
            DebugLog.d(TAG, text);
        }
    }

    private void startProgressHandler() {
        progressHandler.sendEmptyMessage(SHOW_PROGRESS);
    }

    /**
     *  The progress message handler will duplicate recursions of the onProgressMessage handler
     *  on change of player state from any state to STATE_READY with playWhenReady is true (when
     *  the video is not paused). This clears all existing messages.
     */
    private void clearProgressMessageHandler() {
        progressHandler.removeMessages(SHOW_PROGRESS);
    }

    private void videoLoaded() {
        if (!player.isPlayingAd() && loadVideoStarted) {
            loadVideoStarted = false;
            if (audioTrackType != null) {
                setSelectedAudioTrack(audioTrackType, audioTrackValue);
            }
            if (videoTrackType != null) {
                setSelectedVideoTrack(videoTrackType, videoTrackValue);
            }
            if (textTrackType != null) {
                setSelectedTextTrack(textTrackType, textTrackValue);
            }
            Format videoFormat = player.getVideoFormat();
            boolean isRotatedContent = videoFormat != null && (videoFormat.rotationDegrees == 90 || videoFormat.rotationDegrees == 270);
            int width = videoFormat != null ? (isRotatedContent ? videoFormat.height : videoFormat.width) : 0;
            int height = videoFormat != null ? (isRotatedContent ? videoFormat.width : videoFormat.height) : 0;
            String trackId = videoFormat != null ? videoFormat.id : null;

            // Properties that must be accessed on the main thread
            long duration = player.getDuration();
            long currentPosition = player.getCurrentPosition();
            ArrayList<Track> audioTracks = getAudioTrackInfo();
            ArrayList<Track> textTracks  = getTextTrackInfo();

            if (source.getContentStartTime() != -1) {
                ExecutorService es = Executors.newSingleThreadExecutor();
                es.execute(() -> {
                    // To prevent ANRs caused by getVideoTrackInfo we run this on a different thread and notify the player only when we're done
                    ArrayList<VideoTrack> videoTracks = getVideoTrackInfoFromManifest();
                    if (videoTracks != null) {
                        isUsingContentResolution = true;
                    }
                    eventEmitter.onVideoLoad.invoke(duration, currentPosition, width, height,
                            audioTracks, textTracks, videoTracks, trackId );

                });
                return;
            }

            ArrayList<VideoTrack> videoTracks = getVideoTrackInfo();

            eventEmitter.onVideoLoad.invoke(duration, currentPosition, width, height,
                    audioTracks, textTracks, videoTracks, trackId);
            refreshControlsStyles();
        }
    }

    private static boolean isTrackSelected(TrackSelection selection, TrackGroup group,
                                           int trackIndex){
        return selection != null && selection.getTrackGroup() == group
                && selection.indexOf( trackIndex ) != C.INDEX_UNSET;
    }

    private ArrayList<Track> getAudioTrackInfo() {
        ArrayList<Track> audioTracks = new ArrayList<>();
        if (trackSelector == null) {
            // Likely player is unmounting so no audio tracks are available anymore
            return audioTracks;
        }

        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
        int index = getTrackRendererIndex(C.TRACK_TYPE_AUDIO);
        if (info == null || index == C.INDEX_UNSET) {
            return audioTracks;
        }
        TrackGroupArray groups = info.getTrackGroups(index);
        TrackSelectionArray selectionArray = player.getCurrentTrackSelections();
        TrackSelection selection = selectionArray.get( C.TRACK_TYPE_AUDIO );

        for (int i = 0; i < groups.length; ++i) {
            TrackGroup group = groups.get(i);
            Format format = group.getFormat(0);
            Track audioTrack = exoplayerTrackToGenericTrack(format, i, selection, group);
            audioTrack.setBitrate(format.bitrate == Format.NO_VALUE ? 0 : format.bitrate);
            audioTracks.add(audioTrack);
        }
        return audioTracks;
    }

    private VideoTrack exoplayerVideoTrackToGenericVideoTrack(Format format, int trackIndex) {
        VideoTrack videoTrack = new VideoTrack();
        videoTrack.setWidth(format.width == Format.NO_VALUE ? 0 : format.width);
        videoTrack.setHeight(format.height == Format.NO_VALUE ? 0 : format.height);
        videoTrack.setBitrate(format.bitrate == Format.NO_VALUE ? 0 : format.bitrate);
        videoTrack.setRotation(format.rotationDegrees);
        if (format.codecs != null) videoTrack.setCodecs(format.codecs);
        videoTrack.setTrackId(format.id == null ? String.valueOf(trackIndex) : format.id);
        videoTrack.setIndex(trackIndex);
        return videoTrack;
    }

    private ArrayList<VideoTrack> getVideoTrackInfo() {
        ArrayList<VideoTrack> videoTracks = new ArrayList<>();
        if (trackSelector == null) {
            // Likely player is unmounting so no video tracks are available anymore
            return videoTracks;
        }
        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
        int index = getTrackRendererIndex(C.TRACK_TYPE_VIDEO);
        if (info == null || index == C.INDEX_UNSET) {
            return videoTracks;
        }

        TrackGroupArray groups = info.getTrackGroups(index);
        for (int i = 0; i < groups.length; ++i) {
            TrackGroup group = groups.get(i);

            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                Format format = group.getFormat(trackIndex);
                if (isFormatSupported(format)) {
                    VideoTrack videoTrack = exoplayerVideoTrackToGenericVideoTrack(format, trackIndex);
                    videoTracks.add(videoTrack);
                }
            }
        }
        return videoTracks;
    }

    private ArrayList<VideoTrack> getVideoTrackInfoFromManifest() {
        return this.getVideoTrackInfoFromManifest(0);
    }

    // We need retry count to in case where minefest request fails from poor network conditions
    @WorkerThread
    private ArrayList<VideoTrack> getVideoTrackInfoFromManifest(int retryCount) {
        ExecutorService es = Executors.newSingleThreadExecutor();
        final DataSource dataSource = this.mediaDataSourceFactory.createDataSource();
        final Uri sourceUri = source.getUri();
        final long startTime = source.getContentStartTime() * 1000 - 100; // s -> ms with 100ms offset

        Future<ArrayList<VideoTrack>> result = es.submit(new Callable() {
            final DataSource ds = dataSource;
            final Uri uri = sourceUri;
            final long startTimeUs = startTime * 1000; // ms -> us

            public ArrayList<VideoTrack> call() {
                ArrayList<VideoTrack> videoTracks = new ArrayList<>();
                try  {
                    DashManifest manifest = DashUtil.loadManifest(this.ds, this.uri);
                    int periodCount = manifest.getPeriodCount();
                    for (int i = 0; i < periodCount; i++) {
                        Period period = manifest.getPeriod(i);
                        for (int adaptationIndex = 0; adaptationIndex < period.adaptationSets.size(); adaptationIndex++) {
                            AdaptationSet adaptation = period.adaptationSets.get(adaptationIndex);
                            if (adaptation.type != C.TRACK_TYPE_VIDEO) {
                                continue;
                            }
                            boolean hasFoundContentPeriod = false;
                            for (int representationIndex = 0; representationIndex < adaptation.representations.size(); representationIndex++) {
                                Representation representation = adaptation.representations.get(representationIndex);
                                Format format = representation.format;
                                if (isFormatSupported(format)) {
                                    if (representation.presentationTimeOffsetUs <= startTimeUs) {
                                        break;
                                    }
                                    hasFoundContentPeriod = true;
                                    VideoTrack videoTrack = exoplayerVideoTrackToGenericVideoTrack(format, representationIndex);
                                    videoTracks.add(videoTrack);
                                }
                            }
                            if (hasFoundContentPeriod) {
                                return videoTracks;
                            }
                        }
                    }
                } catch (Exception e) {
                    DebugLog.w(TAG, "error in getVideoTrackInfoFromManifest:" + e.getMessage());
                }
                return null;
            }
        });

        try {
            ArrayList<VideoTrack> results = result.get(3000, TimeUnit.MILLISECONDS);
            if (results == null && retryCount < 1) {
                return this.getVideoTrackInfoFromManifest(++retryCount);
            }
            es.shutdown();
            return results;
        } catch (Exception e) {
            DebugLog.w(TAG, "error in getVideoTrackInfoFromManifest handling request:" + e.getMessage());
        }

        return null;
    }

    private Track exoplayerTrackToGenericTrack(Format format, int trackIndex, TrackSelection selection, TrackGroup group) {
        Track track = new Track();
        track.setIndex(trackIndex);
        if (format.sampleMimeType != null) track.setMimeType(format.sampleMimeType);
        if (format.language != null) track.setLanguage(format.language);
        if (format.label != null) track.setTitle(format.label);
        track.setSelected(isTrackSelected(selection, group, trackIndex));
        return track;
    }

    private ArrayList<Track> getTextTrackInfo() {
        ArrayList<Track> textTracks = new ArrayList<>();
        if (trackSelector == null) {
            return textTracks;
        }
        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
        int index = getTrackRendererIndex(C.TRACK_TYPE_TEXT);
        if (info == null || index == C.INDEX_UNSET) {
            return textTracks;
        }
        TrackSelectionArray selectionArray = player.getCurrentTrackSelections();
        TrackSelection selection = selectionArray.get( C.TRACK_TYPE_VIDEO );
        TrackGroupArray groups = info.getTrackGroups(index);

        for (int i = 0; i < groups.length; ++i) {
            TrackGroup group = groups.get(i);
            Format format = group.getFormat(0);
            Track textTrack = exoplayerTrackToGenericTrack(format, i, selection, group);
            textTracks.add(textTrack);
        }
        return textTracks;
    }

    private void onBuffering(boolean buffering) {
        if (isBuffering == buffering) {
            return;
        }

        if (isPaused && isSeeking && !buffering) {
            eventEmitter.onVideoSeek.invoke(player.getCurrentPosition(), seekPosition);
            isSeeking = false;
        }

        isBuffering = buffering;
        eventEmitter.onVideoBuffer.invoke(buffering);
    }

    @Override
    public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPosition, @NonNull Player.PositionInfo newPosition, @Player.DiscontinuityReason int reason) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK) {
            isSeeking = true;
            seekPosition = newPosition.positionMs;
            if (isUsingContentResolution) {
                // We need to update the selected track to make sure that it still matches user selection if track list has changed in this period
                setSelectedTrack(C.TRACK_TYPE_VIDEO, videoTrackType, videoTrackValue);
            }
        }

        if (playerNeedsSource) {
            // This will only occur if the user has performed a seek whilst in the error state. Update the
            // resume position so that if the user then retries, playback will resume from the position to
            // which they seeked.
            updateResumePosition();
        }
        if (isUsingContentResolution) {
            // Discontinuity events might have a different track list so we update the selected track
            setSelectedTrack(C.TRACK_TYPE_VIDEO, videoTrackType, videoTrackValue);
            selectTrackWhenReady = true;
        }
        // When repeat is turned on, reaching the end of the video will not cause a state change
        // so we need to explicitly detect it.
        if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION
                && player.getRepeatMode() == Player.REPEAT_MODE_ONE) {
            updateProgress();
            eventEmitter.onVideoEnd.invoke();
        }
    }

    @Override
    public void onTimelineChanged(@NonNull Timeline timeline, int reason) {
        // Do nothing.
    }

    @Override
    public void onTracksChanged(@NonNull Tracks tracks) {
        eventEmitter.onTextTracks.invoke(getTextTrackInfo());
        eventEmitter.onAudioTracks.invoke(getAudioTrackInfo());
        eventEmitter.onVideoTracks.invoke(getVideoTrackInfo());
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters params) {
        eventEmitter.onPlaybackRateChange.invoke(params.speed);
    }

    @Override
    public void onVolumeChanged(float volume) {
        eventEmitter.onVolumeChange.invoke(volume);
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        if (isPlaying && isSeeking) {
            eventEmitter.onVideoSeek.invoke(player.getCurrentPosition(), seekPosition);
        }
        PictureInPictureUtil.applyPlayingStatus(themedReactContext, pictureInPictureParamsBuilder, pictureInPictureReceiver, !isPlaying);
        eventEmitter.onVideoPlaybackStateChanged.invoke(isPlaying, isSeeking);

        if (isPlaying) {
            isSeeking = false;
        }
    }

    @Override
    public void onPlayerError(@NonNull PlaybackException e) {
        String errorString = "ExoPlaybackException: " + PlaybackException.getErrorCodeName(e.errorCode);
        String errorCode = "2" + e.errorCode;
        switch(e.errorCode) {
            case PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED:
            case PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED:
            case PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED:
            case PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR:
            case PlaybackException.ERROR_CODE_DRM_UNSPECIFIED:
                if (!hasDrmFailed) {
                    // When DRM fails to reach the app level certificate server it will fail with a source error so we assume that it is DRM related and try one more time
                    hasDrmFailed = true;
                    playerNeedsSource = true;
                    updateResumePosition();
                    initializePlayer();
                    setPlayWhenReady(true);
                    return;
                }
                break;
            default:
                break;
        }
        eventEmitter.onVideoError.invoke(errorString, e, errorCode);
        playerNeedsSource = true;
        if (isBehindLiveWindow(e)) {
            clearResumePosition();
            if (player != null) {
                player.seekToDefaultPosition();
                player.prepare();
            }
        } else {
            updateResumePosition();
        }
    }

    private static boolean isBehindLiveWindow(PlaybackException e) {
        return e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW;
    }

    public int getTrackRendererIndex(int trackType) {
        if (player != null) {
            int rendererCount = player.getRendererCount();
            for (int rendererIndex = 0; rendererIndex < rendererCount; rendererIndex++) {
                if (player.getRendererType(rendererIndex) == trackType) {
                    return rendererIndex;
                }
            }
        }
        return C.INDEX_UNSET;
    }

    @Override
    public void onMetadata(@NonNull Metadata metadata) {
        ArrayList<TimedMetadata> metadataArray = new ArrayList<>();
        for (int i = 0; i < metadata.length(); i++) {
            Metadata.Entry entry = metadata.get(i);

            if (entry instanceof Id3Frame) {
                Id3Frame frame = (Id3Frame) metadata.get(i);

                String value = "";

                if (frame instanceof TextInformationFrame) {
                    TextInformationFrame txxxFrame = (TextInformationFrame) frame;
                    value = txxxFrame.value;
                }
                TimedMetadata timedMetadata = new TimedMetadata(frame.id, value);
                metadataArray.add(timedMetadata);
            } else if (entry instanceof EventMessage) {
                EventMessage eventMessage = (EventMessage) entry;
                TimedMetadata timedMetadata = new TimedMetadata(eventMessage.schemeIdUri, eventMessage.value);
                metadataArray.add(timedMetadata);
            } else {
                DebugLog.d(TAG, "unhandled metadata " + entry);
            }
        }
        eventEmitter.onTimedMetadata.invoke(metadataArray);
    }

    public void onCues(CueGroup cueGroup) {
        if (!cueGroup.cues.isEmpty() && cueGroup.cues.get(0).text != null) {
            String subtitleText = cueGroup.cues.get(0).text.toString();
            eventEmitter.onTextTrackDataChanged.invoke(subtitleText);
        }
    }

    public void setSrc(Source source) {
        if (source.getUri() != null) {
            clearResumePosition();
            boolean isSourceEqual = source.isEquals(this.source);
            hasDrmFailed = false;
            this.source = source;
            this.mediaDataSourceFactory =
                    DataSourceUtil.getDefaultDataSourceFactory(this.themedReactContext, bandwidthMeter,
                            source.getHeaders());

            if (source.getCmcdProps() != null) {
                CMCDConfig cmcdConfig = new CMCDConfig(source.getCmcdProps());
                CmcdConfiguration.Factory factory = cmcdConfig.toCmcdConfigurationFactory();
                this.setCmcdConfigurationFactory(factory);
            } else {
                this.setCmcdConfigurationFactory(null);
            }

            if (!isSourceEqual) {
                playerNeedsSource = true;
                initializePlayer();
            }
        } else {
            clearSrc();
        }
    }
    public void clearSrc() {
        if (source.getUri() != null) {
            if (player != null) {
                player.stop();
                player.clearMediaItems();
            }
        }
        exoPlayerView.hideAds();
        this.source = new Source();
        this.mediaDataSourceFactory = null;
        clearResumePosition();
    }

    public void setProgressUpdateInterval(final float progressUpdateInterval) {
        mProgressUpdateInterval = progressUpdateInterval;
    }

    public void setReportBandwidth(boolean reportBandwidth) {
        mReportBandwidth = reportBandwidth;
    }

    public void setResizeModeModifier(@ResizeMode.Mode int resizeMode) {
        if (exoPlayerView != null) {
            exoPlayerView.setResizeMode(resizeMode);
        }
    }

    private void applyModifiers() {
        setRepeatModifier(repeat);
        setMutedModifier(muted);
    }

    public void setRepeatModifier(boolean repeat) {
        if (player != null) {
            if (repeat) {
                player.setRepeatMode(Player.REPEAT_MODE_ONE);
            } else {
                player.setRepeatMode(Player.REPEAT_MODE_OFF);
            }
        }
        this.repeat = repeat;
    }

    public void setPreventsDisplaySleepDuringVideoPlayback(boolean preventsDisplaySleepDuringVideoPlayback) {
        this.preventsDisplaySleepDuringVideoPlayback = preventsDisplaySleepDuringVideoPlayback;
    }

    public void disableTrack(int rendererIndex) {
        DefaultTrackSelector.Parameters disableParameters = trackSelector.getParameters()
                .buildUpon()
                .setRendererDisabled(rendererIndex, true)
                .build();
        trackSelector.setParameters(disableParameters);
    }

    public void setSelectedTrack(int trackType, String type, String value) {
        if (player == null) return;
        int rendererIndex = getTrackRendererIndex(trackType);
        if (rendererIndex == C.INDEX_UNSET) {
            return;
        }
        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
        if (info == null) {
            return;
        }

        TrackGroupArray groups = info.getTrackGroups(rendererIndex);
        int groupIndex = C.INDEX_UNSET;
        List<Integer> tracks = new ArrayList<>();
        tracks.add(0);

        if (TextUtils.isEmpty(type)) {
            type = "default";
        }

        if ("disabled".equals(type)) {
            disableTrack(rendererIndex);
            return;
        } else if ("language".equals(type)) {
            for (int i = 0; i < groups.length; ++i) {
                Format format = groups.get(i).getFormat(0);
                if (format.language != null && format.language.equals(value)) {
                    groupIndex = i;
                    break;
                }
            }
        } else if ("title".equals(type)) {
            for (int i = 0; i < groups.length; ++i) {
                Format format = groups.get(i).getFormat(0);
                if (format.label != null && format.label.equals(value)) {
                    groupIndex = i;
                    break;
                }
            }
        } else if ("index".equals(type)) {
            int iValue = ReactBridgeUtils.safeParseInt(value, -1);
            if (iValue != -1) {
                if (trackType == C.TRACK_TYPE_VIDEO && groups.length == 1) {
                    groupIndex = 0;
                    if (iValue < groups.get(groupIndex).length) {
                        tracks.set(0, iValue);
                    }
                } else if (iValue < groups.length) {
                    groupIndex = iValue;
                }
            }
        } else if ("resolution".equals(type)) {
            int height = ReactBridgeUtils.safeParseInt(value, -1);
            if (height != -1) {
                for (int i = 0; i < groups.length; ++i) { // Search for the exact height
                    TrackGroup group = groups.get(i);
                    Format closestFormat = null;
                    int closestTrackIndex = -1;
                    boolean usingExactMatch = false;
                    for (int j = 0; j < group.length; j++) {
                        Format format = group.getFormat(j);
                        if (format.height == height) {
                            groupIndex = i;
                            tracks.set(0, j);
                            closestFormat = null;
                            closestTrackIndex = -1;
                            usingExactMatch = true;
                            break;
                        } else if (isUsingContentResolution) {
                            // When using content resolution rather than ads, we need to try and find the closest match if there is no exact match
                            if (closestFormat != null) {
                                if ((format.bitrate > closestFormat.bitrate || format.height > closestFormat.height) && format.height < height) {
                                    // Higher quality match
                                    closestFormat = format;
                                    closestTrackIndex = j;
                                }
                            } else if (format.height < height) {
                                closestFormat = format;
                                closestTrackIndex = j;
                            }
                        }
                    }
                    // This is a fallback if the new period contains only higher resolutions than the user has selected
                    if (closestFormat == null && isUsingContentResolution && !usingExactMatch) {
                        // No close match found - so we pick the lowest quality
                        int minHeight = Integer.MAX_VALUE;
                        for (int j = 0; j < group.length; j++) {
                            Format format = group.getFormat(j);
                            if (format.height < minHeight) {
                                minHeight = format.height;
                                groupIndex = i;
                                tracks.set(0, j);
                            }
                        }
                    }
                    // Selecting the closest match found
                    if (closestFormat != null && closestTrackIndex != -1) {
                        // We found the closest match instead of an exact one
                        groupIndex = i;
                        tracks.set(0, closestTrackIndex);
                    }
                }
            }
        } else if (trackType == C.TRACK_TYPE_TEXT && Util.SDK_INT > 18) { // Text default
            // Use system settings if possible
            CaptioningManager captioningManager
                    = (CaptioningManager)themedReactContext.getSystemService(Context.CAPTIONING_SERVICE);
            if (captioningManager != null && captioningManager.isEnabled()) {
                groupIndex = getGroupIndexForDefaultLocale(groups);
            }
        } else if (rendererIndex == C.TRACK_TYPE_AUDIO) { // Audio default
            groupIndex = getGroupIndexForDefaultLocale(groups);
        }

        if (groupIndex == C.INDEX_UNSET && trackType == C.TRACK_TYPE_VIDEO && groups.length != 0) { // Video auto
            // Add all tracks as valid options for ABR to choose from
            TrackGroup group = groups.get(0);
            ArrayList<Integer> allTracks = new ArrayList<>(group.length);
            groupIndex = 0;
            for (int j = 0; j < group.length; j++) {
                allTracks.add(j);
            }

            // Valiate list of all tracks and add only supported formats
            int supportedFormatLength = 0;
            for (int g = 0; g < allTracks.size(); g++) {
                Format format = group.getFormat(g);
                if (isFormatSupported(format)) {
                    supportedFormatLength++;
                }
            }
            if (allTracks.size() == 1) {
                // With only one tracks we can't remove any tracks so attempt to play it anyway
                tracks = allTracks;
            } else {
                tracks =  new ArrayList<>(supportedFormatLength + 1);
                for (int k = 0; k < allTracks.size(); k++) {
                    Format format = group.getFormat(k);
                    if (isFormatSupported(format)) {
                        tracks.add(allTracks.get(k));
                    }
                }
            }
        }

        if (groupIndex == C.INDEX_UNSET) {
            disableTrack(rendererIndex);
            return;
        }

        TrackSelectionOverride selectionOverride = new TrackSelectionOverride(groups.get(groupIndex), tracks);

        DefaultTrackSelector.Parameters.Builder selectionParameters = trackSelector.getParameters()
                .buildUpon()
                .setExceedAudioConstraintsIfNecessary(true)
                .setExceedRendererCapabilitiesIfNecessary(true)
                .setExceedVideoConstraintsIfNecessary(true)
                .setRendererDisabled(rendererIndex, false)
                .clearOverridesOfType(selectionOverride.getType());

        if (trackType == C.TRACK_TYPE_VIDEO && isUsingVideoABR()) {
            selectionParameters.setMaxVideoBitrate(maxBitRate == 0 ? Integer.MAX_VALUE : maxBitRate);
        } else {
            selectionParameters.addOverride(selectionOverride);
        }

        trackSelector.setParameters(selectionParameters.build());
    }

    private boolean isFormatSupported(Format format) {
        int width = format.width == Format.NO_VALUE ? 0 : format.width;
        int height = format.height == Format.NO_VALUE ? 0 : format.height;
        float frameRate = format.frameRate == Format.NO_VALUE ? 0 : format.frameRate;
        String mimeType = format.sampleMimeType;
        if (mimeType == null) {
            return true;
        }
        boolean isSupported;
        try {
            MediaCodecInfo codecInfo = MediaCodecUtil.getDecoderInfo(mimeType, false, false);
            isSupported = codecInfo.isVideoSizeAndRateSupportedV21(width, height, frameRate);
        } catch (Exception e) {
            // Failed to get decoder info - assume it is supported
            isSupported = true;
        }
        return isSupported;
    }

    private int getGroupIndexForDefaultLocale(TrackGroupArray groups) {
        if (groups.length == 0){
            return C.INDEX_UNSET;
        }

        int groupIndex = 0; // default if no match
        String locale2 = Locale.getDefault().getLanguage(); // 2 letter code
        String locale3 = Locale.getDefault().getISO3Language(); // 3 letter code
        for (int i = 0; i < groups.length; ++i) {
            Format format = groups.get(i).getFormat(0);
            String language = format.language;
            if (language != null && (language.equals(locale2) || language.equals(locale3))) {
                groupIndex = i;
                break;
            }
        }
        return groupIndex;
    }

    public void setSelectedVideoTrack(String type, String value) {
        videoTrackType = type;
        videoTrackValue = value;
        if (!loadVideoStarted) setSelectedTrack(C.TRACK_TYPE_VIDEO, videoTrackType, videoTrackValue);
    }

    public void setSelectedAudioTrack(String type, String value) {
        audioTrackType = type;
        audioTrackValue = value;
        setSelectedTrack(C.TRACK_TYPE_AUDIO, audioTrackType, audioTrackValue);
    }

    public void setSelectedTextTrack(String type, String value) {
        textTrackType = type;
        textTrackValue = value;
        setSelectedTrack(C.TRACK_TYPE_TEXT, textTrackType, textTrackValue);
    }

    public void setPausedModifier(boolean paused) {
        isPaused = paused;
        if (player != null) {
            if (!paused) {
                resumePlayback();
            } else {
                pausePlayback();
            }
        }
    }

    public void setEnterPictureInPictureOnLeave(boolean enterPictureInPictureOnLeave) {
        this.enterPictureInPictureOnLeave = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && enterPictureInPictureOnLeave;
        PictureInPictureUtil.applyAutoEnterEnabled(themedReactContext, pictureInPictureParamsBuilder, this.enterPictureInPictureOnLeave);
    }

    protected void setIsInPictureInPicture(boolean isInPictureInPicture) {
        eventEmitter.onPictureInPictureStatusChanged.invoke(isInPictureInPicture);

        if (fullScreenPlayerView != null && fullScreenPlayerView.isShowing()) {
            if (isInPictureInPicture) fullScreenPlayerView.hideWithoutPlayer();
            return;
        }

        Activity currentActivity = themedReactContext.getCurrentActivity();
        if (currentActivity == null) return;

        View decorView = currentActivity.getWindow().getDecorView();
        ViewGroup rootView = decorView.findViewById(android.R.id.content);

        LayoutParams layoutParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);

        if (isInPictureInPicture) {
            ViewGroup parent = (ViewGroup)exoPlayerView.getParent();
            if (parent != null) {
                parent.removeView(exoPlayerView);
            }
            for (int i = 0; i < rootView.getChildCount(); i++) {
                if (rootView.getChildAt(i) != exoPlayerView) {
                    rootViewChildrenOriginalVisibility.add(rootView.getChildAt(i).getVisibility());
                    rootView.getChildAt(i).setVisibility(View.GONE);
                }
            }
            rootView.addView(exoPlayerView, layoutParams);
        } else {
            rootView.removeView(exoPlayerView);
            if (!rootViewChildrenOriginalVisibility.isEmpty()) {
                for (int i = 0; i < rootView.getChildCount(); i++) {
                    rootView.getChildAt(i).setVisibility(rootViewChildrenOriginalVisibility.get(i));
                }
                addView(exoPlayerView, 0, layoutParams);
            }
        }
    }

    public void enterPictureInPictureMode() {
        PictureInPictureParams _pipParams = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ArrayList<RemoteAction> actions = PictureInPictureUtil.getPictureInPictureActions(themedReactContext, isPaused, pictureInPictureReceiver);
            pictureInPictureParamsBuilder.setActions(actions);
            _pipParams = pictureInPictureParamsBuilder
                    .setAspectRatio(PictureInPictureUtil.calcPictureInPictureAspectRatio(player))
                    .build();
        }
        PictureInPictureUtil.enterPictureInPictureMode(themedReactContext, _pipParams);
    }

    public void exitPictureInPictureMode() {
        Activity currentActivity = themedReactContext.getCurrentActivity();
        if (currentActivity == null) return;

        View decorView = currentActivity.getWindow().getDecorView();
        ViewGroup rootView = decorView.findViewById(android.R.id.content);

        if (!rootViewChildrenOriginalVisibility.isEmpty()) {
            if (exoPlayerView.getParent().equals(rootView)) rootView.removeView(exoPlayerView);
            for (int i = 0; i < rootView.getChildCount(); i++) {
                rootView.getChildAt(i).setVisibility(rootViewChildrenOriginalVisibility.get(i));
            }
            rootViewChildrenOriginalVisibility.clear();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && currentActivity.isInPictureInPictureMode()) {
            currentActivity.moveTaskToBack(false);
        }
    }

    public void setMutedModifier(boolean muted) {
        this.muted = muted;
        if (player != null) {
            player.setVolume(muted ? 0.f : audioVolume);
        }
    }

    private void changeAudioOutput(AudioOutput output) {
        if (player != null) {
            int streamType = output.getStreamType();
            int usage = Util.getAudioUsageForStreamType(streamType);
            int contentType = Util.getAudioContentTypeForStreamType(streamType);
            AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(usage)
                    .setContentType(contentType)
                    .build();
            player.setAudioAttributes(audioAttributes, false);
            AudioManager audioManager = (AudioManager) themedReactContext.getSystemService(Context.AUDIO_SERVICE);
            boolean isSpeakerOutput = output == AudioOutput.SPEAKER;
            audioManager.setMode(
                    isSpeakerOutput ? AudioManager.MODE_NORMAL
                            : AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(isSpeakerOutput);
        }
    }

    public void setAudioOutput(AudioOutput output) {
        if (audioOutput != output) {
            this.audioOutput = output;
            changeAudioOutput(output);
        }
    }

    public void setVolumeModifier(float volume) {
        audioVolume = volume;
        if (player != null) {
            player.setVolume(audioVolume);
        }
    }

    public void seekTo(long positionMs) {
        if (player != null) {
            player.seekTo(positionMs);
        }
    }

    public void setRateModifier(float newRate) {
        if (newRate <= 0) {
            DebugLog.w(TAG, "cannot set rate <= 0");
            return;
        }

        rate = newRate;

        if (player != null) {
            PlaybackParameters params = new PlaybackParameters(rate, 1f);
            player.setPlaybackParameters(params);
        }
    }

    public void setMaxBitRateModifier(int newMaxBitRate) {
        maxBitRate = newMaxBitRate;
        if (player != null && isUsingVideoABR()) {
            // do not apply yet if not auto
            trackSelector.setParameters(trackSelector.buildUponParameters()
                    .setMaxVideoBitrate(maxBitRate == 0 ? Integer.MAX_VALUE : maxBitRate));
        }
    }

    public void setPlayInBackground(boolean playInBackground) {
        this.playInBackground = playInBackground;
    }

    public void setDisableFocus(boolean disableFocus) {
        this.disableFocus = disableFocus;
    }

    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
        exoPlayerView.setFocusable(this.focusable);
    }

    public void setShowNotificationControls(boolean showNotificationControls) {
        this.showNotificationControls = showNotificationControls;

        if (playbackServiceConnection == null && showNotificationControls) {
            setupPlaybackService();
        } else if(!showNotificationControls && playbackServiceConnection != null) {
            cleanupPlaybackService();
        }
    }

    public void setBufferingStrategy(BufferingStrategy.BufferingStrategyEnum _bufferingStrategy) {
        bufferingStrategy = _bufferingStrategy;
    }

    public boolean getPreventsDisplaySleepDuringVideoPlayback() {
        return preventsDisplaySleepDuringVideoPlayback;
    }

    private void updateFullScreenButtonVisibility() {
        if (playerControlView != null) {
            final ImageButton fullScreenButton = playerControlView.findViewById(R.id.exo_fullscreen);
            //Handling the fullScreenButton click event
            if (isFullscreen && fullScreenPlayerView != null && !fullScreenPlayerView.isShowing()) {
                fullScreenButton.setVisibility(GONE);
            } else {
                fullScreenButton.setVisibility(VISIBLE);
            }
        }
    }

    public void setDisableDisconnectError(boolean disableDisconnectError) {
        this.disableDisconnectError = disableDisconnectError;
    }

    public void setFullscreen(boolean fullscreen) {
        if (fullscreen == isFullscreen) {
            return; // Avoid generating events when nothing is changing
        }
        isFullscreen = fullscreen;

        Activity activity = themedReactContext.getCurrentActivity();
        if (activity == null) {
            return;
        }

        if (isFullscreen) {
            fullScreenPlayerView = new FullScreenPlayerView(getContext(), exoPlayerView, this, playerControlView, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    setFullscreen(false);
                }
            }, controlsConfig);
            eventEmitter.onVideoFullscreenPlayerWillPresent.invoke();
            if (fullScreenPlayerView != null) {
                fullScreenPlayerView.show();
            }
            UiThreadUtil.runOnUiThread(() -> {
                eventEmitter.onVideoFullscreenPlayerDidPresent.invoke();
            });
        } else {
            eventEmitter.onVideoFullscreenPlayerWillDismiss.invoke();
            if (fullScreenPlayerView != null) {
                fullScreenPlayerView.dismiss();
                reLayoutControls();
                setControls(controls);
            }
            UiThreadUtil.runOnUiThread(() -> {
                eventEmitter.onVideoFullscreenPlayerDidDismiss.invoke();
            });
        }
        // need to be done at the end to avoid hiding fullscreen control button when fullScreenPlayerView is shown
        updateFullScreenButtonVisibility();
    }

    public void setHideShutterView(boolean hideShutterView) {
        exoPlayerView.setHideShutterView(hideShutterView);
    }

    @Override
    public void onDrmKeysLoaded(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        DebugLog.d("DRM Info", "onDrmKeysLoaded");
    }

    @Override
    public void onDrmSessionAcquired(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, int state) {
        DebugLog.d("DRM Info", "onDrmSessionAcquired");
    }

    @Override
    public void onDrmSessionReleased(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        DebugLog.d("DRM Info", "onDrmSessionReleased");
    }

    @Override
    public void onDrmSessionManagerError(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, @NonNull Exception e) {
        DebugLog.d("DRM Info", "onDrmSessionManagerError");
        eventEmitter.onVideoError.invoke("onDrmSessionManagerError", e, "3002");
    }

    @Override
    public void onDrmKeysRestored(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        DebugLog.d("DRM Info", "onDrmKeysRestored");
    }

    @Override
    public void onDrmKeysRemoved(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {
        DebugLog.d("DRM Info", "onDrmKeysRemoved");
    }

    /**
     * Handling controls prop
     *
     * @param controls  Controls prop, if true enable controls, if false disable them
     */
    public void setControls(boolean controls) {
        this.controls = controls;
        if (controls) {
            addPlayerControl();
            updateFullScreenButtonVisibility();
        } else {
            int indexOfPC = indexOfChild(playerControlView);
            if (indexOfPC != -1) {
                removeViewAt(indexOfPC);
            }
        }
        refreshControlsStyles();
    }

    public void setSubtitleStyle(SubtitleStyle style) {
        exoPlayerView.setSubtitleStyle(style);
    }

    public void setShutterColor(Integer color) {
        exoPlayerView.setShutterColor(color);
    }

    @Override
    public void onAdEvent(AdEvent adEvent) {
        // Safely log ad events - removing AdAnalyticsLogger dependency
        Log.d(TAG, "Ad event: " + adEvent.getType());

        if (adEvent.getAdData() != null) {
            eventEmitter.onReceiveAdEvent.invoke(adEvent.getType().name(), adEvent.getAdData());
        } else {
            eventEmitter.onReceiveAdEvent.invoke(adEvent.getType().name(), null);
        }
    }

    @Override
    public void onAdError(AdErrorEvent adErrorEvent) {
        // Safely log ad errors - removing AdAnalyticsLogger dependency
        Log.e(TAG, "Ad error: " + adErrorEvent.getError().getMessage());

        AdError error = adErrorEvent.getError();
        Map<String, String> errMap = Map.of(
                "message", error.getMessage(),
                "code", String.valueOf(error.getErrorCode()),
                "type", String.valueOf(error.getErrorType())
        );
        eventEmitter.onReceiveAdEvent.invoke("ERROR", errMap);
    }

    public void setControlsStyles(ControlsConfig controlsStyles) {
        controlsConfig = controlsStyles;
        refreshControlsStyles();
    }

    /**
     * Set the local video from assets to use as a replacement for ads
     * @param assetPath Path to the video in the assets folder
     */
    public void setLocalAdFromAsset(String assetPath) {
        // Not needed with our new implementation - uses hardcoded path
        Log.d(TAG, "setLocalAdFromAsset is not used - using hardcoded path");
    }
    
    /**
     * Set the local video from file system to use as a replacement for ads
     * @param filePath Path to the video file in the file system
     */
    public void setLocalAdFromFile(String filePath) {
        // Not needed with our new implementation - uses hardcoded path
        Log.d(TAG, "setLocalAdFromFile is not used - using hardcoded path");
    }
}
