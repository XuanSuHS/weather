package top.xuansu.mirai.weather

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.command.getGroupOrNull
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.Image
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
        val img: Image
        if (getGroupOrNull() != null) {
            group = getGroupOrNull()!!
            //如果本群未启用则退出
            if (group.id !in Config.enableGroups) {
                return
            }
            val cityPair = Web.getCityNumber(city)
            //判断城市输入是否有误
            when (cityPair.first) {
                1 -> {
                    sendMessage("未找到该城市，请检查输入是否有误")
                    return
                }

                2 -> {
                    sendMessage("选项过多，请输入更精确的值")
                    return
                }
            }

            Web.getWeather(city)
            val imageName = cityPair.second.toString() + ".png"
            delay(1500)
            img = imageFolder.resolve(imageName).uploadAsImage(group, "png")
            group.sendMessage(img)
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
            //val webResponse = Web.getTyphoon()
            //if (webResponse.first) {
            //    val imageName = webResponse.second
            //    delay(1500)
            //    img = imageFolder.resolve(imageName).uploadAsImage(group, "png")
            //    group.sendMessage(img)
            //}
            //else {
            //    val message = "执行出错，错误代码为：" + webResponse.second
            //    group.sendMessage(message)
            //}
        }
    }
}

class ConfigureCommand : CompositeCommand(
    owner = weatherMain,
    primaryName = "wt"
) {
    @SubCommand("setproxy")
    suspend fun CommandSender.setproxy(arg: String) {

        //分离地址与端口
        val port = arg.split(":")[1].toInt()

        //检查端口是否合法
        if (port < 1 || port > 65535) {
            sendMessage("请输入正确的端口")
            return
        }

        //检查代理地址是否有效
        Web.checkProxy(arg) { status ->
            if (status) {
                runBlocking { sendMessage("输入的地址为 $arg 代理有效") }
                Config.proxyAddress = arg
                Config.save()
            } else {
                runBlocking { sendMessage("代理地址无效，请检查输入是否有误") }
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
            val groupid = getGroupOrNull()!!.id
            when (Web.getCityNumber(city).first) {
                0 -> {
                    Data.defaultCityPerGroup[groupid] = city
                    Data.save()
                    sendMessage("已将群" + groupid + "的默认城市更改为" + city)
                }

                1 -> {
                    sendMessage("未找到该城市，请检查输入是否有误")
                }

                2 -> {
                    sendMessage("选项过多，请输入更精确的值")
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
                var message = "本群已启用天气插件\n当前代理地址：${Config.proxyAddress}\n"
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
    suspend fun CommandSender.dev() {
        Web.getTyphoon { time, err ->
            if (err == null) {
                runBlocking { sendMessage("执行成功，时间：$time") }
            } else {
                runBlocking { sendMessage(err) }
            }
        }
    }
}