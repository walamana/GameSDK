package de.walamana.gamesdk.server

import de.walamana.gamesdk.event.EventEmitter
import de.walamana.gamesdk.room.Room
import org.eclipse.jetty.websocket.api.Session
import org.json.JSONObject
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class Connection(
    val server: GameServer,
    val sessionId: String,
    var session: Session?
) : EventEmitter(){

    companion object{
        const val TIMEOUT = 10*1000L
    }

    val scheduler = Executors.newSingleThreadScheduledExecutor()
    var timeout: ScheduledFuture<*>? = null

    var mainContext: Context = Context.ALL
    
    var room: Room? = null


    fun send(event: String, data: Any? = null, context: Context = mainContext){
        session?.also {
            if(it.isOpen && timeout == null){
                it.remote.sendString(JSONObject().apply {
                    put("event", event)
                    put("context", context.name)
                    put("session_id", sessionId)
                    put("data", data)
                }.toString())
            }
        }
    }

    fun reconnect(session: Session){
        if(timeout != null){
            timeout!!.cancel(true)
            timeout = null
        }
        this.session = session
        call(Event.RECONNECT.name)
        send("reconnect", Context.ALL)
        // TODO: Implement
    }

    fun startTimeout(){
        println("Starting timeout...")
        call(Event.DISCONNECT.name)
        timeout = scheduler.postDelayed(TIMEOUT){
            destroy()
        }
    }

    fun destroy(){
        // TODO: Deletion
        server.connections.remove(this)
        call(Event.DESTROY.name)
    }

    // TODO: add ping to prevent zombie connections

    private fun ScheduledExecutorService.postDelayed(duration: Long, func: () -> Unit): ScheduledFuture<*> =
        schedule(func, duration, TimeUnit.MILLISECONDS)

    enum class Context{
        ALL,
        ROOM,
    }
    enum class Event{
        RECONNECT,
        DISCONNECT,
        DESTROY;
    }
}