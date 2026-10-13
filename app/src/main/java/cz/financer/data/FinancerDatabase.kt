package cz.financer.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Project::class,
        PropertyDefinition::class,
        Client::class,
        ClientPropertyValue::class,
        Product::class,
        SaleTransaction::class,
        SaleItem::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class FinancerDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun propertyDefinitionDao(): PropertyDefinitionDao
    abstract fun clientDao(): ClientDao
    abstract fun clientPropertyValueDao(): ClientPropertyValueDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
}
