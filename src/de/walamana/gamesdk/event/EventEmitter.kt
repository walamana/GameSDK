package de.walamana.gamesdk.event

import de.walamana.gamesdk.server.Connection
import de.walamana.gamesdk.server.GameServer
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.Integer.max
import java.math.BigDecimal
import java.math.BigInteger

open class EventEmitter{
    val eventHandlers = arrayListOf<EventHandler<*>>()

    init {
        this::class.java.methods.forEach { method ->
            if(method.isAnnotationPresent(OnEvent::class.java)){
                val annotation = method.getAnnotation(OnEvent::class.java)
                val event = if(annotation.event == "")
                    method.name.removePrefix("on").split("(?=\\p{Lu})".toRegex()).joinToString(" "){ it.toLowerCase() }.trim()
                else annotation.event
                on<Any>(event){
                    method.invoke(this, it)
                }
            }
        }
    }

    inline fun <reified T> on(
        event: String,
        noinline func: (data: T) -> Unit,
        context: Connection.Context = Connection.Context.ALL,
        priority: Int = 0): EventHandler<T> {

//        println("Type: " + T::class + " on \"$event\"")

        return EventHandler(event, T::class.java, func, context).also {
            when {
                priority < 0 -> eventHandlers.add(0, it)
                priority > 0 -> eventHandlers.add(max(eventHandlers.size - 1, 0), it)
                else -> eventHandlers.add(it)
            }
        }
    }

    inline fun <reified T> on(
            event: String,
            noinline func: (data: T) -> Unit,
            context: Connection.Context = Connection.Context.ALL
    ): EventHandler<T> = on(event, func, context, 0)

    inline fun <reified T> on(
            event: String,
            noinline func: (data: T) -> Unit
    ): EventHandler<T> = on(event, func, context = Connection.Context.ALL)

    fun on(
            event: String,
            emptyFunc: () -> Unit,
            context: Connection.Context = Connection.Context.ALL
    ): EventHandler<Any?> = on(event, func = { emptyFunc() }, context = context)

    fun on(
            event: String,
            emptyFunc: () -> Unit
    ): EventHandler<Any?> = on(event, func = { emptyFunc() }, context = Connection.Context.ALL)

    inline fun <reified T> once(
            event: String,
            noinline func: (data: T) -> Unit,
            context: Connection.Context = Connection.Context.ALL
    ): EventHandler<T> = on("#once_$event", func, context = context)

    fun once(
            event: String,
            emptyFunc: () -> Unit,
            context: Connection.Context = Connection.Context.ALL
    ): EventHandler<Any?> = once(event, func = { emptyFunc() }, context = context)

    fun once(
            event: String,
            emptyFunc: () -> Unit
    ): EventHandler<Any?> = once(event, func = { emptyFunc() }, context = Connection.Context.ALL)

    fun <T> removeEventHandler(eventHandler: EventHandler<T>){
        eventHandlers.remove(eventHandler)
    }

    fun removeEventHandlers(event: String){
        eventHandlers.removeIf { it.event == event }
    }

    fun dropEventHandlers(){
        eventHandlers.clear()
    }

    inline fun <reified T> call(event: String, data: T, context: Connection.Context = Connection.Context.ALL){
        eventHandlers
            .filter { (it.event == event || it.event == "#once_$event") && it.context == context }
            .mapNotNull {
                if(it.dataClass == Any::class.java || it.dataClass == T::class.java)
                    it
                else null
            }
            .forEach {
                (it.func as (data: T) -> Unit).invoke(data)
                if(it.event.startsWith("#once_")){
                    removeEventHandler(it)
                }
            }
    }

    fun callAny(event: String, data: Any?, context: Connection.Context = Connection.Context.ALL){
        when (data) {
            is String -> {
                try{
                    call(event, JSONObject(data), context)
                }catch(e: JSONException){
                    call(event, data, context)
                }
            }
            is BigInteger -> call(event, data, context)
            is BigDecimal -> call(event, data, context)
            is Boolean -> call(event, data, context)
            is Double -> call(event, data, context)
            is Float -> call(event, data, context)
            is Long -> call(event, data, context)
            is Number -> call(event, data, context)
            else -> call(event, data, context)
        }
    }

    fun call(event: String, context: Connection.Context = Connection.Context.ALL){
        call<Any?>(event, null, context)
    }

    data class EventHandler<T>(
        val event: String,
        val dataClass: Class<*>,
        val func: (data: T) -> Unit,
        val context: Connection.Context)

}