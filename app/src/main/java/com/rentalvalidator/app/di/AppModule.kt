package com.rentalvalidator.app.di

import android.content.Context
import androidx.room.Room
import com.rentalvalidator.app.data.local.AppDatabase
import com.rentalvalidator.app.data.local.dao.PaymentDao
import com.rentalvalidator.app.data.local.dao.BackupDao
import com.rentalvalidator.app.data.local.dao.TenantDao
import com.rentalvalidator.app.data.local.dao.UnitDao
import com.rentalvalidator.app.data.local.datastore.PreferencesManager
import com.rentalvalidator.app.data.repository.PaymentRepositoryImpl
import com.rentalvalidator.app.data.repository.TenantRepositoryImpl
import com.rentalvalidator.app.data.repository.UnitRepositoryImpl
import com.rentalvalidator.app.domain.repository.PaymentRepository
import com.rentalvalidator.app.domain.repository.TenantRepository
import com.rentalvalidator.app.domain.repository.UnitRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideTenantDao(database: AppDatabase): TenantDao {
        return database.tenantDao()
    }

    @Provides
    @Singleton
    fun providePaymentDao(database: AppDatabase): PaymentDao {
        return database.paymentDao()
    }

    @Provides
    @Singleton
    fun provideUnitDao(database: AppDatabase): UnitDao {
        return database.unitDao()
    }

    @Provides
    @Singleton
    fun provideBackupDao(database: AppDatabase): BackupDao {
        return database.backupDao()
    }

    @Provides
    @Singleton
    fun provideTenantRepository(tenantDao: TenantDao): TenantRepository {
        return TenantRepositoryImpl(tenantDao)
    }

    @Provides
    @Singleton
    fun providePaymentRepository(paymentDao: PaymentDao): PaymentRepository {
        return PaymentRepositoryImpl(paymentDao)
    }

    @Provides
    @Singleton
    fun provideUnitRepository(unitDao: UnitDao): UnitRepository {
        return UnitRepositoryImpl(unitDao)
    }

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }
}
