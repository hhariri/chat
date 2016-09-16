package com.hadihariri.chat

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import org.wasabifx.wasabi.protocol.websocket.channelHandler
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

val clientList = CopyOnWriteArrayList<ClientData>()

class ClientListMessage(val type: String = "CLIENT_LIST", val body: List<String>)

data class ClientData(val username: String = "", val connection: ChannelHandlerContext)

sealed class IncomingSocketMessage {
    class ConnectMessage(val username: String) : IncomingSocketMessage()
    class InvalidIncomingMessage(val message: String) : IncomingSocketMessage()
    class AddRoomMessage(val room: String) : IncomingSocketMessage()
    class GetRoomMessage(val room: String) : IncomingSocketMessage()
    class AddPostMessage(val post: String) : IncomingSocketMessage()
}



val chatServer = channelHandler {

    (frame as? TextWebSocketFrame).let {
        val message = parseIncomingMessage(it?.text() ?: "{}")
        when (message) {
            is IncomingSocketMessage.ConnectMessage -> {
                clientList.add(ClientData(message.username, ctx!!))
                // TODO: This needs to be broadcasted
                response.frame = TextWebSocketFrame(ClientListMessage(body = clientList.map { it.username }).toJSON())
            }
        }
    }

}


fun parseIncomingMessage(messageText: String): IncomingSocketMessage {
    val node = ObjectMapper().readValue(messageText, ObjectNode::class.java)
    if (node.has("type")) {
        val type = node.get("type").asText()
        when (type) {
            "CONNECT" -> return IncomingSocketMessage.ConnectMessage(node.get("username").asText())
            else -> IncomingSocketMessage.InvalidIncomingMessage("Invalid Message Type")
        }
    }
    return IncomingSocketMessage.InvalidIncomingMessage("Invalid Message Format")
}


fun Any.toJSON(): String {
    val mapper = ObjectMapper()
    return mapper.writeValueAsString(this)
}