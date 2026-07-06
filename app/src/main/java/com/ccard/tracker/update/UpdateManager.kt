package com.ccard.tracker.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 새 APK를 다운로드하고 시스템 설치 화면을 띄운다. 스토어 배포가 아닌 사이드로드 앱이므로
 * "출처를 알 수 없는 앱 설치" 권한(REQUEST_INSTALL_PACKAGES)이 필요하다.
 *
 * 처음엔 DownloadManager + ACTION_DOWNLOAD_COMPLETE 브로드캐스트로 구현했으나, 일부 제조사
 * 롬(특히 삼성 OneUI)에서 백그라운드 브로드캐스트가 지연·소실되어 "다운로드는 되는데 설치
 * 화면이 안 뜬다"는 문제가 있었다. 그래서 앱이 직접 HTTP로 내려받고, 완료되는 즉시(같은
 * 코루틴 안에서) 설치 인텐트를 띄우는 방식으로 바꿨다 — 시스템 브로드캐스트에 의존하지 않는다.
 */
class UpdateManager(private val context: Context) {

    private val scope = MainScope()

    fun hasInstallPermission(): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun installPermissionSettingsIntent(): Intent =
        Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    fun downloadAndInstall(info: UpdateInfo) {
        scope.launch {
            Toast.makeText(context, "v${info.versionName} 다운로드 중...", Toast.LENGTH_SHORT).show()
            val result = runCatching { download(info) }
            result.onSuccess { file ->
                installApk(file)
            }.onFailure { error ->
                Toast.makeText(context, "업데이트 다운로드 실패: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun download(info: UpdateInfo): File = withContext(Dispatchers.IO) {
        val destinationDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val destination = File(destinationDir, "ccard-${info.versionCode}.apk")
        if (destination.exists()) destination.delete()

        val connection = URL(info.downloadUrl).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        try {
            connection.connect()
            if (connection.responseCode !in 200..299) {
                error("HTTP ${connection.responseCode}")
            }
            connection.inputStream.use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
        } finally {
            connection.disconnect()
        }
        destination
    }

    private fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
