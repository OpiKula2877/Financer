package cz.financer.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FinancerDatabase::class.java,
    )

    /** Data z verze 1 musí po migraci zůstat a nové sloupce musí mít bezpečné výchozí hodnoty. */
    @Test
    fun migrate1To2_keepsExistingDataAndAddsColumns() {
        helper.createDatabase(DB_NAME, 1).apply {
            execSQL("INSERT INTO projects (id, name, ownerFirstName, ownerLastName, logoPath, createdAt) VALUES (1, 'Projekt', 'Jan', 'Novák', NULL, 0)")
            execSQL("INSERT INTO clients (id, projectId, name, createdAt) VALUES (1, 1, 'Ana', 0)")
            execSQL("INSERT INTO property_definitions (id, projectId, name, position) VALUES (1, 1, 'Výška', 0)")
            execSQL("INSERT INTO sale_transactions (id, projectId, clientId, type, createdAt, totalHalire) VALUES (1, 1, 1, 'DEBT', 0, 4000)")
            close()
        }

        // runMigrationsAndValidate porovná schéma s exportovaným 2.json a selže při nesouladu.
        val db = helper.runMigrationsAndValidate(DB_NAME, 2, true, MIGRATION_1_2)

        db.query("SELECT name, clientId FROM property_definitions WHERE id = 1").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("Výška", cursor.getString(0))
            assertNull(cursor.getLongOrNull(1))
        }
        db.query("SELECT totalHalire, isManual FROM sale_transactions WHERE id = 1").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(4000L, cursor.getLong(0))
            assertEquals(0, cursor.getInt(1))
        }
        // Nová osobní vlastnost (clientId vyplněno) musí jít uložit.
        db.execSQL("INSERT INTO property_definitions (projectId, clientId, name, position) VALUES (1, 1, 'Velikost', 0)")
        db.close()
    }

    private fun android.database.Cursor.getLongOrNull(index: Int): Long? =
        if (isNull(index)) null else getLong(index)

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}
