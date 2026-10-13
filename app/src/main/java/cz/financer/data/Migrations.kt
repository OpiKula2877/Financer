package cz.financer.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 1 → 2: osobní vlastnosti klienta (property_definitions.clientId) a příznak ručně
 * zadaného záznamu (sale_transactions.isManual). Data se zachovají.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE property_definitions ADD COLUMN clientId INTEGER REFERENCES clients(id) ON DELETE CASCADE")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_property_definitions_clientId ON property_definitions (clientId)")
        db.execSQL("ALTER TABLE sale_transactions ADD COLUMN isManual INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * 2 → 3: údaje a číslování účtenek v projektech, tabulka účtenek a nastavení instalace
 * (identifikační klíč). Data se zachovají, nové sloupce mají výchozí hodnoty.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE projects ADD COLUMN taxpayerName TEXT")
        db.execSQL("ALTER TABLE projects ADD COLUMN address TEXT")
        db.execSQL("ALTER TABLE projects ADD COLUMN phone TEXT")
        db.execSQL("ALTER TABLE projects ADD COLUMN nextReceiptNumber INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE projects ADD COLUMN receiptLayout TEXT")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS receipts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                projectId INTEGER NOT NULL,
                transactionId INTEGER,
                number INTEGER NOT NULL,
                type TEXT NOT NULL,
                bkp TEXT NOT NULL,
                payload TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                cancelled INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_receipts_projectId_number ON receipts (projectId, number)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_receipts_bkp ON receipts (bkp)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS app_settings (
                id INTEGER NOT NULL PRIMARY KEY,
                identificationKey TEXT NOT NULL
            )
            """.trimIndent(),
        )
    }
}
