package top.xuansu.mirai.weather

import kotlinx.coroutines.delay
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
        val img: Image
        if (getGroupOrNull() != null) {
            group = getGroupOrNull()!!
            //如果本群未启用则退出
            if (group.id !in Config.enableGroups) {
                return
            }

            val imageName = Web.getTyphoon()
            delay(1500)
            img = imageFolder.resolve(imageName).uploadAsImage(group, "png")
            group.sendMessage(img)
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

        Config.proxyAddress = arg
        Config.save()
    }

    @SubCommand("getproxy")
    suspend fun CommandSender.getproxy() {
        sendMessage("当前代理地址：" + Config.proxyAddress)
    }

    @SubCommand("enable")
    suspend fun CommandSender.enable() {
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

    @SubCommand("disable")
    suspend fun CommandSender.disable() {
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
    suspend fun CommandSender.setdefaultcity(city: String) {
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
        val group: Group
        if (getGroupOrNull() != null) {
            group = getGroupOrNull()!!
            if (group.id in Config.enableGroups) {
                sendMessage("本群已开启天气插件\n本群默认城市为 " + Data.defaultCityPerGroup[group.id])
            } else {
                sendMessage("本群未开启天气插件")
            }
        } else {
            sendMessage("请在群聊环境下触发")
        }
    }

    @SubCommand("resetcookie")
    suspend fun CommandSender.resetcookie() {
        Web.getCookie()
        sendMessage("Cookie更新完毕")
    }

    @SubCommand("reload")
    suspend fun CommandSender.configreload() {
        Config.reload()
        sendMessage("config重载成功")
    }
}