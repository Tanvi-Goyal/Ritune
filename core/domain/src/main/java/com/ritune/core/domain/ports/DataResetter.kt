package com.ritune.core.domain.ports

interface DataResetter {
    suspend fun resetAll()
}