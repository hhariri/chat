package com.hadihariri.chat

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import org.wasabifx.wasabi.protocol.websocket.channelHandler
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

val users = CopyOnWriteArrayList<User>()
val rooms = CopyOnWriteArrayList<Room>()

class UserListMessage(val type: String = "CLIENT_LIST", val body: List<String>)

data class User(val username: String = "", val context: Channel)
data class Post(val from: String, val body: String)
data class Room(val title: String, val posts: List<Post>)


sealed class IncomingSocketMessage {
    class ConnectMessage(val username: String) : IncomingSocketMessage()
    class InvalidIncomingMessage(val message: String) : IncomingSocketMessage()
    class AddRoomMessage(val title: String) : IncomingSocketMessage()
    class GetRoomMessage(val room: String) : IncomingSocketMessage()
    class AddPostMessage(val post: String) : IncomingSocketMessage()
}



val chatServer = channelHandler {

    (frame as? TextWebSocketFrame).let {
        val message = parseIncomingMessage(it?.text() ?: "{}")
        when (message) {
            is IncomingSocketMessage.ConnectMessage -> {
                users.firstOrNull() { it.username.compareTo(message.username, true) == 0 }?.let {
                    users.add(User(message.username, ctx?.channel()!!))
                }
                broadcastMessage(UserListMessage(body = users.map { it.username }).toJSON())
                response.frame = TextWebSocketFrame("")
            }
        }
    }

}

fun  broadcastMessage(message: String) {
    for ((username, context) in users) {
        context.writeAndFlush(TextWebSocketFrame(message))
    }
}


fun parseIncomingMessage(messageText: String): IncomingSocketMessage {
    val node = ObjectMapper().readValue(messageText, ObjectNode::class.java)
    if (node.has("type")) {
        val type = node.get("type").asText()
        when (type) {
            "CONNECT" -> return IncomingSocketMessage.ConnectMessage(node.get("username").asText())
            "ADD_ROOM" -> return IncomingSocketMessage.AddRoomMessage(node.get("title").asText())
            "GET_ROOM" -> return IncomingSocketMessage.GetRoomMessage(node.get("title").asText())
            "ADD_POST" -> return IncomingSocketMessage.AddPostMessage(node.get("title").asText())
            else -> IncomingSocketMessage.InvalidIncomingMessage("Invalid Message Type")
        }
    }
    return IncomingSocketMessage.InvalidIncomingMessage("Invalid Message Format")
}


fun Any.toJSON(): String {
    val mapper = ObjectMapper()
    return mapper.writeValueAsString(this)
}