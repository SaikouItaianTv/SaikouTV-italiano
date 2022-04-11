package ani.saikou.tv.login

import android.app.Activity
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import ani.saikou.anilist.Anilist
import ani.saikou.toastString
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*

object NearbyTVConnection {

    val SERVICE_ID = "SaikouTV"
    val TV_NAME = "TV"
    val PHONE_NAME = "PHONE"

    private var _advertising: Boolean = false
    val isAdvertising: Boolean get() = _advertising

    fun startAdvertising(activity: Activity) {
        val token = Anilist.token ?: return
        if (isAdvertising) return else _advertising = true
        val advertisingOptions: AdvertisingOptions = AdvertisingOptions.Builder().setStrategy(
            Strategy.P2P_POINT_TO_POINT
        ).build()
        Nearby.getConnectionsClient(activity).startAdvertising(
            PHONE_NAME.toByteArray(),
            SERVICE_ID,
            object : ConnectionLifecycleCallback() {
                override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
                    AlertDialog.Builder(activity)
                        .setTitle("Accept connection to " + p1.endpointName)
                        .setMessage("Confirm the code matches on both devices: " + p1.authenticationDigits)
                        .setPositiveButton(
                            "Accept"
                        ) { dialog: DialogInterface?, which: Int ->  // The user confirmed, so we can accept the connection.
                            Nearby.getConnectionsClient(activity)
                                .acceptConnection(p0, object : PayloadCallback() {
                                    override fun onPayloadReceived(p0: String, p1: Payload) {}
                                    override fun onPayloadTransferUpdate(
                                        p0: String,
                                        p1: PayloadTransferUpdate
                                    ) {
                                    }
                                })
                        }
                        .setNegativeButton(
                            "Cancel"
                        ) { dialog: DialogInterface?, which: Int ->  // The user canceled, so we should reject the connection.
                            Nearby.getConnectionsClient(activity).rejectConnection(p0)
                        }
                        .show()
                }

                override fun onConnectionResult(p0: String, p1: ConnectionResolution) {
                    when (p1.status.statusCode) {
                        ConnectionsStatusCodes.STATUS_OK -> { // We are connected, send token to TV
                            val bytesPayload =
                                Payload.fromBytes(token.toByteArray())
                            Nearby.getConnectionsClient(activity)
                                .sendPayload(p0, bytesPayload)
                        }
                        ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                            toastString("TVConnection rejected")
                        }
                        ConnectionsStatusCodes.STATUS_ERROR -> {
                            toastString("TVConnection error")
                        }
                        else -> {
                        }
                    }
                }

                override fun onDisconnected(p0: String) {
                }
            },
            advertisingOptions
        )
            .addOnSuccessListener {
                toastString("Advertising OK")
            }
            .addOnFailureListener { e: Exception? ->
                toastString("Advertising KO")
            }
    }

    fun stopAdvertising(activity: Activity) {
        Nearby.getConnectionsClient(activity).stopAdvertising()
        _advertising = false
    }
}