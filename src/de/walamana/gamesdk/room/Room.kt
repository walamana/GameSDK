package de.walamana.gamesdk.room

import de.walamana.gamesdk.event.EventEmitter
import de.walamana.gamesdk.event.OnEvent
import de.walamana.gamesdk.ext.json
import de.walamana.gamesdk.ext.toJSONArray
import de.walamana.gamesdk.player.Player
import de.walamana.gamesdk.server.Connection
import de.walamana.gamesdk.server.GameServer
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.reflect.full.functions

abstract class Room(
    var playerClass: Class<*> = Player::class.java,
    var configuration: JSONObject,
    val server: GameServer
) : EventEmitter(){

    val id = UUID.randomUUID()
    val players = arrayListOf<Player>()

    fun join(connection: Connection){
        val player = playerClass.constructors[0]?.newInstance(connection) as Player? ?: return // TODO: Throw exception
        players.add(player)
        connection.on(Connection.Event.DESTROY.name){
            removePlayer(player)
        }
        connection.send("room joined", json {
            put("id", id)
            put("configuration", configuration)
        })
        call("join", player)
    }

    fun removePlayer(player: Player){
        players.remove(player)
        if(players.size == 0){
            server.rooms.remove(this)
        }
    }

    fun destroy(){

    }

    @OnEvent(event = "players get")
    fun onGetPlayers(player: Player){
        player.send("players", players.toJSONArray())
    }

    fun getPlayer(connection: Connection): Player? = players.find { it.connection.sessionId == connection.sessionId }

    fun broadcast(func: (player: Player) -> Unit){
        this.players.forEach {
            func(it)
        }
    }

    fun call(player: Player, event: String, data: Any?){
        this::class.java.methods.forEach { method ->
            if(method.isAnnotationPresent(OnEvent::class.java)){
                val annotation = method.getAnnotation(OnEvent::class.java)
                val aEvent =
                        if(annotation.event == "")
                            method.name
                                    .removePrefix("on")
                                    .split("(?=\\p{Lu})".toRegex())
                                    .joinToString(" "){ it.toLowerCase() }
                                    .trim()
                        else annotation.event
                if(aEvent == event){
                    when(method.parameterCount){
                        1 -> method.invoke(this, player)
                        2 -> method.invoke(this, player, data)
                    }
                }
            }
        }
    }

    fun call(connection: Connection, event: String, data: Any?) {
        val player = getPlayer(connection) ?: return
        call(player, event, data)
    }

    data class RoomCreatedResult(val connection: Connection, val room: Room)

}