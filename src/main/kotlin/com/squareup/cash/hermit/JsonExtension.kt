package com.squareup.cash.hermit

import kotlinx.serialization.json.*

fun JsonElement.asArray(): Result<JsonArray> {
    return if (this is JsonArray) {
        success(this.jsonArray)
    } else {
        failure("Expected an array. Got ${this.javaClass.simpleName} instead")
    }
}

fun JsonElement.asObject(): Result<JsonObject> {
    return if (this is JsonObject) {
        success(this.jsonObject)
    } else {
        failure("Expected an object. Got ${this.javaClass.simpleName} instead")
    }
}

fun JsonElement.asPrimitive(): Result<JsonPrimitive> {
    return if (this is JsonPrimitive) {
        success(this.jsonPrimitive)
    } else {
        failure("Expected a primitive. Got ${this.javaClass.simpleName} instead")
    }
}

fun JsonObject.field(name: String): Result<JsonElement> {
    return if (this.containsKey(name)) {
        success(this.get(name)!!)
    } else {
        failure("The object does not contain a field called $name")
    }
}

fun JsonPrimitive.nullableString(): String {
    return if (this.contentOrNull == null) {
        "<null>"
    } else {
        this.content
    }
}