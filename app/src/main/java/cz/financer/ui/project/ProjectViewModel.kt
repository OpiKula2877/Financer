package cz.financer.ui.project

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.financer.FinancerApplication
import cz.financer.data.Client
import cz.financer.data.Product
import cz.financer.data.PropertyDefinition
import cz.financer.data.Project
import cz.financer.data.SaleWithItems
import cz.financer.data.TransactionType
import cz.financer.domain.CartLine
import cz.financer.domain.SaleCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Klient s odvozenými hodnotami pro seznam a profil. */
data class ClientRow(
    val client: Client,
    val debt: Long,
    val earned: Long,
    /** Hodnoty vlastností: id definice → text. Zahrnují i osobní vlastnosti. */
    val values: Map<Long, String>,
    /** Osobní vlastnosti tohoto klienta (ne globální z nastavení projektu). */
    val personalProperties: List<PropertyDefinition>,
)

/** Produkt s počtem prodaných kusů. */
data class ProductRow(
    val product: Product,
    val soldQuantity: Int,
)

/**
 * Data jednoho projektu. Všechny taby a obrazovky projektu čtou stejné
 * toky, takže jakákoli změna se propíše všude.
 */
class ProjectViewModel(
    private val app: FinancerApplication,
    val projectId: Long,
) : ViewModel() {

    private val repository = app.repository

    val project: StateFlow<Project?> = repository.observeProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val definitions: StateFlow<List<PropertyDefinition>> = repository.observePropertyDefinitions(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sales: StateFlow<List<SaleWithItems>> = repository.observeSales(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val clients: StateFlow<List<ClientRow>> = combine(
        repository.observeClients(projectId),
        repository.observeClientValues(projectId),
        repository.observePersonalDefinitions(projectId),
        sales,
    ) { clientList, valueList, personalList, saleList ->
        val valuesByClient = valueList.groupBy { it.clientId }
        val personalByClient = personalList.groupBy { it.clientId }
        clientList.map { client ->
            ClientRow(
                client = client,
                debt = SaleCalculator.debtOf(client.id, saleList),
                earned = SaleCalculator.earnedFrom(client.id, saleList),
                values = valuesByClient[client.id].orEmpty().associate { it.definitionId to it.value },
                personalProperties = personalByClient[client.id].orEmpty(),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val products: StateFlow<List<ProductRow>> = combine(
        repository.observeProducts(projectId),
        sales,
    ) { productList, saleList ->
        productList.map { ProductRow(it, SaleCalculator.soldQuantity(it.id, saleList)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Projekt

    fun updateProject(current: Project, name: String, ownerFirstName: String, ownerLastName: String, newLogo: Uri?) {
        viewModelScope.launch {
            val logoPath = newLogo?.let { uri ->
                withContext(Dispatchers.IO) { app.images.saveSquare(uri) }?.also { app.images.delete(current.logoPath) }
            } ?: current.logoPath
            repository.updateProject(
                current.copy(
                    name = name.trim(),
                    ownerFirstName = ownerFirstName.trim(),
                    ownerLastName = ownerLastName.trim(),
                    logoPath = logoPath,
                ),
            )
        }
    }

    fun deleteProject(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            onDeleted()
        }
    }

    // Vlastnosti klienta

    fun addPropertyDefinition(name: String) {
        viewModelScope.launch {
            repository.addPropertyDefinition(projectId, name, definitions.value.size)
        }
    }

    fun renamePropertyDefinition(definition: PropertyDefinition, name: String) {
        viewModelScope.launch { repository.updatePropertyDefinition(definition.copy(name = name.trim())) }
    }

    fun deletePropertyDefinition(definition: PropertyDefinition) {
        viewModelScope.launch { repository.deletePropertyDefinition(definition.id) }
    }

    /** Prohodí pořadí dvou vlastností (posun nahoru / dolů). */
    fun swapPropertyOrder(first: PropertyDefinition, second: PropertyDefinition) {
        viewModelScope.launch {
            repository.updatePropertyDefinition(first.copy(position = second.position))
            repository.updatePropertyDefinition(second.copy(position = first.position))
        }
    }

    // Klienti

    /**
     * Založí klienta. [debtHalire] = dluh, který zbývá zaplatit, [paidHalire] = už zaplaceno.
     * Obě hodnoty jsou volitelné (0) a zapisují se jako ručně nastavený stav.
     */
    fun addClient(name: String, debtHalire: Long, paidHalire: Long, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            onCreated(repository.addClientWithBalance(projectId, name, debtHalire, paidHalire))
        }
    }

    /** Osobní vlastnost jednoho klienta, např. „Výška: 150 cm“. */
    fun addPersonalProperty(clientId: Long, name: String, value: String) {
        viewModelScope.launch { repository.addPersonalProperty(projectId, clientId, name, value) }
    }

    fun updateClient(client: Client, name: String) {
        viewModelScope.launch { repository.updateClient(client.copy(name = name.trim())) }
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch { repository.deleteClient(client.id) }
    }

    fun setClientValue(clientId: Long, definitionId: Long, value: String) {
        viewModelScope.launch { repository.setClientValue(clientId, definitionId, value) }
    }

    // Produkty

    /**
     * Uloží produkt. Nový obrázek se ořízne na 1:1 a starý se smaže.
     * [priceHalire] je už zvalidovaná cena za kus.
     */
    fun saveProduct(
        existing: Product?,
        name: String,
        priceHalire: Long,
        isSoldOut: Boolean,
        newImage: Uri?,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            val imagePath = newImage?.let { uri ->
                withContext(Dispatchers.IO) { app.images.saveSquare(uri) }?.also { app.images.delete(existing?.imagePath) }
            } ?: existing?.imagePath

            repository.saveProduct(
                (existing ?: Product(
                    projectId = projectId,
                    name = "",
                    imagePath = null,
                    unitPriceHalire = 0,
                    isSoldOut = false,
                    createdAt = System.currentTimeMillis(),
                )).copy(
                    name = name.trim(),
                    imagePath = imagePath,
                    unitPriceHalire = priceHalire,
                    isSoldOut = isSoldOut,
                ),
            )
            onSaved()
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch { repository.deleteProduct(product) }
    }

    // Platby a dluhy

    fun recordSale(
        clientId: Long,
        type: TransactionType,
        createdAt: Long,
        lines: List<CartLine>,
        onRecorded: () -> Unit,
    ) {
        viewModelScope.launch {
            repository.recordSale(projectId, clientId, type, createdAt, lines)
            onRecorded()
        }
    }

    /**
     * Splacení dluhu. Vrací chybovou zprávu, nebo null pokud je vše v pořádku.
     * Částka nesmí být vyšší než aktuální dluh klienta.
     */
    fun recordRepayment(clientId: Long, amountHalire: Long, createdAt: Long): String? {
        val debt = SaleCalculator.debtOf(clientId, sales.value)
        if (amountHalire <= 0) return "Částka musí být větší než nula."
        if (amountHalire > debt) return "Částka je vyšší než dluh klienta."
        viewModelScope.launch { repository.recordRepayment(projectId, clientId, createdAt, amountHalire) }
        return null
    }

    fun deleteSale(transactionId: Long) {
        viewModelScope.launch { repository.deleteSale(transactionId) }
    }
}
