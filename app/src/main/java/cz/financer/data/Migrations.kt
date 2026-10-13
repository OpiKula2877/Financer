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
