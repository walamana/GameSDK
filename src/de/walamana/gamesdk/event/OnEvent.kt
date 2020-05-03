package de.walamana.gamesdk.event

@Target(AnnotationTarget.FUNCTION)
annotation class OnEvent(val event: String = "")
