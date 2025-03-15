package com.brentvatne.exoplayer;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.media3.common.AdPlaybackState;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Assertions;
import androidx.media3.datasource.DataSpec;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ads.AdsLoader;
import androidx.media3.exoplayer.source.ads.AdsMediaSource;

import com.brentvatne.react.R;
import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.google.common.collect.ImmutableList;

/**
 * Custom AdsLoader that uses the IMA SDK for ad timing and tracking but replaces
 * the ad content with a local video file.
 */
public class CustomAdsLoader implements AdsLoader {
    private final ImaAdsLoader originalAdsLoader;
    private final Context context;
    private final Uri localAdUri;
    private boolean adsLoaded = false;
    private EventListener eventListener;
    
    public CustomAdsLoader(ImaAdsLoader originalAdsLoader, Context context) {
        this.originalAdsLoader = originalAdsLoader;
        this.context = context;
        // Create a URI for the local video file in the raw resources
        this.localAdUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.local_ad);
    }
    
    @Override
    public void setPlayer(@Nullable Player player) {
        // Pass through to the original ads loader to maintain tracking
        originalAdsLoader.setPlayer(player);
    }
    
    @Override
    public void release() {
        originalAdsLoader.release();
    }
    
    @Override
    public void start(EventListener eventListener, AdViewProvider adViewProvider) {
        this.eventListener = eventListener;
        // Start the original ads loader to handle the ad lifecycle events
        originalAdsLoader.start(eventListener, adViewProvider);
    }
    
    @Override
    public void handlePrepareComplete(MediaSource.MediaPeriodId mediaPeriodId, Timeline timeline) {
        if (mediaPeriodId.isAd()) {
            // When preparing an ad period, we'll pass through to the original implementation
            // but we'll be ready to swap the content when playback starts
            originalAdsLoader.handlePrepareComplete(mediaPeriodId, timeline);
        }
    }
    
    @Override
    public void handlePrepareError(MediaSource.MediaPeriodId mediaPeriodId, Timeline timeline, Exception exception) {
        // Pass through to original implementation
        originalAdsLoader.handlePrepareError(mediaPeriodId, timeline, exception);
    }

    /**
     * Pass through to support the IMA SDK's functionality but the actual
     * ad media will be replaced with our local file.
     */
    public void passEvent(AdEvent adEvent) {
        // Pass events to the original ad event listeners
        originalAdsLoader.passEvent(adEvent);
    }
} 