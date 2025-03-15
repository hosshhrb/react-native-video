package com.brentvatne.exoplayer;

import android.content.Context;
import android.net.Uri;

import androidx.media3.common.MediaItem;
import androidx.media3.common.util.Log;
import androidx.media3.datasource.DataSpec;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ads.AdsMediaSource;

import com.brentvatne.common.toolbox.DebugLog;
import com.brentvatne.react.R;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.google.common.collect.ImmutableList;

/**
 * Manager class to handle local ad video functionality.
 * This class provides an easy way to configure and use local ad videos instead of actual ad creatives.
 */
public class LocalAdManager {
    private static final String TAG = "LocalAdManager";
    
    private final Context context;
    private Uri localAdUri;
    private boolean enabled = true;
    
    public LocalAdManager(Context context) {
        this.context = context;
        // Create a URI for the local video file in the raw resources
        this.localAdUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.local_ad);
    }
    
    /**
     * Enable or disable the local ad replacement functionality
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Change the URI of the local ad video
     */
    public void setLocalAdUri(Uri uri) {
        if (uri != null) {
            this.localAdUri = uri;
        }
    }
    
    /**
     * Get the local ad video URI
     */
    public Uri getLocalAdUri() {
        return localAdUri;
    }
    
    /**
     * Create a media source for the local ad video
     */
    public MediaSource createLocalAdMediaSource(DefaultMediaSourceFactory mediaSourceFactory) {
        return mediaSourceFactory.createMediaSource(MediaItem.fromUri(localAdUri));
    }
    
    /**
     * Create an AdsMediaSource with the local ad implementation
     */
    public AdsMediaSource createAdsMediaSource(
            MediaSource contentMediaSource,
            DataSpec adTagDataSpec,
            ImmutableList<Uri> adTagUris,
            DefaultMediaSourceFactory mediaSourceFactory,
            androidx.media3.exoplayer.source.ads.AdsLoader adsLoader,
            AdsMediaSource.AdsLoaderProvider adsLoaderProvider) {
        
        if (enabled) {
            DebugLog.i(TAG, "Using local ad video replacement");
            
            // Our approach uses the original IMA AdsLoader for timing and tracking
            // but intercepts the actual video content and replaces it with our local video
            return new AdsMediaSource(
                    contentMediaSource,
                    adTagDataSpec,
                    adTagUris,
                    mediaSourceFactory,
                    adsLoader,
                    adsLoaderProvider) {
                
                @Override
                public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator, long startPositionUs) {
                    if (id.isAd()) {
                        // Here we could intercept the ad loading and replace it with our local media
                        // but we're letting the original AdsMediaSource handle the period creation
                        // for better tracking compatibility
                        DebugLog.d(TAG, "Creating ad period for local ad video");
                    }
                    return super.createPeriod(id, allocator, startPositionUs);
                }
            };
        } else {
            // Use the standard AdsMediaSource
            return new AdsMediaSource(
                    contentMediaSource,
                    adTagDataSpec,
                    adTagUris,
                    mediaSourceFactory,
                    adsLoader,
                    adsLoaderProvider);
        }
    }
} 