package miner.domain.usecase

import miner.data.InventoryTable
import miner.data.UserTable
import miner.data.model.Inventory
import miner.data.model.InventoryItem
import miner.data.model.Item
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

interface InventoryUC {
    fun getInventory(userId: Int): Inventory
    fun storeInInventory(userId: Int, item: Item, quantity: Int)
}

class InventoryUCImpl : InventoryUC {
    override fun getInventory(userId: Int): Inventory = transaction {
        // TODO: Consider checking if user exists
        val items = InventoryTable.select { InventoryTable.userId.eq(userId) }
            .map { it.toInventoryItem() }

        Inventory(items)
    }

    override fun storeInInventory(userId: Int, item: Item, quantity: Int) {
        // TODO: Consider checking if user exists
        InventoryTable.insert {
            it[InventoryTable.userId] = EntityID(userId, UserTable)
            it[InventoryTable.item] = item
            it[InventoryTable.quantity] = quantity
        }
    }

    private fun ResultRow.toInventoryItem() = InventoryItem(
        item = this[InventoryTable.item],
        quantity = this[InventoryTable.quantity]
    )
}
