package com.ritune.platform.android

import com.ritune.core.models.IdGenerator
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UuidGenerator @Inject constructor() : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}