package com.ccard.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Transaction::class, CardCondition::class],
    // v5: 스키마 변경은 없지만, 연도 추정 버그로 미래 날짜가 저장된 기존 거래를
    // 정리하기 위해 버전을 올려 로컬 데이터를 초기화한다 (재스캔은 자동으로 다시 돈다).
    version = 5,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun cardConditionDao(): CardConditionDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ccard.db",
                )
                    // 이 앱은 아직 초기 개발 단계라 마이그레이션 대신 스키마 변경 시 로컬 데이터를 초기화한다.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
