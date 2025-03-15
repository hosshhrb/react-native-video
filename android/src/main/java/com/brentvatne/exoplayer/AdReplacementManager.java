package com.brentvatne.exoplayer;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.media3.common.MediaItem;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;

import com.brentvatne.common.toolbox.DebugLog;
import com.brentvatne.react.R;

/**
 * Manages replacement of ad videos with local content.
 * This class uses composition rather than inheritance to avoid issues with final classes.
 */
public class AdReplacementManager {
    private static final String TAG = "AdReplacementManager";
    
    private final Context context;
    private Uri localAdUri;
    private boolean enabled = true;
    private final DataSource.Factory dataSourceFactory;
    
    public AdReplacementManager(Context context, DataSource.Factory dataSourceFactory) {
        this.context = context;
        this.dataSourceFactory = dataSourceFactory;
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
     * Set a custom URI for the local ad video
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
     * Create a media source using the local ad video
     */
    public MediaSource createLocalAdMediaSource() {
        if (!enabled) {
            DebugLog.w(TAG, "Ad replacement is disabled");
            return null;
        }
        
        DebugLog.i(TAG, "Creating local ad media source: " + localAdUri);
        return new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(localAdUri));
    }
    
    /**
     * Check if a MediaItem is an ad
     */
    public boolean isAdMediaItem(MediaItem mediaItem) {
        return mediaItem != null && 
                mediaItem.localConfiguration != null && 
                mediaItem.localConfiguration.adsConfiguration != null &&
                mediaItem.localConfiguration.adsConfiguration.adTagUri != null;
    }
} 