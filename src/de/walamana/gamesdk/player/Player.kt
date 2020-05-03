package de.walamana.gamesdk.player

import de.walamana.gamesdk.server.Connection
import de.walamana.gamesdk.server.Parsable
import org.json.JSONObject

open class Player(
    var connection: Connection
) : Parsable{

    var name: String = RandomNameGenerator.next()

    fun send(event: String, data: Any? = null){
        connection.send(event, data, Connection.Context.ROOM)
    }

    override fun toJSON(): JSONObject = JSONObject().apply {
        put("name", name)
    }

}