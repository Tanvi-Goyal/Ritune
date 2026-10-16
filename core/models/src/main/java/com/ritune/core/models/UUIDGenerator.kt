package com.ritune.core.models

fun interface IdGenerator {
    fun newId(): String
}