package ani.saikou.tv.utils

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import kotlin.concurrent.thread

object NetworkTVConnection {

    private var _isListening = false
    val isListening: Boolean get() = _isListening

    private var onTokenReceived: OnTokenReceivedCallback? = null
    private var listeningThread: Thread? = null

    fun listen(onTokenReceivedCallback: OnTokenReceivedCallback) {
        this.onTokenReceived = onTokenReceivedCallback
        var server: ServerSocket? = null
        listeningThread = thread(start = true) {
            try {
                _isListening = true
                server = ServerSocket(2413)
                val socket = server?.accept()
                val streamReader = BufferedReader(InputStreamReader(socket.getInputStream()))

                var readToken = ""

                var message: String = streamReader.readLine()
                while (message != null) {
                    readToken +=message
                }
                onTokenReceived?.onTokenReceived(readToken)
            } catch (e: Exception) {
                server?.let {
                    it.close()
                }
                _isListening = false
            }
        }
    }

    fun stopListening() {
        listeningThread?.let {
            it.interrupt()
        }
    }

    interface OnTokenReceivedCallback {
        fun onTokenReceived(token: String)
    }
}