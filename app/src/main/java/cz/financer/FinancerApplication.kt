package cz.financer

import android.app.Application
import androidx.room.Room
import cz.financer.data.BackupManager
import cz.financer.data.FinancerDatabase
import cz.financer.data.FinancerRepository
import cz.financer.data.MIGRATION_1_2
import cz.financer.image.ImageStore

/** Ručně vytvořený kontejner závislostí: databáze, úložiště obrázků, repozitář a záloha. */
class FinancerApplication : Application() {

    val images: ImageStore by lazy { ImageStore(this) }

    val database: FinancerDatabase by lazy {
        Room.databaseBuilder(this, FinancerDatabase::class.java, "financer.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    val repository: FinancerRepository by lazy { FinancerRepository(database, images) }

    val backup: BackupManager by lazy { BackupManager(this, database, images) }
}
