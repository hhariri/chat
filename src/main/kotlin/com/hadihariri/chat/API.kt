package com.hadihariri.chat

import org.joda.time.DateTimeUtils
import org.wasabifx.wasabi.routing.routeHandler
import java.util.*

/**
 * Created by hadihariri on 03/09/16.
 */
val clientListRoute = routeHandler {
    if (request.routeParams["id"] == "") {
        response.send(connections.toList(), "application/json")
    } else {
        try {
            response.send(connections.first { it.name.toUpperCase() == request.routeParams["id"]?.toUpperCase() }, "application/json")
        } catch (e: NoSuchElementException) {
            response.statusCode = 404
            response.statusDescription = "Client not found"
        }
    }
}

val clientCreateRoute = routeHandler {
    val name = request.bodyParams["name"]
    if (name != null) {
        if (connections.firstOrNull { it.name.toUpperCase() == name.toString().toUpperCase() } == null) {
            connections.add(Client(name.toString(), DateTimeUtils.getInstantMillis(null)))
            response.statusCode = 201
            response.statusDescription = "Client created"
        } else {
            response.statusCode = 409
            response.statusDescription = "Conflicting name"
        }
    } else {
        response.statusCode = 400
        response.statusDescription = "Name is required"
    }
}

val clientDeleteRoute = routeHandler {
    val client = connections.firstOrNull() { it.name.toUpperCase() == request.routeParams["id"]?.toUpperCase()}
    if (client != null) {
        connections.remove(client)
        response.statusCode = 200
        response.statusDescription = "Deleted"
    } else {
        response.statusCode = 404
        response.statusDescription = "Client not found"
    }
}

