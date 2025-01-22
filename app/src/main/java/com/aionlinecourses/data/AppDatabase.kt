package com.aionlinecourses.data

import android.content.Context
import androidx.room.*
import com.aionlinecourses.data.dao.*
import com.aionlinecourses.data.entity.*
import com.aionlinecourses.data.converters.Converters

@Database(
    entities = [
        User::class,
        Course::class,
        Transaction::class,
        PaymentMethod::class,
        Subscription::class,
        Dispute::class,
        WebhookEvent::class,
        CourseProgress::class,
        Comment::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun courseDao(): CourseDao
    abstract fun transactionDao(): TransactionDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun disputeDao(): DisputeDao
    abstract fun webhookDao(): WebhookDao
    abstract fun courseProgressDao(): CourseProgressDao
    abstract fun commentDao(): CommentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_courses_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
