package com.brentvatne.exoplayer;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.BaseDataSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A {@link DataSource} for reading from a local asset.
 */
public final class AssetDataSource extends BaseDataSource {

    private final Context context;
    private final AssetManager assetManager;

    @Nullable
    private Uri uri;
    @Nullable
    private InputStream inputStream;
    private long bytesRemaining;
    private boolean opened;

    /**
     * Creates an AssetDataSource.
     *
     * @param context A context.
     */
    public AssetDataSource(Context context) {
        super(/* isNetwork= */ false);
        this.context = context.getApplicationContext();
        this.assetManager = this.context.getAssets();
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        try {
            uri = dataSpec.uri;
            transferInitializing(dataSpec);

            String assetPath = uri.toString().replace("asset:///", "");
            inputStream = assetManager.open(assetPath, AssetManager.ACCESS_RANDOM);
            long skipped = inputStream.skip(dataSpec.position);
            if (skipped < dataSpec.position) {
                throw new EOFException();
            }

            if (dataSpec.length != C.LENGTH_UNSET) {
                bytesRemaining = dataSpec.length;
            } else {
                bytesRemaining = inputStream.available();
                if (bytesRemaining == 0) {
                    bytesRemaining = C.LENGTH_UNSET;
                }
            }
        } catch (IOException e) {
            throw new IOException("Error opening asset: " + uri, e);
        }

        opened = true;
        transferStarted(dataSpec);
        return bytesRemaining;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        if (readLength == 0) {
            return 0;
        } else if (bytesRemaining == 0) {
            return C.RESULT_END_OF_INPUT;
        }

        int bytesRead;
        if (bytesRemaining != C.LENGTH_UNSET) {
            bytesRead = inputStream.read(buffer, offset, (int) Math.min(bytesRemaining, readLength));
        } else {
            bytesRead = inputStream.read(buffer, offset, readLength);
        }

        if (bytesRead == -1) {
            return C.RESULT_END_OF_INPUT;
        }

        if (bytesRemaining != C.LENGTH_UNSET) {
            bytesRemaining -= bytesRead;
        }

        bytesTransferred(bytesRead);
        return bytesRead;
    }

    @Nullable
    @Override
    public Uri getUri() {
        return uri;
    }

    @Override
    public void close() throws IOException {
        uri = null;
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } finally {
            inputStream = null;
            if (opened) {
                opened = false;
                transferEnded();
            }
        }
    }

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        // Assets don't have response headers
        return Collections.emptyMap();
    }
} 