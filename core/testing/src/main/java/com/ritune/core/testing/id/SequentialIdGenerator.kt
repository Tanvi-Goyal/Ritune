package com.ritune.core.testing.id

import com.ritune.core.models.IdGenerator

class SequentialIdGenerator(private val prefix: String = "id") : IdGenerator {

    private var next = 1

    override fun newId(): String = "$prefix-${next++}"
}
