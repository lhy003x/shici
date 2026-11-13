package com.shici.app.data

import android.content.Context

class Settings(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("shici_settings", Context.MODE_PRIVATE)

    /** 每天的新词数量 */
    var dailyNew: Int
        get() = sp.getInt(KEY_DAILY_NEW, 10)
        set(value) = sp.edit().putInt(KEY_DAILY_NEW, value.coerceIn(5, 50)).apply()

    /** 每日复习上限，0 表示不限 */
    var dailyReviewLimit: Int
        get() = sp.getInt(KEY_REVIEW_LIMIT, 60)
        set(value) = sp.edit().putInt(KEY_REVIEW_LIMIT, value.coerceIn(0, 300)).apply()

    /** 演示加速：把「天」换成「分钟」，答辩时能当场看到间隔变化 */
    var demoMode: Boolean
        get() = sp.getBoolean(KEY_DEMO, false)
        set(value) = sp.edit().putBoolean(KEY_DEMO, value).apply()

    /** 卡片自动发音 */
    var autoSpeak: Boolean
        get() = sp.getBoolean(KEY_AUTO_SPEAK, false)
        set(value) = sp.edit().putBoolean(KEY_AUTO_SPEAK, value).apply()

    /** 首次启动写入内置词库 */
    var seeded: Boolean
        get() = sp.getBoolean(KEY_SEEDED, false)
        set(value) = sp.edit().putBoolean(KEY_SEEDED, value).apply()

    var nickname: String
        get() = sp.getString(KEY_NAME, "同学") ?: "同学"
        set(value) = sp.edit().putString(KEY_NAME, value).apply()

    private companion object {
        const val KEY_DAILY_NEW = "daily_new"
        const val KEY_REVIEW_LIMIT = "review_limit"
        const val KEY_DEMO = "demo_mode"
        const val KEY_AUTO_SPEAK = "auto_speak"
        const val KEY_SEEDED = "seeded"
        const val KEY_NAME = "nickname"
    }
}
