package com.hadihariri.chat

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import org.joda.time.DateTime
import org.wasabifx.wasabi.protocol.websocket.channelHandler
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

val users = CopyOnWriteArrayList<User>()
val rooms = CopyOnWriteArrayList<Room>()

class UserListMessage(val type: String = "CLIENT_LIST", val body: List<String>)
class RoomListMessage(val type: String = "ROOM_LIST", val body: List<Room>)
class RoomPostsMessage(val type: String = "ROOM_POSTS", val body: Room)

data class User(val username: String = "", val context: Channel)
data class Post(val from: String, val body: String)
data class Room(val id: String = DateTime.now().millisOfDay.toString(), val title: String, val posts: CopyOnWriteArrayList<Post> = CopyOnWriteArrayList())


sealed class IncomingSocketMessage {
    class ConnectMessage(val username: String) : IncomingSocketMessage()
    class InvalidIncomingMessage(val message: String) : IncomingSocketMessage()
    class AddRoomMessage(val title: String) : IncomingSocketMessage()
    class GetRoomMessage(val roomId: String) : IncomingSocketMessage()
    class AddPostMessage(val roomId: String, val body: String, val from: String, val to: String) : IncomingSocketMessage()
    class GetPeerMessage(val from: String, val to: String) : IncomingSocketMessage()
}


val chatServer = channelHandler {

    (frame as? TextWebSocketFrame).let {
        val message = parseIncomingMessage(it?.text() ?: "{}")
        when (message) {
            is IncomingSocketMessage.ConnectMessage -> {
                if (users.find { it.username.compareTo(message.username, true) == 0 } == null) {
                    users.add(User(message.username, ctx?.channel()!!))
                }
                broadcastMessage(UserListMessage(body = users.filter { it.username.compareTo(message.username) != 0 }.map { it.username }).toJSON())
                response.frame = TextWebSocketFrame(RoomListMessage(body = rooms).toJSON())
            }
            is IncomingSocketMessage.AddRoomMessage -> {
                if (rooms.find { it.title.compareTo(message.title, true) == 0 } == null) {
                    rooms.add(Room(title = message.title))
                }
                broadcastMessage(RoomListMessage(body = rooms).toJSON())
                response.frame = TextWebSocketFrame("")
            }
            is IncomingSocketMessage.GetRoomMessage -> {
                rooms.firstOrNull { it.id.compareTo(message.roomId) == 0 }?.let {
                    response.frame = TextWebSocketFrame(RoomPostsMessage(body = it).toJSON())
                }
            }
            is IncomingSocketMessage.AddPostMessage -> {
                rooms.firstOrNull { it.id.compareTo(message.roomId) == 0 }?.let {
                    it.posts.add(Post(message.from, message.body))
                    if (message.roomId.contains("#")) {
                        sendMessage(message.from, RoomPostsMessage(body = it).toJSON())
                        sendMessage(message.to, RoomPostsMessage(body = it).toJSON())
                    } else {
                        broadcastMessage(RoomPostsMessage(body = it).toJSON())
                    }
                    response.frame = TextWebSocketFrame("")
                }
            }
            is IncomingSocketMessage.GetPeerMessage -> {
                val roomName = "@${message.from}#${message.to}"
                val roomNameAlt = "@${message.to}#${message.from}"
                val room = rooms.find { it.id.compareTo(roomName) == 0 || it.id.compareTo(roomNameAlt) == 0 }
                if (room != null)
                {
                    response.frame = TextWebSocketFrame(RoomPostsMessage(body = room).toJSON())
                } else {
                    rooms.add(Room(id = roomName, title = message.to))
                    response.frame = TextWebSocketFrame("")
                }
            }
            else -> {
                response.frame = TextWebSocketFrame("ERROR: Invalid Message")
            }
        }
    }

}

fun sendMessage(username: String, message: String) {
    users.find { it.username == username}?.let {
        it.context.writeAndFlush(TextWebSocketFrame(message))
    }
}


fun broadcastMessage(message: String) {
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
            "GET_ROOM" -> return IncomingSocketMessage.GetRoomMessage(node.get("roomId").asText())
            "ADD_POST" -> return IncomingSocketMessage.AddPostMessage(node.get("roomId").asText(), node.get("body").asText(), node.get("from").asText(), node.get("to").asText())
            "GET_PEER" -> return IncomingSocketMessage.GetPeerMessage(node.get("from").asText(), node.get("to").asText())
            else -> IncomingSocketMessage.InvalidIncomingMessage("Invalid Message Type")
        }
    }
    return IncomingSocketMessage.InvalidIncomingMessage("Invalid Message Format")
}


fun Any.toJSON(): String {
    val mapper = ObjectMapper()
    return mapper.writeValueAsString(this)
}