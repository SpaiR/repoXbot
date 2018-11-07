package io.github.spair.repoxbot.util

inline fun <reified E : Enum<E>> valueOfIgnoreCase(name: String, default: E) = enumValues<E>().find {
    it.name.equals(name, ignoreCase = true)
} ?: default
