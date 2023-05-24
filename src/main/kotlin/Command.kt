package top.xuansu.mirai.weather

import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.command.getGroupOrNull
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import top.xuansu.mirai.weather.weatherMain.imageFolder
import top.xuansu.mirai.weather.weatherMain.reload
import top.xuansu.mirai.weather.weatherMain.save

class WeatherCommand : SimpleCommand(
    owner = weatherMain,
    primaryName = "weather",
    secondaryNames = arrayOf("天气")
) {
    @Handler
    suspend fun CommandSender.handle(city: String) {
        val group: Group
        if (getGroupOrNull() != null) {
            group = getGroupOrNull()!!
            //如果本群未启用则退出
            if (group.id !in Config.enableGroups) {
                return
            }

            Web.getWeather(city) { err, imageName ->
                if (err != null) {
                    runBlocking { group.sendMessage(err) }
                } else {
                    runBlocking {
                        val img = imageFolder.resolve(imageName).uploadAsImage(group, "png")
                        group.sendMessage(img)
                    }
                }
            }
        }
    }
}

class TyphoonCommand : SimpleCommand(
    owner = weatherMain,
    primaryName = "typhoon",
    secondaryNames = arrayOf("台风")
) {
    @Handler
    suspend fun CommandSender.handle() {
        val group: Group
        if (getGroupOrNull() != null) {
            group = getGroupOrNull()!!
            //如果本群未启用则退出
            if (group.id !in Config.enableGroups) {
                return
            }
            Web.getTyphoon { imageName, err ->
                if (err == null) {
                    runBlocking {
                        val img = imageFolder.resolve(imageName!!).uploadAsImage(group, "png")
                        group.sendMessage(img)
                    }
                } else {
                    runBlocking { sendMessage(err) }
                }
            }
        }
    }
}

class ConfigureCommand : CompositeCommand(
    owner = weatherMain,
    primaryName = "wt"
) {
    @SubCommand("proxy")
    suspend fun CommandSender.setproxy(arg: String, address: String = "") {
        when (arg) {
            "on" -> {
                Web.checkProxy(Config.proxyAddress) { status ->
                    if (status) {
                        runBlocking { sendMessage("代理地址：${Config.proxyAddress}\n测试通过，代理开启") }
                        Web.enableProxy()
                    } else {
                        runBlocking {
                            sendMessage(
                                "代理地址：${Config.proxyAddress}\n" + "测试不通过，请检查"
                            )
                        }
                    }
                }
            }

            "off" -> {
                Web.disableProxy()
                sendMessage("关闭代理访问")
            }

            "set" -> {
                if (address == "") {
                    sendMessage("请输入正确的代理地址")
                    return
                }
                val port = address.split(":")[1].toInt()
                if (port < 1 || port > 65535) {
                    sendMessage("请输入正确的代理地址")
                    return
                }
                Web.checkProxy(address) { status ->
                    if (status) {
                        runBlocking { sendMessage("输入的地址为 $address 代理有效") }
                        Config.proxyAddress = address
                        Config.save()
                    } else {
                        runBlocking { sendMessage("代理地址无效，请检查输入是否有误") }
                    }
                }
            }
        }
    }

    @SubCommand("on")
    suspend fun CommandSender.on() {
        val group: Group
        if (getGroupOrNull() != null) {
            group = getGroupOrNull()!!
            Config.enableGroups.add(group.id)
            Config.save()
            sendMessage("开启本群天气插件")
        } else {
            sendMessage("请在群聊环境下触发")
        }
    }

    @SubCommand("off")
    suspend fun CommandSender.off() {
        val group: Group
        if (getGroupOrNull() != null) {
            group = getGroupOrNull()!!
            Config.enableGroups.remove(group.id)
            Config.save()
            sendMessage("关闭本群天气插件")
        } else {
            sendMessage("请在群聊环境下触发")
        }
    }

    @SubCommand("default")
    suspend fun CommandSender.setDefaultCity(city: String) {
        if (getGroupOrNull() != null) {
            val groupID = getGroupOrNull()!!.id
            Web.getCityNumber(city) { isSuccessful, data ->
                if (isSuccessful) {
                    runBlocking { sendMessage("已将群" + groupID + "的默认城市更改为" + city) }
                } else {
                    runBlocking { sendMessage("出错了：$data") }
                }
            }
        } else {
            sendMessage("请在群聊环境下触发")
        }
    }

    @SubCommand("status")
    suspend fun CommandSender.status() {
        if (getGroupOrNull() != null) {
            val group = getGroupOrNull()!!
            if (group.id in Config.enableGroups) {
                var message = "本群已启用天气插件\n"

                message += if (Config.isProxyEnabled) {
                    "当前代理地址：${Config.proxyAddress}\n"
                } else {
                    "代理未启用\n"
                }

                message += if (Data.defaultCityPerGroup[group.id] != null) {
                    "默认城市：${Data.defaultCityPerGroup[group.id]}"
                } else {
                    "默认城市未设置"
                }
                sendMessage(message)
            } else {
                sendMessage("本群未启用天气插件")
            }
        }
    }

    @SubCommand("resetCookie")
    suspend fun CommandSender.resetCookie() {
        Web.getCookie { err ->
            if (err == null) {
                runBlocking { sendMessage("Cookie更新完毕") }
            } else {
                runBlocking { sendMessage("Cookie更新时出错：$err") }
            }
        }
    }

    @SubCommand("reload")
    suspend fun CommandSender.configReload() {
        Config.reload()
        sendMessage("配置文件重载成功")
    }

    @SubCommand("dev")
    suspend fun CommandSender.dev(city: String) {
        Web.getWeather(city) { err, imageName ->
            if (err != null) {
                runBlocking { sendMessage(err) }
            } else {
                runBlocking { sendMessage("Finished, imageName:$imageName") }
            }
        }
    }
}