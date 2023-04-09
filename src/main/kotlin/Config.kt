package top.xuansu.mirai.weather

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object Config : AutoSavePluginConfig("config") {
    val commands: List<String> by value(listOf("未来天气", "天气如何"))
    var proxyAddress: String by value("127.0.0.1:7890")
    val enableGroups: MutableSet<Long> by value(mutableSetOf())
}