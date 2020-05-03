package de.walamana.gamesdk.ext

import de.walamana.gamesdk.server.Parsable
import org.json.JSONArray
import org.json.JSONObject

fun JSONObject.getOrNull(key: String): Any? = if(has(key)) get(key) else null
fun JSONObject.getStringOrNull(key: String): String? = if(has(key)) getString(key) else null
fun JSONObject.getJSONObjectOrNull(key: String): JSONObject? = if(has(key)) getJSONObject(key) else null
fun JSONObject.getJSONArrayOrNull(key: String): JSONArray? = if(has(key)) getJSONArray(key) else null


fun Collection<*>.toJSONArray(): JSONArray = JSONArray().also { json ->
    for(el in this){
        if(el is Parsable){
            json.put(el.toJSON())
        }else{
            json.put(el.toString())
        }
    }
}

fun Array<*>.toJSONArray(): JSONArray = JSONArray().also { json ->
    for(el in this){
        if(el is Parsable){
            json.put(el.toJSON())
        }else{
            json.put(el.toString())
        }
    }
}


fun json(func: JSONObject.() -> Unit): JSONObject = JSONObject().apply(func)