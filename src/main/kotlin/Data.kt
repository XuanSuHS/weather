package top.xuansu.mirai.weather

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value

object Data : AutoSavePluginData("data") {
    var webCookie: String by value("")
    var webCookieValue: String by value("")
    val seaforUse: Map<String, String> by value(
        mapOf(
            Pair("wpac", "西太平洋"),
            Pair("epac", "东太平洋"),
            Pair("natl", "北大西洋"),
            Pair("nio", "北印度洋"),
            Pair("sio", "南印度洋"),
            Pair("aus", "南太平洋"),
            Pair("eastasia", "东亚地区"),
            Pair("micronesia", "中太平洋/密克罗尼西亚")
        )
    )
    val defaultCityPerGroup: MutableMap<Long, String> by value(mutableMapOf())
}