package io.github.highlandev210.kisha.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** NF-5: no data loss across app updates. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), KishaDatabase::class.java)

    @Test fun migrate1To2KeepsDataAndAddsEmptyPocketDates() {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                "INSERT INTO budget_config (id, periodType, cycleStartDay, baseIncome, savingsTarget, allowanceMode, currency, indianGrouping) " +
                    "VALUES (1, 'MONTH', 1, 6000000, 0, 'SPREAD', '₹', 1)",
            )
            db.execSQL("INSERT INTO period (id, startDate, endDate, income, fixedTotal, savingsTarget) VALUES (1, 20393, 20422, 6000000, 3600000, 0)")
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 2, true)

        migrated.query("SELECT baseIncome, setupDate, pocketShownThrough FROM budget_config WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(6_000_000L, c.getLong(0))
            assertTrue(c.isNull(1))
            assertTrue(c.isNull(2))
        }
        migrated.query("SELECT COUNT(*) FROM period").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
        }
    }
}
