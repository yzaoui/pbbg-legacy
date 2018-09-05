package com.bitwiserain.pbbg.db.repository

import com.bitwiserain.pbbg.EMAIL_MAX_LENGTH
import com.bitwiserain.pbbg.USERNAME_MAX_LENGTH
import org.jetbrains.exposed.sql.Table

object UnconfirmedUserTable : Table() {
    val hash = uuid("hash").uniqueIndex()
    val username = UserTable.varchar("username", USERNAME_MAX_LENGTH)
    val passwordHash = UserTable.binary("password_hash", 60)
    val email = UserTable.varchar("email", EMAIL_MAX_LENGTH)
}
