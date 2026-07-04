package com.ccard.tracker.update

import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * GitHub Releases의 "latest" 릴리즈를 조회해 현재 설치된 버전보다 새 APK가 있는지 확인한다.
 * 릴리즈 태그는 CI 워크플로(.github/workflows/release.yml)가 "v<runNumber>" 형식으로 만든다.
 */
object UpdateChecker {
    private const val OWNER = "yuchoi-bb"
    private const val REPO = "CCard"
    private const val API_URL = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

    fun checkForUpdate(currentVersionCode: Int): UpdateInfo? {
        val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = 10_000
            readTimeout = 10_000
        }

        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tagName = json.optString("tag_name")
            val remoteVersionCode = tagName.removePrefix("v").toIntOrNull() ?: return null
            if (remoteVersionCode <= currentVersionCode) return null

            val assets = json.optJSONArray("assets") ?: return null
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name").endsWith(".apk")) {
                    apkUrl = asset.optString("browser_download_url")
                    break
                }
            }
            val downloadUrl = apkUrl ?: return null

            UpdateInfo(
                versionCode = remoteVersionCode,
                versionName = json.optString("name").ifBlank { tagName },
                downloadUrl = downloadUrl,
                releaseNotes = json.optString("body"),
            )
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
