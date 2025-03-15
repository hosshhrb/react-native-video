package com.brentvatne.common.api

import android.net.Uri
import android.text.TextUtils
import com.brentvatne.common.toolbox.ReactBridgeUtils
import com.facebook.react.bridge.ReadableMap

class AdsProps {
    var adTagUrl: Uri? = null
    var adLanguage: String? = null
    var localAdPath: String? = null

    /** return true if this and src are equals  */
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is AdsProps) return false
        return (
            adTagUrl == other.adTagUrl &&
                adLanguage == other.adLanguage &&
                localAdPath == other.localAdPath
            )
    }

    companion object {
        private const val PROP_AD_TAG_URL = "adTagUrl"
        private const val PROP_AD_LANGUAGE = "adLanguage"
        private const val PROP_LOCAL_AD_PATH = "localAdPath"

        @JvmStatic
        fun parse(src: ReadableMap?): AdsProps {
            val adsProps = AdsProps()
            if (src != null) {
                val uriString = ReactBridgeUtils.safeGetString(src, PROP_AD_TAG_URL)
                if (TextUtils.isEmpty(uriString)) {
                    adsProps.adTagUrl = null
                } else {
                    adsProps.adTagUrl = Uri.parse(uriString)
                }
                val languageString = ReactBridgeUtils.safeGetString(src, PROP_AD_LANGUAGE)
                if (!TextUtils.isEmpty(languageString)) {
                    adsProps.adLanguage = languageString
                }
                val localAdPathString = ReactBridgeUtils.safeGetString(src, PROP_LOCAL_AD_PATH)
                if (!TextUtils.isEmpty(localAdPathString)) {
                    adsProps.localAdPath = localAdPathString
                }
            }
            return adsProps
        }
    }
}
