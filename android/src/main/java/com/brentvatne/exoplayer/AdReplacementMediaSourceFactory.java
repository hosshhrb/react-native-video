package com.brentvatne.exoplayer;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.source.ads.AdsLoader;
import androidx.media3.exoplayer.source.ads.AdsMediaSource;

import com.brentvatne.common.toolbox.DebugLog;
import com.google.common.collect.ImmutableList;

/**
 * A wrapper around DefaultMediaSourceFactory that replaces ad media sources with local content.
 * This approach uses composition instead of inheritance to avoid issues with final classes.
 */
public class AdReplacementMediaSourceFactory {
    private static final String TAG = "AdReplacementMediaSourceFactory";
    
    private final DefaultMediaSourceFactory originalFactory;
    private final AdReplacementManager adReplacementManager;
    private final Context context;
    
    public AdReplacementMediaSourceFactory(
            DataSource.Factory dataSourceFactory, 
            Context context) {
        this.originalFactory = new DefaultMediaSourceFactory(dataSourceFactory);
        this.adReplacementManager = new AdReplacementManager(context, dataSourceFactory);
        this.context = context;
    }
    
    /**
     * Create a media source, potentially replacing ad content with local video
     */
    public MediaSource createMediaSource(MediaItem mediaItem) {
        if (adReplacementManager.isAdMediaItem(mediaItem)) {
            // This is an ad media item, replace it with our local video
            DebugLog.i(TAG, "Replacing ad media source with local video");
            return adReplacementManager.createLocalAdMediaSource();
        }
        
        // For regular content, use the original factory
        return originalFactory.createMediaSource(mediaItem);
    }
    
    /**
     * Create an AdsMediaSource that will use our ad replacement strategy
     */
    public AdsMediaSource createAdsMediaSource(
            MediaSource contentMediaSource,
            DataSpec adTagDataSpec,
            ImmutableList<Uri> adTagUris,
            AdsLoader adsLoader,
            AdsMediaSource.AdsLoaderProvider adsLoaderProvider) {
            
        // Create the AdsMediaSource with the original factory for proper ad integration
        return new AdsMediaSource(
                contentMediaSource,
                adTagDataSpec,
                adTagUris,
                originalFactory,
                adsLoader,
                adsLoaderProvider);
    }
    
    /**
     * Enable or disable ad replacement
     */
    public void setAdReplacementEnabled(boolean enabled) {
        adReplacementManager.setEnabled(enabled);
    }
    
    /**
     * Set a custom URI for the local ad video
     */
    public void setLocalAdUri(Uri uri) {
        adReplacementManager.setLocalAdUri(uri);
    }
    
    /**
     * Get the original factory for use with AdsMediaSource
     */
    public DefaultMediaSourceFactory getOriginalFactory() {
        return originalFactory;
    }
} 