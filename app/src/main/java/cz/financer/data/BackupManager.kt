package cz.financer.data

import android.content.Context
import android.net.Uri
import cz.financer.image.ImageStore
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Záloha = ZIP soubor s data.json (všechny tabulky, včetně účtenek a identifikačního klíče)
 * a složkou images/ s obrázky. Kdo má soubor, má i klíč, proto ho chraňte jako heslo.
 * Import nahradí všechna data v aplikaci obsahem zálohy. Zálohy verze 1 jdou načíst.
 */
class BackupManager(
    private val context: Context,
    private val database: FinancerDatabase,
    private val images: ImageStore,
) {

    suspend fun export(target: Uri) = withContext(Dispatchers.IO) {
        val projects = database.projectDao().getAll()
        val definitions = database.propertyDefinitionDao().getAll()
        val clients = database.clientDao().getAll()
        val values = database.clientPropertyValueDao().getAll()
        val products = database.productDao().getAll()
        val sales = database.saleDao().getAllTransactions()
        val items = database.saleDao().getAllItems()
        val receipts = database.receiptDao().getAll()
        val settings = database.appSettingsDao().get()

        val root = JSONObject()
            .put("version", 2)
            .put("projects", JSONArray(projects.map { p ->
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("ownerFirstName", p.ownerFirstName)
                    .put("ownerLastName", p.ownerLastName)
                    .put("logo", fileName(p.logoPath))
                    .put("createdAt", p.createdAt)
                    .put("taxpayerName", p.taxpayerName ?: JSONObject.NULL)
                    .put("address", p.address ?: JSONObject.NULL)
                    .put("phone", p.phone ?: JSONObject.NULL)
                    .put("nextReceiptNumber", p.nextReceiptNumber)
                    .put("receiptLayout", p.receiptLayout ?: JSONObject.NULL)
            }))
            .put("properties", JSONArray(definitions.map { d ->
                JSONObject().put("id", d.id).put("projectId", d.projectId).put("name", d.name).put("position", d.position)
            }))
            .put("clients", JSONArray(clients.map { c ->
                JSONObject().put("id", c.id).put("projectId", c.projectId).put("name", c.name).put("createdAt", c.createdAt)
            }))
            .put("clientValues", JSONArray(values.map { v ->
                JSONObject().put("id", v.id).put("clientId", v.clientId).put("definitionId", v.definitionId).put("value", v.value)
            }))
            .put("products", JSONArray(products.map { p ->
                JSONObject()
                    .put("id", p.id)
                    .put("projectId", p.projectId)
                    .put("name", p.name)
                    .put("image", fileName(p.imagePath))
                    .put("unitPriceHalire", p.unitPriceHalire)
                    .put("isSoldOut", p.isSoldOut)
                    .put("createdAt", p.createdAt)
            }))
            .put("sales", JSONArray(sales.map { s ->
                JSONObject()
                    .put("id", s.id)
                    .put("projectId", s.projectId)
                    .put("clientId", s.clientId)
                    .put("type", s.type.name)
                    .put("createdAt", s.createdAt)
                    .put("totalHalire", s.totalHalire)
            }))
            .put("saleItems", JSONArray(items.map { i ->
                JSONObject()
                    .put("id", i.id)
                    .put("transactionId", i.transactionId)
                    .put("productId", i.productId ?: JSONObject.NULL)
                    .put("productNameSnapshot", i.productNameSnapshot)
                    .put("unitPriceHalire", i.unitPriceHalire)
                    .put("quantity", i.quantity)
            }))
            .put("receipts", JSONArray(receipts.map { r ->
                JSONObject()
                    .put("id", r.id)
                    .put("projectId", r.projectId)
                    .put("transactionId", r.transactionId ?: JSONObject.NULL)
                    .put("number", r.number)
                    .put("type", r.type.name)
                    .put("bkp", r.bkp)
                    .put("payload", r.payload)
                    .put("createdAt", r.createdAt)
                    .put("cancelled", r.cancelled)
            }))
            .put("settings", JSONObject().put("identificationKey", settings?.identificationKey ?: JSONObject.NULL))

        val imagePaths = (projects.map { it.logoPath } + products.map { it.imagePath }).filterNotNull()

        val stream = context.contentResolver.openOutputStream(target)
            ?: error("Soubor pro zálohu nelze otevřít.")
        stream.use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("data.json"))
                zip.write(root.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                imagePaths.map { File(it) }.filter { it.exists() }.forEach { file ->
                    zip.putNextEntry(ZipEntry("images/${file.name}"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }

    /** Nahradí všechna data a obrázky obsahem zálohy. */
    suspend fun import(source: Uri) = withContext(Dispatchers.IO) {
        var dataJson: String? = null
        val imageBytes = mutableMapOf<String, ByteArray>()

        val stream = context.contentResolver.openInputStream(source)
            ?: error("Zálohu nelze otevřít.")
        stream.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "data.json" -> dataJson = zip.readBytes().toString(Charsets.UTF_8)
                        entry.name.startsWith("images/") -> imageBytes[entry.name.removePrefix("images/")] = zip.readBytes()
                    }
                    entry = zip.nextEntry
                }
            }
        }
        val root = JSONObject(dataJson ?: error("V záloze chybí data.json."))

        // Nejdřív smazat staré soubory, až potom zapsat nové. Jinak by se mohly smazat právě obnovené.
        val oldImages = database.productDao().getAll().mapNotNull { it.imagePath } +
            database.projectDao().getAll().mapNotNull { it.logoPath }
        database.projectDao().getAll().forEach { database.projectDao().delete(it.id) }
        oldImages.forEach { images.delete(it) }

        val imageDir = File(context.filesDir, "images").apply { mkdirs() }
        imageBytes.forEach { (name, bytes) -> File(imageDir, name).writeBytes(bytes) }
        fun imagePath(name: String?): String? = name?.let { File(imageDir, it).absolutePath }

        database.projectDao().insertAll(root.array("projects") { o ->
            Project(
                id = o.getLong("id"),
                name = o.getString("name"),
                ownerFirstName = o.getString("ownerFirstName"),
                ownerLastName = o.getString("ownerLastName"),
                logoPath = imagePath(o.str("logo")),
                createdAt = o.getLong("createdAt"),
                taxpayerName = o.str("taxpayerName"),
                address = o.str("address"),
                phone = o.str("phone"),
                nextReceiptNumber = o.optLong("nextReceiptNumber", 1),
                receiptLayout = o.str("receiptLayout"),
            )
        })
        database.propertyDefinitionDao().insertAll(root.array("properties") { o ->
            PropertyDefinition(
                id = o.getLong("id"),
                projectId = o.getLong("projectId"),
                name = o.getString("name"),
                position = o.getInt("position"),
            )
        })
        database.clientDao().insertAll(root.array("clients") { o ->
            Client(
                id = o.getLong("id"),
                projectId = o.getLong("projectId"),
                name = o.getString("name"),
                createdAt = o.getLong("createdAt"),
            )
        })
        database.clientPropertyValueDao().insertAll(root.array("clientValues") { o ->
            ClientPropertyValue(
                id = o.getLong("id"),
                clientId = o.getLong("clientId"),
                definitionId = o.getLong("definitionId"),
                value = o.getString("value"),
            )
        })
        database.productDao().insertAll(root.array("products") { o ->
            Product(
                id = o.getLong("id"),
                projectId = o.getLong("projectId"),
                name = o.getString("name"),
                imagePath = imagePath(o.str("image")),
                unitPriceHalire = o.getLong("unitPriceHalire"),
                isSoldOut = o.getBoolean("isSoldOut"),
                createdAt = o.getLong("createdAt"),
            )
        })
        database.saleDao().insertAllTransactions(root.array("sales") { o ->
            SaleTransaction(
                id = o.getLong("id"),
                projectId = o.getLong("projectId"),
                clientId = o.getLong("clientId"),
                type = TransactionType.valueOf(o.getString("type")),
                createdAt = o.getLong("createdAt"),
                totalHalire = o.getLong("totalHalire"),
            )
        })
        database.saleDao().insertAllItems(root.array("saleItems") { o ->
            SaleItem(
                id = o.getLong("id"),
                transactionId = o.getLong("transactionId"),
                productId = o.lng("productId"),
                productNameSnapshot = o.getString("productNameSnapshot"),
                unitPriceHalire = o.getLong("unitPriceHalire"),
                quantity = o.getInt("quantity"),
            )
        })
        database.receiptDao().insertAll(root.array("receipts") { o ->
            Receipt(
                id = o.getLong("id"),
                projectId = o.getLong("projectId"),
                transactionId = o.lng("transactionId"),
                number = o.getLong("number"),
                type = TransactionType.valueOf(o.getString("type")),
                bkp = o.getString("bkp"),
                payload = o.getString("payload"),
                createdAt = o.getLong("createdAt"),
                cancelled = o.optBoolean("cancelled", false),
            )
        })
        // Záloha verze 1 nemá klíč. Pak se zachová ten, který už v telefonu je.
        root.optJSONObject("settings")?.str("identificationKey")?.let { key ->
            database.appSettingsDao().save(AppSettings(identificationKey = key))
        }
    }

    private fun fileName(path: String?): Any = path?.let { File(it).name } ?: JSONObject.NULL

    private fun JSONObject.str(key: String): String? = if (isNull(key)) null else getString(key)

    private fun JSONObject.lng(key: String): Long? = if (isNull(key)) null else getLong(key)

    private fun <T> JSONObject.array(key: String, map: (JSONObject) -> T): List<T> {
        val array = optJSONArray(key) ?: return emptyList()
        return (0 until array.length()).map { map(array.getJSONObject(it)) }
    }
}
