package top.xuansu.mirai.weather

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object Config : AutoSavePluginConfig("config") {
    var isProxyEnabled: Boolean by value(false)
    var proxyAddress: String by value("127.0.0.1:7890")
    var defaultEnsemble = "ec"
    val enableGroups: MutableSet<Long> by value(mutableSetOf())
    const val defaultSeaArea = "wpac"
    const val defaultImgType = "VIS"
}