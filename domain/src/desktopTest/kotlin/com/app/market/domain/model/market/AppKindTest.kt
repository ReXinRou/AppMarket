package com.app.market.domain.model.market

import kotlin.test.Test
import kotlin.test.assertEquals

class AppKindTest {
    @Test
    fun blankCategoryIsUnknown() {
        assertEquals(AppKind.UNKNOWN, classifyKindFromCategory(""))
        assertEquals(AppKind.UNKNOWN, classifyKindFromCategory("   "))
    }

    @Test
    fun gameCategoryNamesClassifyAsGame() {
        listOf("游戏", "休闲游戏", "网络游戏", "角色扮演", "动作射击", "Games", "GAME")
            .forEach { assertEquals(AppKind.GAME, classifyKindFromCategory(it), it) }
    }

    @Test
    fun nonGameCategoryNamesClassifyAsApp() {
        listOf("社交通讯", "影音娱乐", "便捷生活", "购物比价", "Tools", "Productivity")
            .forEach { assertEquals(AppKind.APP, classifyKindFromCategory(it), it) }
    }

    @Test
    fun kindIsNotComplementaryForUnknown() {
        // UNKNOWN must not collapse into APP: the display layer decides, and guessing is what
        // previously mis-filed games.
        assertEquals(AppKind.UNKNOWN, classifyKindFromCategory(""))
    }
}
