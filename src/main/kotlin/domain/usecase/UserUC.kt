package miner.domain.usecase

import miner.data.UserTable
import miner.data.model.User
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

interface UserUC {
    fun getUserById(userId: Int): User?
    fun getUserByUsername(username: String): User?
    fun usernameAvailable(username: String): Boolean
    fun registerUser(username: String, passwordHash: ByteArray): Int
}

class UserUCImpl : UserUC {
    override fun getUserById(userId: Int): User? = transaction {
        UserTable.select { UserTable.id.eq(userId) }
            .mapNotNull {
                User(it[UserTable.id].value, it[UserTable.username], it[UserTable.passwordHash])
            }
            .singleOrNull()
    }

    override fun getUserByUsername(username: String): User? = transaction {
        UserTable.select { UserTable.username.eq(username) }
            .mapNotNull { User(it[UserTable.id].value, it[UserTable.username], it[UserTable.passwordHash]) }
            .singleOrNull()
    }

    override fun usernameAvailable(username: String): Boolean {
        return getUserByUsername(username) == null
    }

    override fun registerUser(username: String, passwordHash: ByteArray): Int = transaction {
        UserTable.insertAndGetId {
            it[UserTable.username] = username
            it[UserTable.passwordHash] = passwordHash
        }.value
    }
}
