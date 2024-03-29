package org.tyndalebt.storyproduceradv.model

import android.util.Log
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.sql.Timestamp
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import org.json.JSONException
import org.tyndalebt.storyproduceradv.model.messaging.Message
import org.tyndalebt.storyproduceradv.model.messaging.Approval
import org.tyndalebt.storyproduceradv.model.messaging.MessageROCC
import java.net.URI
import java.nio.ByteBuffer

class MessageWebSocketClient(uri: URI) : WebSocketClient(uri) {

    private lateinit var messageReceiveChannel: ReceiveChannel<Message>

    override fun onOpen(handshakeData: ServerHandshake) {
        Log.e("@pwhite", "opened web-socket; starting queue listener")
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
    }

    override fun onMessage(message: ByteBuffer) {
    }

    override fun onMessage(messageString: String?) {

        try {
            val message = JSONObject(messageString)
            val type = message.getString("type")
            val slideNumber = message.getInt("slideNumber")
            val storyId = message.getInt("storyId")
            val timeSent = Timestamp.valueOf(message.getString("timeSent"))

            if (type == "text") {
                Log.e("@pwhite", "got message: ${messageString!!}")
                val isConsultant = message.getBoolean("isConsultant")
                val isTranscript = message.getBoolean("isTranscript")
                val text = message.getString("text")
                GlobalScope.launch {
                    Workspace.messageChannel.send(
                        MessageROCC(slideNumber, storyId, isConsultant, isTranscript, timeSent, text)
                    )
                }
            } else if (type == "approval") {
                val approvalStatus = message.getBoolean("approvalStatus")
                GlobalScope.launch {
                    Workspace.approvalChannel.send(Approval(slideNumber, storyId, timeSent, approvalStatus))
                }
            } else {
                Log.e("@pwhite", "message was of an unexpected type: $type. ignoring it.")
            }
        } catch (e: JSONException) {
            Log.e("@pwhite", "message was corrupted: $e. ignoring.")
        }
    }

    override fun onError(ex: Exception) {
        Log.e("@pwhite", "Error on websocket: ${ex.message} ")
    }
}
