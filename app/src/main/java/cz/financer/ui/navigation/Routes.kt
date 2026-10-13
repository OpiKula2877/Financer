package cz.financer.ui.navigation

/** Trasy aplikace. Pořadí a tvar argumentů odpovídá FinancerNavHost. */
object Routes {
    const val PROJECTS = "projects"

    const val ARG_PROJECT = "projectId"
    const val ARG_CLIENT = "clientId"
    const val ARG_PRODUCT = "productId"
    const val ARG_TYPE = "type"
    const val ARG_RECEIPT = "receiptId"

    const val CLIENTS = "project/{$ARG_PROJECT}/clients"
    const val PRODUCTS = "project/{$ARG_PROJECT}/products"
    const val SALES = "project/{$ARG_PROJECT}/sales"
    const val ACCOUNTING = "project/{$ARG_PROJECT}/accounting"

    const val CLIENT_DETAIL = "project/{$ARG_PROJECT}/clients/{$ARG_CLIENT}"
    const val PRODUCT_NEW = "project/{$ARG_PROJECT}/products/new"
    const val PRODUCT_EDIT = "project/{$ARG_PROJECT}/products/edit/{$ARG_PRODUCT}"
    const val SALE_NEW = "project/{$ARG_PROJECT}/sales/new/{$ARG_TYPE}"
    const val SETTINGS = "project/{$ARG_PROJECT}/settings"
    const val RECEIPT_SETTINGS = "project/{$ARG_PROJECT}/receipt-settings"

    /** Receipt id 0 znamená účtenku otevřenou z QR kódu, která není uložená v telefonu. */
    const val RECEIPT = "project/{$ARG_PROJECT}/receipt/{$ARG_RECEIPT}"

    /** Záložky, na kterých je vidět spodní navigace. */
    val projectTabs = setOf(CLIENTS, PRODUCTS, SALES, ACCOUNTING)

    fun clients(projectId: Long) = "project/$projectId/clients"
    fun products(projectId: Long) = "project/$projectId/products"
    fun sales(projectId: Long) = "project/$projectId/sales"
    fun accounting(projectId: Long) = "project/$projectId/accounting"
    fun clientDetail(projectId: Long, clientId: Long) = "project/$projectId/clients/$clientId"
    fun productNew(projectId: Long) = "project/$projectId/products/new"
    fun productEdit(projectId: Long, productId: Long) = "project/$projectId/products/edit/$productId"
    fun saleNew(projectId: Long, type: String) = "project/$projectId/sales/new/$type"
    fun settings(projectId: Long) = "project/$projectId/settings"
    fun receiptSettings(projectId: Long) = "project/$projectId/receipt-settings"
    fun receipt(projectId: Long, receiptId: Long) = "project/$projectId/receipt/$receiptId"
}
