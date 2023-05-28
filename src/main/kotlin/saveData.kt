package top.xuansu.mirai.weather

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value

object saveData : AutoSavePluginData("data") {
    var webCookie: String by value("")
    var webCookieValue: String by value("")
}