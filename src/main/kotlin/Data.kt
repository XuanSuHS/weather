package top.xuansu.mirai.weather

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value

object Data : AutoSavePluginData("data") {
    var webCookie: String by value("")
    var webCookieValue: String by value("")
    val defaultCityPerGroup: MutableMap<Long, String> by value(mutableMapOf())
}