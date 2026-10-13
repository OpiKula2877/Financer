package cz.financer.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeById(id: Long): Flow<Project?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: Long): Project?

    @Query("SELECT * FROM projects")
    suspend fun getAll(): List<Project>

    @Insert
    suspend fun insert(project: Project): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<Project>)

    /** Jen základní údaje. Záměrně nepřepisuje číslování účtenek. */
    @Query("UPDATE projects SET name = :name, ownerFirstName = :ownerFirstName, ownerLastName = :ownerLastName, logoPath = :logoPath WHERE id = :id")
    suspend fun updateDetails(id: Long, name: String, ownerFirstName: String, ownerLastName: String, logoPath: String?)

    @Query("UPDATE projects SET taxpayerName = :taxpayerName, address = :address, phone = :phone WHERE id = :id")
    suspend fun updateReceiptHeader(id: Long, taxpayerName: String?, address: String?, phone: String?)

    @Query("UPDATE projects SET receiptLayout = :layout WHERE id = :id")
    suspend fun updateReceiptLayout(id: Long, layout: String?)

    @Query("UPDATE projects SET nextReceiptNumber = :value WHERE id = :id")
    suspend fun setNextReceiptNumber(id: Long, value: Long)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PropertyDefinitionDao {
    /** Globální vlastnosti projektu (ne osobní vlastnosti klientů). */
    @Query("SELECT * FROM property_definitions WHERE projectId = :projectId AND clientId IS NULL ORDER BY position")
    fun observeForProject(projectId: Long): Flow<List<PropertyDefinition>>

    /** Osobní vlastnosti všech klientů projektu. */
    @Query("SELECT * FROM property_definitions WHERE projectId = :projectId AND clientId IS NOT NULL ORDER BY position")
    fun observePersonalForProject(projectId: Long): Flow<List<PropertyDefinition>>

    @Query("SELECT * FROM property_definitions")
    suspend fun getAll(): List<PropertyDefinition>

    @Insert
    suspend fun insert(definition: PropertyDefinition): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(definitions: List<PropertyDefinition>)

    @Update
    suspend fun update(definition: PropertyDefinition)

    @Query("DELETE FROM property_definitions WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients WHERE projectId = :projectId ORDER BY name COLLATE NOCASE")
    fun observeForProject(projectId: Long): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getById(id: Long): Client?

    @Query("SELECT * FROM clients")
    suspend fun getAll(): List<Client>

    @Insert
    suspend fun insert(client: Client): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clients: List<Client>)

    @Update
    suspend fun update(client: Client)

    @Query("DELETE FROM clients WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ClientPropertyValueDao {
    @Query(
        """
        SELECT v.* FROM client_property_values v
        INNER JOIN clients c ON c.id = v.clientId
        WHERE c.projectId = :projectId
        """,
    )
    fun observeForProject(projectId: Long): Flow<List<ClientPropertyValue>>

    @Query("SELECT * FROM client_property_values")
    suspend fun getAll(): List<ClientPropertyValue>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(value: ClientPropertyValue)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(values: List<ClientPropertyValue>)

    @Query("DELETE FROM client_property_values WHERE clientId = :clientId AND definitionId = :definitionId")
    suspend fun delete(clientId: Long, definitionId: Long)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeForProject(projectId: Long): Flow<List<Product>>

    @Query("SELECT * FROM products")
    suspend fun getAll(): List<Product>

    @Insert
    suspend fun insert(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun update(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun delete(id: Long)
}

data class SaleWithItems(
    @Embedded val sale: SaleTransaction,
    @Relation(parentColumn = "id", entityColumn = "transactionId")
    val items: List<SaleItem>,
)

@Dao
interface SaleDao {
    @Transaction
    @Query("SELECT * FROM sale_transactions WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeForProject(projectId: Long): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sale_transactions WHERE id = :id")
    suspend fun getWithItems(id: Long): SaleWithItems?

    @Query("SELECT * FROM sale_transactions")
    suspend fun getAllTransactions(): List<SaleTransaction>

    @Query("SELECT * FROM sale_items")
    suspend fun getAllItems(): List<SaleItem>

    @Insert
    suspend fun insertTransaction(transaction: SaleTransaction): Long

    @Insert
    suspend fun insertItems(items: List<SaleItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTransactions(transactions: List<SaleTransaction>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllItems(items: List<SaleItem>)

    @Query("DELETE FROM sale_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)
}

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getById(id: Long): Receipt?

    @Query("SELECT * FROM receipts WHERE bkp = :bkp")
    suspend fun findByBkp(bkp: String): List<Receipt>

    @Query("SELECT * FROM receipts WHERE transactionId = :transactionId ORDER BY id DESC LIMIT 1")
    suspend fun findByTransaction(transactionId: Long): Receipt?

    @Query("SELECT * FROM receipts")
    suspend fun getAll(): List<Receipt>

    @Query("SELECT COALESCE(MAX(number), 0) FROM receipts WHERE projectId = :projectId")
    suspend fun maxNumber(projectId: Long): Long

    @Insert
    suspend fun insert(receipt: Receipt): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(receipts: List<Receipt>)

    @Query("UPDATE receipts SET cancelled = 1 WHERE transactionId = :transactionId")
    suspend fun cancelForTransaction(transactionId: Long)
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: AppSettings)
}
