package com.bitwiserain.pbbg.domain.usecase

import com.bitwiserain.pbbg.domain.model.mine.Mine
import com.bitwiserain.pbbg.domain.model.mine.MineActionResult

interface MiningUC {
    fun getMine(userId: Int): Mine?
    fun generateMine(userId: Int, width: Int, height: Int): Mine

    /**
     * @throws NoEquippedPickaxeException when mining cannot occur due to the lack of an equipped pickaxe.
     * @throws NotInMineSessionException when mining cannot occur due to the lack of an existing mining session.
     */
    fun submitMineAction(userId: Int, x: Int, y: Int): List<MineActionResult>
}

class NoEquippedPickaxeException : IllegalStateException()
class NotInMineSessionException : IllegalStateException()
