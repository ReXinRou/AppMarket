package com.app.market.domain.model.market

/**
 * 应用 / 游戏的顶层归类。
 *
 * 数据源优先：能由来源的结构化字段（分类名、类型、标签）明确判定时用 GAME / APP；
 * 判定不出来时用 UNKNOWN，交给展示层按专区决定是否收录。刻意不使用「非游戏即应用」的
 * 互补判断——一个条目可能两边都不属于。
 */
enum class AppKind {
    GAME,
    APP,
    UNKNOWN,
}

/**
 * 从分类名推断顶层归类，供没有结构化类型字段的来源兜底（华为 kindName、OPPO/三星/豌豆荚
 * 的分类名、TapTap 的标签串）。只在分类名明确命中游戏分类时判为 GAME；其余非空分类视为 APP。
 * 分类名为空时返回 UNKNOWN，交由展示层按专区决定。
 */
fun classifyKindFromCategory(category: String): AppKind {
    val value = category.lowercase()
    if (value.isBlank()) return AppKind.UNKNOWN
    return if (GAME_CATEGORY_MARKERS.any(value::contains)) AppKind.GAME else AppKind.APP
}

private val GAME_CATEGORY_MARKERS = listOf(
    "游戏", "手游", "网游", "单机",
    "棋牌", "益智", "休闲", "消除", "跑酷", "捕鱼", "塔防", "卡牌", "自走棋", "战棋",
    "动作", "射击", "角色扮演", "冒险", "模拟", "经营", "策略", "竞速", "体育", "竞技",
    "moba", "rpg", "沙盒", "放置", "回合制", "开放世界", "二次元", "传奇", "修仙",
    "game", "games", "gaming",
)
