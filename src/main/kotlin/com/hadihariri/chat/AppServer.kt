package com.hadihariri.chat

import org.joda.time.DateTimeUtils
import org.wasabifx.wasabi.app.AppServer
import org.wasabifx.wasabi.interceptors.enableCORSGlobally
import org.wasabifx.wasabi.interceptors.enableContentNegotiation
import org.wasabifx.wasabi.interceptors.serveStaticFilesFromFolder
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by hadihariri on 28/08/16.
 */

data class Client(val name: String = "", val timestamp: Long = DateTimeUtils.getInstantMillis(null))

val connections = CopyOnWriteArrayList<Client>()

fun main(args: Array<String>) {


    val server = AppServer()

    server.enableContentNegotiation()
    server.enableCORSGlobally()
    server.serveStaticFilesFromFolder("public")


    server.get("/index", indexRoute)
    server.get("/client/:id", clientListRoute)
    server.post("/client", clientCreateRoute)
    server.delete("/client/:id", clientDeleteRoute)

    server.channel("/chat", chatServer)

    connections.add(Client("hadi"))
    connections.add(Client("joe"))

    println("Starting server")
    server.start(true)


}


