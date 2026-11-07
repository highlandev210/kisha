package io.github.highlandev210.kisha.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter fun epochDayToLocalDate(day: Long?): LocalDate? = day?.let(LocalDate::ofEpochDay)

    @TypeConverter fun instantToEpochMilli(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter fun epochMilliToInstant(millis: Long?): Instant? = millis?.let(Instant::ofEpochMilli)
}

@Database(
    entities = [
        BudgetConfigEntity::class,
        PeriodEntity::class,
        ExpenseEntity::class,
        IncomeEntity::class,
        FixedCostEntity::class,
        CategoryEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(Converters::class)
abstract class KishaDatabase : RoomDatabase() {
    abstract fun budgetConfigDao(): BudgetConfigDao
    abstract fun periodDao(): PeriodDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun fixedCostDao(): FixedCostDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        fun build(context: Context): KishaDatabase =
            Room.databaseBuilder(context, KishaDatabase::class.java, "kisha.db").build()

        fun inMemory(context: Context): KishaDatabase =
            Room.inMemoryDatabaseBuilder(context, KishaDatabase::class.java).build()
    }
}
