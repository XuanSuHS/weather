package top.xuansu.mirai.weather

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object Config : AutoSavePluginConfig("config") {
    val commands: List<String> by value(listOf("未来天气"))

    var proxyAddress: String by value("127.0.0.1")
    var proxyPort: Int by value(7890)
}