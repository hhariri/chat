package com.hadihariri.chat

import org.wasabifx.wasabi.app.AppServer
import org.wasabifx.wasabi.interceptors.enableCORSGlobally
import org.wasabifx.wasabi.interceptors.enableContentNegotiation
import org.wasabifx.wasabi.interceptors.serveStaticFilesFromFolder

fun main(args: Array<String>) {
    val server = AppServer()
    server.enableContentNegotiation()
    server.enableCORSGlobally()
    server.serveStaticFilesFromFolder("public")
    server.get("/index", indexRoute)
    server.channel("/chat", chatServer)
    println("Starting server")
    server.start(true)
}


