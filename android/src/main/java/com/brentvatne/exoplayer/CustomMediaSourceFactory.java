package com.brentvatne.exoplayer;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.media3.common.AdPlaybackState;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.Log;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ads.AdsLoader;
import androidx.media3.exoplayer.source.ads.AdsMediaSource;

import com.brentvatne.common.toolbox.DebugLog;
import com.brentvatne.react.R;

/**
 * Custom MediaSourceFactory that replaces ad media sources with local video.
 */
public class CustomMediaSourceFactory extends DefaultMediaSourceFactory {
    private static final String TAG = "CustomMediaSourceFactory";
    
    private final Context context;
    private final Uri localAdUri;
    
    public CustomMediaSourceFactory(DataSource.Factory dataSourceFactory, Context context) {
        super(dataSourceFactory);
        this.context = context;
        this.localAdUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.local_ad);
    }
    
    @Override
    public MediaSource createMediaSource(MediaItem mediaItem) {
        if (mediaItem.localConfiguration != null && 
                mediaItem.localConfiguration.adsConfiguration != null &&
                mediaItem.localConfiguration.adsConfiguration.adTagUri != null) {
            
            // This is an ad media item, replace it with our local video
            DebugLog.i(TAG, "Replacing ad media source with local video");
            MediaItem localAdMediaItem = MediaItem.fromUri(localAdUri);
            return super.createMediaSource(localAdMediaItem);
        }
        
        // This is a regular media item, use default implementation
        return super.createMediaSource(mediaItem);
    }
} 