package com.hadihariri.chat

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.hadihariri.chat.IncomingSocketMessage.*
import io.netty.channel.Channel
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import org.joda.time.DateTime
import org.wasabifx.wasabi.protocol.websocket.channelHandler
import java.util.concurrent.CopyOnWriteArrayList

val users = CopyOnWriteArrayList<User>()
val rooms = CopyOnWriteArrayList<Room>()

class UserListMessage(val type: String = "CLIENT_LIST", val body: List<String>)
class RoomListMessage(val type: String = "ROOM_LIST", val body: List<Room>)
class RoomPostsMessage(val type: String = "ROOM_POSTS", val body: Room)

data class User(val username: String, val context: Channel)
data class Post(val from: String, val body: String)
data class Room(
        val id: String = DateTime.now().millisOfDay.toString(),
        val title: String,
        val posts: CopyOnWriteArrayList<Post> = CopyOnWriteArrayList()
)

sealed class IncomingSocketMessage {
    class ConnectMessage(val username: String) : IncomingSocketMessage()
    class InvalidIncomingMessage(val message: String) : IncomingSocketMessage()
    class AddRoomMessage(val title: String) : IncomingSocketMessage()
    class GetRoomMessage(val roomId: String) : IncomingSocketMessage()
    class AddPostMessage(val roomId: String, val body: String, val from: String, val to: String) : IncomingSocketMessage()
    class GetPeerMessage(val from: String, val to: String) : IncomingSocketMessage()
}

val chatServer = channelHandler {

    val message = parseIncomingMessage((frame as? TextWebSocketFrame)?.text() ?: "{}")
    when (message) {
        is ConnectMessage -> {
            if (users.none { it.username equalsIgnoreCase message.username }) {
                users.add(User(message.username, ctx?.channel()!!))
            }
            broadcastMessage(UserListMessage(body = users.map { it.username }).toJSON())
            response.frame = TextWebSocketFrame(RoomListMessage(body = rooms).toJSON())
        }
        is AddRoomMessage -> {
            if (rooms.none { it.title equalsIgnoreCase message.title }) {
                rooms.add(Room(title = message.title))
            }
            broadcastMessage(RoomListMessage(body = rooms).toJSON())
            response.frame = TextWebSocketFrame("")
        }
        is GetRoomMessage -> {
            rooms.firstOrNull { it.id equalsIgnoreCase message.roomId }?.let {
                response.frame = TextWebSocketFrame(RoomPostsMessage(body = it).toJSON())
            }
        }
        is AddPostMessage -> {
            rooms.firstOrNull { it.id equalsIgnoreCase message.roomId }?.let {
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
        is GetPeerMessage -> {
            val roomName = "@${message.from}#${message.to}"
            val roomNameAlt = "@${message.to}#${message.from}"
            val room = rooms.find { it.id == roomName || it.id == roomNameAlt }
            if (room != null) {
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

fun sendMessage(username: String, message: String) {
    users.find { it.username == username }?.let {
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
    if (!node.has("type")) return InvalidIncomingMessage("Invalid Message Format")

    val type = node.get("type").asText()
    return when (type) {
        "CONNECT" -> ConnectMessage(node["username"].asText())
        "ADD_ROOM" -> AddRoomMessage(node["title"].asText())
        "GET_ROOM" -> GetRoomMessage(node["roomId"].asText())
        "ADD_POST" -> AddPostMessage(node["roomId"].asText(),
                node["body"].asText(), node["from"].asText(), node["to"].asText())
        "GET_PEER" -> GetPeerMessage(node["from"].asText(), node["to"].asText())
        else -> InvalidIncomingMessage("Invalid Message Type")
    }
}


fun Any.toJSON(): String {
    val mapper = ObjectMapper()
    return mapper.writeValueAsString(this)
}