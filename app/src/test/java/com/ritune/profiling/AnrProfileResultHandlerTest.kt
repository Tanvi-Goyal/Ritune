package com.ritune.profiling

import android.os.ProfilingResult
import org.junit.Test
import kotlin.test.assertEquals

class AnrProfileResultHandlerTest {

    private val scheduled = mutableListOf<String>()
    private val handler = AnrProfileResultHandler { scheduled += it }

    @Test
    fun onResult_success_schedulesUpload() {
        handler.onResult(ProfilingResult.ERROR_NONE, "/files/profiling/anr.perfetto-trace")

        assertEquals(listOf("/files/profiling/anr.perfetto-trace"), scheduled)
    }

    @Test
    fun onResult_error_schedulesNothing() {
        handler.onResult(ProfilingResult.ERROR_FAILED_RATE_LIMIT_PROCESS, null)

        assertEquals(emptyList(), scheduled)
    }

    @Test
    fun onResult_successWithoutPath_schedulesNothing() {
        handler.onResult(ProfilingResult.ERROR_NONE, null)

        assertEquals(emptyList(), scheduled)
    }
}
