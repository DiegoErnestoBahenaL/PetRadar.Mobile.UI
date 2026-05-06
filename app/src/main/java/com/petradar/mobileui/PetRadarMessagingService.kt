package com.petradar.mobileui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.petradar.mobileui.api.models.UpdateFcmTokenRequest
import com.petradar.mobileui.repository.UserRepository
import com.petradar.mobileui.utils.AuthManager
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class PetRadarMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        Log.d(TAG, "From: ${remoteMessage.from}, data: $data")

        if (data.containsKey(ChatActivity.EXTRA_OTHER_USER_ID)) {
            val title = remoteMessage.notification?.title ?: getString(R.string.app_name)
            val body = remoteMessage.notification?.body ?: return
            sendChatNotification(title, body, data)
            return
        }

        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            it.body?.let { body -> sendNotification(body) }
        }
    }

    // [START on_new_token]
    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        serviceScope.launch {
            sendRegistrationToServer(token)
        }
    }
    // [END on_new_token]

    private suspend fun sendRegistrationToServer(token: String?) {
        if (token.isNullOrEmpty()) return

        val userId = AuthManager.getUserId(applicationContext)
        if (userId == null) {
            // Expected on first install: Firebase mints a token before the user logs in.
            // The login flow will call syncFcmTokenForCurrentUser() once a userId exists.
            Log.d(TAG, "sendRegistrationTokenToServer: no authenticated user yet, skipping")
            return
        }

        registerToken(userId, token)
    }

    private fun sendChatNotification(title: String, body: String, data: Map<String, String>) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data[ChatActivity.EXTRA_ADOPTION_ANIMAL_ID]?.toLongOrNull()?.let { putExtra(ChatActivity.EXTRA_ADOPTION_ANIMAL_ID, it) }
            data[ChatActivity.EXTRA_MATCH_ID]?.toLongOrNull()?.let { putExtra(ChatActivity.EXTRA_MATCH_ID, it) }
            data[ChatActivity.EXTRA_OTHER_USER_ID]?.toLongOrNull()?.let { putExtra(ChatActivity.EXTRA_OTHER_USER_ID, it) }
            data[ChatActivity.EXTRA_ANIMAL_NAME]?.let { putExtra(ChatActivity.EXTRA_ANIMAL_NAME, it) }
            data[ChatActivity.EXTRA_OTHER_USER_NAME]?.let { putExtra(ChatActivity.EXTRA_OTHER_USER_NAME, it) }
            data[ChatActivity.EXTRA_STRAY_REPORT_ID]?.toLongOrNull()?.let { putExtra(ChatActivity.EXTRA_STRAY_REPORT_ID, it) }
            data[ChatActivity.EXTRA_LOST_PET_LABEL]?.let { putExtra(ChatActivity.EXTRA_LOST_PET_LABEL, it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.petradar_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private fun sendNotification(messageBody: String) {
        val requestCode = 0
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.petradar_logo)
            .setContentTitle(getString(R.string.fcm_message))
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = 0
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {

        private const val TAG = "PetRadarFirebaseMsgService"

        /**
         * Fetches the current FCM token from Firebase and sends it to the server for the
         * currently authenticated user. Call this from the login success path so the token
         * is registered even when [onNewToken] was fired before the user logged in.
         *
         * No-op if no user is logged in.
         */
        suspend fun syncFcmTokenForCurrentUser(context: Context) {
            val userId = AuthManager.getUserId(context)
            if (userId == null) {
                Log.d(TAG, "syncFcmTokenForCurrentUser: no authenticated user, skipping")
                return
            }

            val token = try {
                suspendCancellableCoroutine<String?> { cont ->
                    FirebaseMessaging.getInstance().token
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "syncFcmTokenForCurrentUser: getToken failed", e)
                            cont.resume(null)
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "syncFcmTokenForCurrentUser: getToken error", e)
                null
            }

            if (token.isNullOrEmpty()) {
                Log.w(TAG, "syncFcmTokenForCurrentUser: empty token, nothing to send")
                return
            }

            registerToken(userId, token)
        }

        private suspend fun registerToken(userId: Long, token: String) {
            try {
                val response = UserRepository().updateFcmToken(userId, UpdateFcmTokenRequest(token))
                if (response.isSuccessful) {
                    Log.d(TAG, "registerToken: success (userId=$userId)")
                } else {
                    Log.e(TAG, "registerToken: failed code=${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "registerToken: error", e)
            }
        }
    }
}