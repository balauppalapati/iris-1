package com.scienaptic.common.util

import io.vertx.core.json.JsonObject

/**
  * Add additional methods to [[io.vertx.core.json.JsonObject]]
  * Get value from JsonObject using (key, defaultValue) tuple.
  */
object JsonObjectImplicit {

  implicit class JsonObjectHelper(val json: JsonObject) extends AnyVal {

    /**
      * Return String from JsonObject from key with a fallback value
      */
    def getString(keyValue: (String, String)): String = json.getString(keyValue._1, keyValue._2)

    /**
      * Return Int from JsonObject from key with a fallback value
      */
    def getInteger(keyValue: (String, Int)): Integer = json.getInteger(keyValue._1, keyValue._2)

    /**
      * Return Long from JsonObject from key with a fallback value
      */
    def getLong(keyValue: (String, Long)): Long = json.getLong(keyValue._1, keyValue._2)
  }

}
