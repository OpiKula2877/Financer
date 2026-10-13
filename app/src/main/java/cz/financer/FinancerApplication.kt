package cz.financer

import android.app.Application
import androidx.room.Room
import cz.financer.data.BackupManager
import cz.financer.data.FinancerDatabase
import cz.financer.data.FinancerRepository
import cz.financer.data.MIGRATION_1_2
import cz.financer.data.MIGRATION_2_3
import cz.financer.image.ImageStore
import cz.financer.receipt.OpenedReceipt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Ručně vytvořený kontejner závislostí: databáze, úložiště obrázků, repozitář a záloha. */
class FinancerApplication : Application() {

    val images: ImageStore by lazy { ImageStore(this) }

    val database: FinancerDatabase by lazy {
        Room.databaseBuilder(this, FinancerDatabase::class.java, "financer.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }

    val repository: FinancerRepository by lazy { FinancerRepository(database, images) }

    val backup: BackupManager by lazy { BackupManager(this, database, images) }

    /** Účtenka otevřená z QR kódu nebo ručně, která není uložená v tomto telefonu. */
    var openedReceipt: OpenedReceipt? = null

    override fun onCreate() {
        super.onCreate()
        // Identifikační klíč vzniká hned při prvním spuštění, ne až při první účtence.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { repository.identificationKey() }
    }
}
