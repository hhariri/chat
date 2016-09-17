package com.hadihariri.chat

import org.wasabifx.wasabi.app.AppServer
import org.wasabifx.wasabi.interceptors.enableCORSGlobally
import org.wasabifx.wasabi.interceptors.enableContentNegotiation
import org.wasabifx.wasabi.interceptors.serveStaticFilesFromFolder
import java.util.concurrent.CopyOnWriteArrayList

fun main(args: Array<String>) {
    val server = AppServer()
    server.enableContentNegotiation()
    server.enableCORSGlobally()
    server.serveStaticFilesFromFolder("public")
    server.get("/index", indexRoute)
    server.channel("/chat", chatServer)
    val posts = CopyOnWriteArrayList<Post>()
    posts.add(Post("hadi", "This is a first message"))
    rooms.add(Room(title = "General", posts = posts))
    println("Starting server")
    server.start(true)
}


