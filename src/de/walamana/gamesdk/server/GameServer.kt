package de.walamana.gamesdk.server

import de.walamana.gamesdk.event.EventEmitter
import de.walamana.gamesdk.ext.getJSONObjectOrNull
import de.walamana.gamesdk.ext.getOrNull
import de.walamana.gamesdk.ext.getStringOrNull
import de.walamana.gamesdk.player.Player
import de.walamana.gamesdk.room.Room
import io.javalin.Javalin
import io.javalin.websocket.WsCloseContext
import io.javalin.websocket.WsConnectContext
import io.javalin.websocket.WsHandler
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventException
import java.lang.Exception
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

// TODO: Add Java constructor
open class GameServer (func: GameServer.() -> Unit = {}) : EventEmitter(){

    val app = Javalin.create().start(4567)

    var socketPath: String = "/socket"

    var connections = ConcurrentLinkedQueue<Connection>();

    var roomClass: Class<*> = Room::class.java
    val rooms = arrayListOf<Room>()

    val all = EventEmitter()


    init {
        app.ws(socketPath) {
            it.onConnect{ ctx -> onConnect(ctx) }
            it.onClose { ctx -> onClose(ctx) }
            it.onMessage { ctx -> onMessage(ctx.session, ctx.message()) }
        }
        func()
        setupEvents()
        println("Server Started")
    }

    /**
     * Sends a message to all connected clients
     */
    fun send(event: String, data: Any? = null){
        connections.forEach { it.send(event, data, Connection.Context.ALL) }
    }

    fun onRoomCreated(func: (room: Room.RoomCreatedResult) -> Unit) = on("room created", func)

    fun getRoom(id: String): Room? = rooms.find { it.id.toString() == id }

    private fun setupEvents(){
        on<Connection>("connection", { con ->
            con.on<JSONObject>("room create"){ configuration ->
                try{
                    // FIXME: JAVA: inner classes require parent object as first constructor parameter
                    val room = roomClass.getConstructor(JSONObject::class.java, GameServer::class.java).apply {
                        isAccessible = true
                    }.newInstance(configuration, this@GameServer) as Room
                    rooms.add(room)
                    call("room created", Room.RoomCreatedResult(con, room))
                    con.send("room created", room.id)
                }catch(e: Exception){
                    e.printStackTrace()
                }
            }
            con.on<String>("room join"){ id ->
                rooms.find { it.id.toString() == id }?.join(con)
            }
        }, priority = 1)
    }

    private fun onConnect(ctx: WsConnectContext){
        ctx.session.remote.sendString(JSONObject().apply {
            put("event", "register")
        }.toString())
    }

    private fun onClose(ctx: WsCloseContext){
        connections.find { it.session == ctx.session }?.also {
            it.startTimeout()
        }
    }

    private fun onMessage(session: Session, message: String){
        try{
            val packet = JSONObject(message)
            val context = packet.getString("context").split(":")
            val data = packet.getOrNull("data")
            val sessionId = packet.getStringOrNull("session_id")

            when(val event = packet.getString("event")){
                "register" -> registerSession(session, sessionId)
                else -> {
                    connections.find {
                        it.sessionId == sessionId
                    }?.also { con ->
                        when(Connection.Context.valueOf(context[0])){
                            Connection.Context.ALL -> con.callAny(event, data)
                            Connection.Context.ROOM -> {
                                getRoom(context[1])?.also { room ->
                                    room.call(con, event, data)
                                }
                            }
                        }
                    }
                }
            }
        }catch(ex: JSONException){
            System.err.println("Invalid websocket message $message")
            ex.printStackTrace()
            session.disconnect()
        }
    }



    private fun registerSession(session: Session, sessionId: String?){
        if(sessionId == null || sessionId.isEmpty()){
            createConnection(session)
            return
        }
        connections.find { it.sessionId == sessionId }?.reconnect(session) ?: createConnection(session)
    }

    private fun createConnection(session: Session){
        val con = Connection(this@GameServer, UUID.randomUUID().toString(), session)
        connections.add(con)
        call("connection", con)
        con.send("registered", context = Connection.Context.ALL)
    }
}