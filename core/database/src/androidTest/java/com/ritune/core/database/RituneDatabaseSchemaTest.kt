package com.ritune.core.database

import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class RituneDatabaseSchemaTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val file = instrumentation.targetContext.getDatabasePath("schema-test.db")
    private val driver = AndroidSQLiteDriver()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = file,
        driver = driver,
        databaseClass = RituneDatabase::class,
    )

    @Test
    fun entities_matchLatestExportedSchema() = runTest {
        helper.createDatabase(LATEST_VERSION).close()

        val database = Room.databaseBuilder(instrumentation.targetContext, RituneDatabase::class.java, file.absolutePath)
            .setDriver(driver)
            .build()
        helper.closeWhenFinished(database)

        assertEquals(0, database.routineDao().count())
    }

    private companion object {
        const val LATEST_VERSION = 1
    }
}
