package io.github.highlandev210.kisha.ui.entry

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.highlandev210.kisha.AppContainer
import io.github.highlandev210.kisha.testing.FixedTimeSource
import io.github.highlandev210.kisha.testing.setUpWorkedExample
import io.github.highlandev210.kisha.testing.testContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class EntryViewModelTest {
    private val time = FixedTimeSource(Instant.parse("2026-11-01T18:29:00Z")) // 23:59 IST, Nov 1
    private lateinit var container: AppContainer
    private lateinit var vm: EntryViewModel

    @Before fun setUp() {
        container = testContainer(time)
        runBlocking { container.setUpWorkedExample() }
        vm = EntryViewModel(container.expenseRepository, container.budgetRepository, time)
    }

    @After fun tearDown() = container.database.close()

    private suspend fun savedExpense() = container.expenseRepository
        .observeForPeriod(container.database.periodDao().latest()!!.id)
        .first { it.isNotEmpty() }
        .single()

    /** A sheet left open across midnight must save to the day you tapped Save, not the day you opened it. */
    @Test fun untouchedTimeIsTakenWhenSaving() = runTest {
        vm.open(EntryTarget.NewExpense)
        listOf(1, 0, 0).forEach { vm.press(Key.Digit(it)) }
        time.currentInstant = Instant.parse("2026-11-01T18:31:00Z") // 00:01 IST, Nov 2

        vm.save()

        assertEquals(time.currentInstant, savedExpense().occurredAt)
    }

    @Test fun aPickedTimeIsKept() = runTest {
        vm.open(EntryTarget.NewExpense)
        listOf(1, 0, 0).forEach { vm.press(Key.Digit(it)) }
        vm.setDateTime(LocalDate.of(2026, 11, 1), LocalTime.of(10, 0))
        time.currentInstant = Instant.parse("2026-11-01T18:31:00Z")

        vm.save()

        assertEquals(LocalDate.of(2026, 11, 1).atTime(10, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant(), savedExpense().occurredAt)
    }
}
