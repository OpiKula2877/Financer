package cz.financer.data

import androidx.room.withTransaction
import cz.financer.domain.CartLine
import cz.financer.domain.SaleCalculator
import cz.financer.image.ImageStore
import cz.financer.receipt.ReceiptBuilder
import cz.financer.receipt.ReceiptData
import cz.financer.receipt.ReceiptLayout
import cz.financer.security.ReceiptCrypto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Jediné místo, které mluví s databází a se soubory obrázků. */
class FinancerRepository(
    private val database: FinancerDatabase,
    private val images: ImageStore,
) {
    private val projects = database.projectDao()
    private val definitions = database.propertyDefinitionDao()
    private val clients = database.clientDao()
    private val clientValues = database.clientPropertyValueDao()
    private val products = database.productDao()
    private val sales = database.saleDao()
    private val receipts = database.receiptDao()
    private val appSettings = database.appSettingsDao()

    // Projekty

    fun observeProjects(): Flow<List<Project>> = projects.observeAll()

    fun observeProject(projectId: Long): Flow<Project?> = projects.observeById(projectId)

    suspend fun createProject(
        name: String,
        ownerFirstName: String,
        ownerLastName: String,
        logoPath: String?,
    ): Long = projects.insert(
        Project(
            name = name.trim(),
            ownerFirstName = ownerFirstName.trim(),
            ownerLastName = ownerLastName.trim(),
            logoPath = logoPath,
            createdAt = System.currentTimeMillis(),
        ),
    )

    /** Upraví jen název, majitele a logo. Záměrně nesahá na číslování účtenek. */
    suspend fun updateProjectDetails(
        projectId: Long,
        name: String,
        ownerFirstName: String,
        ownerLastName: String,
        logoPath: String?,
    ) = projects.updateDetails(projectId, name.trim(), ownerFirstName.trim(), ownerLastName.trim(), logoPath)

    /** Smaže projekt i všechny jeho obrázky. Záznamy v databázi se smažou kaskádově. */
    suspend fun deleteProject(projectId: Long) {
        val project = projects.getAll().firstOrNull { it.id == projectId }
        val productImages = products.getAll().filter { it.projectId == projectId }.map { it.imagePath }
        projects.delete(projectId)
        productImages.forEach { images.delete(it) }
        images.delete(project?.logoPath)
    }

    // Vlastnosti klienta (nastavení projektu)

    fun observePropertyDefinitions(projectId: Long): Flow<List<PropertyDefinition>> =
        definitions.observeForProject(projectId)

    suspend fun addPropertyDefinition(projectId: Long, name: String, position: Int) {
        definitions.insert(PropertyDefinition(projectId = projectId, name = name.trim(), position = position))
    }

    suspend fun updatePropertyDefinition(definition: PropertyDefinition) = definitions.update(definition)

    suspend fun deletePropertyDefinition(definitionId: Long) = definitions.delete(definitionId)

    // Klienti

    fun observeClients(projectId: Long): Flow<List<Client>> = clients.observeForProject(projectId)

    suspend fun addClient(projectId: Long, name: String): Long = clients.insert(
        Client(projectId = projectId, name = name.trim(), createdAt = System.currentTimeMillis()),
    )

    /**
     * Založí klienta a rovnou zapíše jeho stav před používáním aplikace:
     * [debtHalire] je dluh, který ještě zbývá zaplatit, [paidHalire] je částka, kterou už zaplatil.
     * Obě části jsou ručně zadané (typ "Nastavený ručně"). Dluh = dluh + zaplaceno − splacení.
     */
    suspend fun addClientWithBalance(projectId: Long, name: String, debtHalire: Long, paidHalire: Long): Long {
        require(debtHalire >= 0 && paidHalire >= 0) { "Částky nesmí být záporné" }
        val clientId = addClient(projectId, name)
        val now = System.currentTimeMillis()
        if (debtHalire + paidHalire > 0) {
            sales.insertTransaction(
                SaleTransaction(
                    projectId = projectId,
                    clientId = clientId,
                    type = TransactionType.DEBT,
                    createdAt = now,
                    totalHalire = debtHalire + paidHalire,
                    isManual = true,
                ),
            )
        }
        if (paidHalire > 0) {
            sales.insertTransaction(
                SaleTransaction(
                    projectId = projectId,
                    clientId = clientId,
                    type = TransactionType.REPAYMENT,
                    createdAt = now,
                    totalHalire = paidHalire,
                    isManual = true,
                ),
            )
        }
        return clientId
    }

    /** Osobní vlastnosti všech klientů projektu. */
    fun observePersonalDefinitions(projectId: Long): Flow<List<PropertyDefinition>> =
        definitions.observePersonalForProject(projectId)

    /** Přidá vlastnost jen jednomu klientovi, rovnou s hodnotou. */
    suspend fun addPersonalProperty(projectId: Long, clientId: Long, name: String, value: String) {
        val definitionId = definitions.insert(
            PropertyDefinition(projectId = projectId, clientId = clientId, name = name.trim(), position = 0),
        )
        setClientValue(clientId, definitionId, value)
    }

    suspend fun updateClient(client: Client) = clients.update(client)

    suspend fun deleteClient(clientId: Long) = clients.delete(clientId)

    /** Hodnoty vlastností všech klientů projektu. */
    fun observeClientValues(projectId: Long): Flow<List<ClientPropertyValue>> =
        clientValues.observeForProject(projectId)

    /** Prázdná hodnota vlastnost u klienta odstraní. */
    suspend fun setClientValue(clientId: Long, definitionId: Long, value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            clientValues.delete(clientId, definitionId)
        } else {
            clientValues.upsert(ClientPropertyValue(clientId = clientId, definitionId = definitionId, value = trimmed))
        }
    }

    // Produkty

    fun observeProducts(projectId: Long): Flow<List<Product>> = products.observeForProject(projectId)

    suspend fun saveProduct(product: Product): Long {
        return if (product.id == 0L) {
            products.insert(product)
        } else {
            products.update(product)
            product.id
        }
    }

    suspend fun deleteProduct(product: Product) {
        products.delete(product.id)
        images.delete(product.imagePath)
    }

    // Prodeje, platby a dluhy

    fun observeSales(projectId: Long): Flow<List<SaleWithItems>> = sales.observeForProject(projectId)

    /**
     * Zapíše platbu (PAYMENT) nebo dluh (DEBT) se seznamem produktů.
     * Cena se spočítá z košíku, položky si uloží snapshot názvu a ceny. Vrací ID transakce.
     */
    suspend fun recordSale(
        projectId: Long,
        clientId: Long,
        type: TransactionType,
        createdAt: Long,
        lines: List<CartLine>,
    ): Long {
        require(type != TransactionType.REPAYMENT) { "Splacení se zapisuje přes recordRepayment" }
        require(lines.isNotEmpty()) { "Košík je prázdný" }
        require(lines.all { it.quantity > 0 }) { "Množství musí být kladné" }

        val transactionId = sales.insertTransaction(
            SaleTransaction(
                projectId = projectId,
                clientId = clientId,
                type = type,
                createdAt = createdAt,
                totalHalire = SaleCalculator.cartTotal(lines),
            ),
        )
        sales.insertItems(
            lines.map {
                SaleItem(
                    transactionId = transactionId,
                    productId = it.productId,
                    productNameSnapshot = it.productName,
                    unitPriceHalire = it.unitPriceHalire,
                    quantity = it.quantity,
                )
            },
        )
        return transactionId
    }

    /** Splacení dluhu libovolnou částkou. Kontrolu, že částka nepřesahuje dluh, dělá ProjectViewModel. */
    suspend fun recordRepayment(projectId: Long, clientId: Long, createdAt: Long, amountHalire: Long): Long {
        require(amountHalire > 0) { "Částka musí být větší než nula" }
        return sales.insertTransaction(
            SaleTransaction(
                projectId = projectId,
                clientId = clientId,
                type = TransactionType.REPAYMENT,
                createdAt = createdAt,
                totalHalire = amountHalire,
            ),
        )
    }

    /** Smaže záznam. Jeho účtenka zůstane v číslování a označí se jako stornovaná. */
    suspend fun deleteSale(transactionId: Long) = database.withTransaction {
        receipts.cancelForTransaction(transactionId)
        sales.deleteTransaction(transactionId)
    }

    /** Aktuální dluh klienta jako tok hodnot, pro profil klienta. */
    fun observeDebt(projectId: Long, clientId: Long): Flow<Long> =
        observeSales(projectId).map { SaleCalculator.debtOf(clientId, it) }

    /** Kombinovaný tok pro seznam klientů: klienti spolu s dluhem a vydělaným. */
    fun observeClientSummaries(projectId: Long): Flow<List<Pair<Client, Long>>> =
        combine(observeClients(projectId), observeSales(projectId)) { clientList, saleList ->
            clientList.map { it to SaleCalculator.debtOf(it.id, saleList) }
        }

    // Účtenky

    /** Údaje na účtence. Prázdné hodnoty se uloží jako null a na účtence se nezobrazí. */
    suspend fun updateReceiptHeader(projectId: Long, taxpayerName: String, address: String, phone: String) =
        projects.updateReceiptHeader(
            projectId,
            taxpayerName.trim().ifEmpty { null },
            address.trim().ifEmpty { null },
            phone.trim().ifEmpty { null },
        )

    suspend fun updateReceiptLayout(projectId: Long, layout: ReceiptLayout) =
        projects.updateReceiptLayout(projectId, layout.toJson())

    /** Nastaví číslo další účtenky. Vrací chybu, nebo null, pokud je nastavení v pořádku. */
    suspend fun setNextReceiptNumber(projectId: Long, value: Long): String? = database.withTransaction {
        val minimum = receipts.maxNumber(projectId) + 1
        if (value < minimum) return@withTransaction "Další číslo musí být alespoň $minimum."
        projects.setNextReceiptNumber(projectId, value)
        null
    }

    /** Identifikační klíč instalace. Při prvním použití se vytvoří a uloží. */
    suspend fun identificationKey(): String = database.withTransaction { identificationKeyInTransaction() }

    private suspend fun identificationKeyInTransaction(): String =
        appSettings.get()?.identificationKey
            ?: ReceiptCrypto.generateKey().also { appSettings.save(AppSettings(identificationKey = it)) }

    /**
     * Vystaví účtenku k zapsanému záznamu. Přidělí číslo, zašifruje obsah a podepíše ho BKP.
     * Vrací ID účtenky.
     */
    suspend fun issueReceipt(transactionId: Long): Long = database.withTransaction {
        val sale = sales.getWithItems(transactionId) ?: error("Záznam neexistuje.")
        val project = projects.getById(sale.sale.projectId) ?: error("Projekt neexistuje.")
        val clientName = clients.getById(sale.sale.clientId)?.name
        val number = project.nextReceiptNumber
        val json = ReceiptBuilder.build(project, sale, clientName, number).toJson()
        val key = identificationKeyInTransaction()
        projects.setNextReceiptNumber(project.id, number + 1)
        receipts.insert(
            Receipt(
                projectId = project.id,
                transactionId = transactionId,
                number = number,
                type = sale.sale.type,
                bkp = ReceiptCrypto.bkp(key, json),
                payload = ReceiptCrypto.encrypt(key, json),
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun getReceipt(receiptId: Long): Receipt? = receipts.getById(receiptId)

    suspend fun findReceiptByBkp(bkp: String): Receipt? = receipts.findByBkp(bkp).firstOrNull()

    /** Účtenka k záznamu. Ručně zadané záznamy a záznamy bez účtenky vrací null. */
    suspend fun findReceiptByTransaction(transactionId: Long): Receipt? = receipts.findByTransaction(transactionId)

    /** Rozšifruje účtenku klíčem a ověří, že její obsah odpovídá BKP. Jinak vyhodí výjimku. */
    fun openReceipt(key: String, bkp: String, payload: String): ReceiptData {
        val json = ReceiptCrypto.decrypt(key, payload)
        check(ReceiptCrypto.bkp(key, json) == bkp) { "BKP neodpovídá obsahu účtenky." }
        return ReceiptData.fromJson(json)
    }
}
