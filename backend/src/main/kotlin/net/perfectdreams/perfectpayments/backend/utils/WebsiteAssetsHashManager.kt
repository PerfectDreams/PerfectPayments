package net.perfectdreams.perfectpayments.backend.utils

import org.apache.commons.codec.digest.DigestUtils
import java.util.concurrent.ConcurrentHashMap

class WebsiteAssetsHashManager {
    val websiteFileHashes = ConcurrentHashMap<String, String>()

    /**
     * Generates a MD5 hexadecimal hash for the file in the [assetName] path, useful for cache busting
     *
     * After the first hash generation, the hash will be cached
     *
     * @param assetName the file path
     * @return the asset content hashed in MD5
     */
    fun getAssetHash(assetName: String): String {
        return if (websiteFileHashes.containsKey(assetName)) {
            websiteFileHashes[assetName]!!
        } else {
            val md5 = DigestUtils.md5Hex(WebsiteAssetsHashManager::class.java.getResourceAsStream("/static/${assetName.removePrefix("/")}"))
            websiteFileHashes[assetName] = md5
            md5
        }
    }
}