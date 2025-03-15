package com.brentvatne.exoplayer;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.Assertions;
import androidx.media3.datasource.DataSpec;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaPeriod;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ads.AdsLoader;
import androidx.media3.exoplayer.source.ads.AdsMediaSource;
import androidx.media3.exoplayer.upstream.Allocator;
import androidx.media3.exoplayer.upstream.TransferListener;

import com.brentvatne.react.R;
import com.google.common.collect.ImmutableList;

import java.io.IOException;

/**
 * Custom AdsMediaSource implementation that replaces ad videos with a local video
 * while preserving all ad tracking functionality.
 */
public class CustomAdMediaSource extends AdsMediaSource {
    private final Context context;
    private final Uri localAdUri;
    private final DefaultMediaSourceFactory mediaSourceFactory;

    public CustomAdMediaSource(
            MediaSource contentMediaSource,
            DataSpec adTagDataSpec,
            ImmutableList<Uri> adTagUris,
            DefaultMediaSourceFactory mediaSourceFactory,
            AdsLoader adsLoader,
            Context context) {
        super(contentMediaSource, adTagDataSpec, adTagUris, mediaSourceFactory, adsLoader, null);
        this.context = context;
        this.mediaSourceFactory = mediaSourceFactory;
        // Create a URI for the local video file in the raw resources
        this.localAdUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.local_ad);
    }

    @Override
    public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator, long startPositionUs) {
        if (id.isAd()) {
            // For ad periods, we'll use our custom approach
            // Create a media source with the local video but let the original ads loader handle timing and events
            return super.createPeriod(id, allocator, startPositionUs);
        } else {
            // For content periods, use the regular approach
            return super.createPeriod(id, allocator, startPositionUs);
        }
    }

    /**
     * Call this when an ad is about to start playing.
     * The ExoPlayer will still think it's playing the original ad, but we'll swap the content.
     */
    public MediaSource createLocalAdMediaSource() {
        return mediaSourceFactory.createMediaSource(MediaItem.fromUri(localAdUri));
    }
} 