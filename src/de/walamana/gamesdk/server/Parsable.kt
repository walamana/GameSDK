package de.walamana.gamesdk.server

import org.json.JSONObject

interface Parsable{
    fun toJSON(): JSONObject
}