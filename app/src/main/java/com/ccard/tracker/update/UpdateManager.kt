package com.ccard.tracker.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

/**
 * 새 APK를 다운로드하고 시스템 설치 화면을 띄운다. 스토어 배포가 아닌 사이드로드 앱이므로
 * "출처를 알 수 없는 앱 설치" 권한(REQUEST_INSTALL_PACKAGES)이 필요하다.
 */
class UpdateManager(private val context: Context) {

    fun hasInstallPermission(): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun installPermissionSettingsIntent(): Intent =
        Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    fun downloadAndInstall(info: UpdateInfo) {
        val fileName = "ccard-${info.versionCode}.apk"
        val destinationDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val destination = File(destinationDir, fileName)
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(info.downloadUrl))
            .setTitle("CCard 업데이트")
            .setDescription("v${info.versionName} 다운로드 중")
            .setDestinationUri(Uri.fromFile(destination))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (completedId == downloadId) {
                    receiverContext.unregisterReceiver(this)
                    installApk(destination)
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
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
