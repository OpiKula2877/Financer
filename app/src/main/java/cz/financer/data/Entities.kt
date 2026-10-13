package cz.financer.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Typ záznamu v účtárně: prodej zaplacený hned, prodej na dluh, nebo splacení dluhu. */
enum class TransactionType { PAYMENT, DEBT, REPAYMENT }

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ownerFirstName: String,
    val ownerLastName: String,
    val logoPath: String?,
    val createdAt: Long,
)

/**
 * Pojmenovaná vlastnost. Pokud je [clientId] null, je to globální vlastnost projektu
 * (nastavení projektu). Jinak je to osobní vlastnost jednoho klienta.
 */
@Entity(
    tableName = "property_definitions",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId"), Index("clientId")],
)
data class PropertyDefinition(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val clientId: Long? = null,
    val name: String,
    val position: Int,
)

@Entity(
    tableName = "clients",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val createdAt: Long,
)

/** Hodnota vlastnosti u konkrétního klienta. */
@Entity(
    tableName = "client_property_values",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PropertyDefinition::class,
            parentColumns = ["id"],
            childColumns = ["definitionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["clientId", "definitionId"], unique = true), Index("definitionId")],
)
data class ClientPropertyValue(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val definitionId: Long,
    val value: String,
)

/** Cena je uložená v haléřích, aby se nezaokrouhlovalo. */
@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val imagePath: String?,
    val unitPriceHalire: Long,
    val isSoldOut: Boolean,
    val createdAt: Long,
)

@Entity(
    tableName = "sale_transactions",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId"), Index("clientId")],
)
data class SaleTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val clientId: Long,
    val type: TransactionType,
    val createdAt: Long,
    val totalHalire: Long,
    /** true = záznam zadaný ručně při zakládání klienta (stav před používáním aplikace). */
    @ColumnInfo(defaultValue = "0") val isManual: Boolean = false,
)

/** Snapshot názvu a ceny: úprava produktu nezmění historii prodeje. */
@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleTransaction::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("transactionId"), Index("productId")],
)
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val productId: Long?,
    val productNameSnapshot: String,
    val unitPriceHalire: Long,
    val quantity: Int,
)
