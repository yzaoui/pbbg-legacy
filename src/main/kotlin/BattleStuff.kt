package com.bitwiserain.pbbg

import com.bitwiserain.pbbg.db.repository.SquadTable
import com.bitwiserain.pbbg.db.repository.UnitTable
import com.bitwiserain.pbbg.db.repository.UserTable
import com.bitwiserain.pbbg.db.repository.battle.BattleEnemyTable
import com.bitwiserain.pbbg.db.repository.battle.BattleSessionTable
import com.bitwiserain.pbbg.domain.model.MyUnit
import com.bitwiserain.pbbg.domain.model.MyUnit.IceCreamWizard
import com.bitwiserain.pbbg.domain.model.MyUnit.Twolip
import com.bitwiserain.pbbg.route.api.CharUnitJSON
import com.google.gson.annotations.SerializedName
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.ResultSet

interface BattleUC {
    fun getCurrentBattle(userId: Int): Battle?
    fun generateBattle(userId: Int): Battle
    fun attack(userId: Int, allyId: Long, enemyId: Long): Battle
}

class BattleUCImpl(private val db: Database) : BattleUC {
    override fun getCurrentBattle(userId: Int): Battle? = transaction(db) {
        val battleSession = BattleSessionTable.getBattleSessionId(userId)

        if (battleSession == null) return@transaction null

        Battle(allies = SquadTable.getAllies(userId), enemies = BattleEnemyTable.getEnemies(battleSession))
    }

    override fun generateBattle(userId: Int): Battle = transaction(db) {
        // TODO: Forbid action if a battle is already in progress

        val battleSession = BattleSessionTable.insertAndGetId {
            it[BattleSessionTable.userId] = EntityID(userId, UserTable)
        }

        BattleEnemyTable.insertEnemies(battleSession, listOf(
            IceCreamWizard(0, 10, 10, 2, 0L),
            Twolip(0, 16, 16, 3, 0L)
        ))

        val allies = SquadTable.getAllies(userId)
        val enemies = BattleEnemyTable.getEnemies(battleSession)

        Battle(allies = allies, enemies = enemies)
    }

    override fun attack(userId: Int, allyId: Long, enemyId: Long): Battle = transaction(db) {
        val battleSession = BattleSessionTable.getBattleSessionId(userId) ?: throw Exception()

        // Ally should exist
        val ally = SquadTable.getAlly(userId, allyId) ?: throw Exception()

        // Enemy should exist
        val enemy = BattleEnemyTable.getEnemy(battleSession, enemyId) ?: throw Exception()

        // Enemy should not already be dead
        if (enemy.dead) throw Exception()

        // Apply damage to attacked enemy and update unit
        val updatedEnemy = enemy.receiveDamage(ally.atk)
        UnitTable.updateUnit(enemyId, updatedEnemy)

        // Gain experience if ally killed enemy
        if (updatedEnemy.dead) {
            val updatedAlly = ally.gainExperience(2L)
            UnitTable.updateUnit(allyId, updatedAlly)
        }

        val updatedBattle = Battle(allies = SquadTable.getAllies(userId), enemies = BattleEnemyTable.getEnemies(battleSession))

        deleteBattleIfOver(updatedBattle, battleSession)

        return@transaction updatedBattle
    }

    private fun deleteBattleIfOver(battle: Battle, battleSession: EntityID<Long>) = transaction(db) {
        val aliveEnemies = battle.enemies.filter { it.hp > 0 }

        if (aliveEnemies.isEmpty()) {
            // All enemies are defeated

            val enemyIdCSV = battle.enemies.asSequence().map { it.id }.joinToString()

            BattleSessionTable.deleteBattle(battleSession)

            "DELETE FROM ${UnitTable.tableName} WHERE ${UnitTable.id.name} IN ($enemyIdCSV)".execAndMap {}
        }
    }
}

data class Battle(
    val allies: List<MyUnit>,
    val enemies: List<MyUnit>
)

class BattleJSON(
    @SerializedName("allies") val allies: List<CharUnitJSON>,
    @SerializedName("enemies") val enemies: List<CharUnitJSON>
)

fun BattleEnemyTable.insertEnemies(battleSession: EntityID<Long>, enemies: List<MyUnit>) {
    // TODO: There's gotta be a way to do this in batch :/
    for (enemy in enemies) {
        // Create enemy unit in unit table
        val enemyId = UnitTable.insertUnitAndGetId(enemy)

        // Connect newly created enemy to this battle session
        insert {
            it[BattleEnemyTable.battle] = battleSession
            it[BattleEnemyTable.unit] = enemyId
        }
    }
}

fun BattleEnemyTable.getEnemies(battleSession: EntityID<Long>): List<MyUnit> {
    return innerJoin(UnitTable)
        .slice(UnitTable.columns)
        .select { BattleEnemyTable.battle.eq(battleSession) }
        .map { it.toCharUnit() }
}

fun BattleEnemyTable.getEnemy(battleSession: EntityID<Long>, enemyId: Long): MyUnit? {
    return innerJoin(UnitTable)
        .slice(UnitTable.columns)
        .select { BattleEnemyTable.battle.eq(battleSession) and UnitTable.id.eq(enemyId) }
        .singleOrNull()
        ?.toCharUnit()
}

fun BattleSessionTable.getBattleSessionId(userId: Int): EntityID<Long>? {
    return select { BattleSessionTable.userId.eq(userId) }
        .singleOrNull()
        ?.get(BattleSessionTable.id)
}

fun BattleSessionTable.deleteBattle(battleSession: EntityID<Long>) {
    deleteWhere { BattleSessionTable.id.eq(battleSession) }
}

fun <T : Any> String.execAndMap(transform: (ResultSet) -> T): List<T> {
    val result = arrayListOf<T>()
    TransactionManager.current().exec(this) { rs ->
        while (rs.next()) {
            result += transform(rs)
        }
    }
    return result
}
