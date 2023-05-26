package top.xuansu.mirai.weather

import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.command.getGroupOrNull
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.PlainText
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

            Web.CityWeatherFunc.getWeather(city) { err, imageName ->
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
    secondaryNames = arrayOf("台风", "ty")
) {
    @Handler
    suspend fun CommandSender.handle(codeIn: String = "") {
        val group: Group
        if (getGroupOrNull() != null) {
            group = getGroupOrNull()!!
            //如果本群未启用则退出
            if (group.id !in Config.enableGroups) {
                return
            }

            //输出帮助信息
            if (codeIn.lowercase() in setOf("help", "帮助")) {
                var message = "显示当前活动台风基础信息\n"
                    .plus("命令格式：/typhoon [台风代号]\n")
                    .plus("当前可用台风：")
                val stormList = Data.TyphoonData.keys.toList()
                stormList.forEach {
                    message += "[$it] "
                }
                sendMessage(message)
                return
            }

            //更新台风信息
            var isError = false
            Web.TyphoonFunc.getTyphoonData { status, data ->
                if (!status) {
                    runBlocking { sendMessage("更新信息时出错：$data") }
                    isError = true
                }
            }
            if (isError) {
                return
            }


            var message = ""
            //检查台风代号可用性
            val codeCheckResult = Web.TyphoonFunc.checkTyphoonCode(codeIn)
            val typhoonCode = when (codeCheckResult.first) {
                true -> {
                    codeCheckResult.second
                }

                false -> {
                    sendMessage(codeCheckResult.second)
                    return
                }
            }

            //加入台风代号文字信息
            val typhoonData = Data.TyphoonData[typhoonCode]!!
            message += "$typhoonCode.${typhoonData.name}\n"
                .plus("风速：${typhoonData.windSpeed}\n")
                .plus("气压：${typhoonData.pressure}\n")
                .plus("位置：${typhoonData.longitude} ${typhoonData.latitude}\n")

            //加入台风图片
            //如该台风不存在图片则跳过
            if (!Data.TyphoonData[typhoonCode]!!.isSatelliteTarget) {
                message += "该台风无图片"
                sendMessage(message)
                return
            }

            val imgType = Config.defaultImgType
            Web.TyphoonFunc.getTyphoonSatePic(typhoonCode, imgType) { status, data ->
                if (status) {
                    runBlocking {
                        val img = imageFolder.resolve(data!!).uploadAsImage(group, "png")
                        group.sendMessage(PlainText(message) + img)
                    }
                } else {
                    runBlocking { sendMessage("下载图片时出错：$data") }
                }
            }
        }
    }
}

class TyphoonImgCommand : SimpleCommand(
    owner = weatherMain,
    primaryName = "typhoon-img",
    secondaryNames = arrayOf("台风图片", "ty-img", "tyImg")
) {
    @Handler
    suspend fun CommandSender.handle(codeIn: String = "", imageType: String = "") {

        if (getGroupOrNull() == null) {
            sendMessage("请在群聊下执行")
            return
        }
        val group = getGroupOrNull()!!
        //如果本群未启用则退出
        if (group.id !in Config.enableGroups) {
            return
        }

        //输出帮助信息
        if (codeIn.lowercase() in setOf("help", "帮助")) {
            var message = "显示当前活动台风卫星图片\n"
                .plus("命令格式：/typhoon-img [台风代号] [图片类型]\n")
                .plus("当前可用台风：")
            val stormList = Data.TyphoonData.keys.toList()
            stormList.forEach {
                if (Data.TyphoonData[it]!!.isSatelliteTarget) {
                    message += "[$it] "
                }
            }
            sendMessage(message)
            return
        }

        //更新台风信息
        var isError = false
        Web.TyphoonFunc.getTyphoonData { status, data ->
            if (!status) {
                runBlocking { sendMessage("更新信息时出错：$data") }
                isError = true
            }
        }
        if (isError) {
            return
        }

        //检查台风代号可用性
        val codeCheckResult = Web.TyphoonFunc.checkTyphoonCode(codeIn)
        val typhoonCode = when (codeCheckResult.first) {
            true -> {
                codeCheckResult.second
            }

            false -> {
                sendMessage(codeCheckResult.second)
                return
            }
        }

        //检查图片搜索类型可用性
        val imgTypeCheckResult = Web.TyphoonFunc.checkImgType(imageType)
        val imgType = when (imgTypeCheckResult.first) {
            true -> {
                imgTypeCheckResult.second
            }

            false -> {
                sendMessage(imgTypeCheckResult.second)
                return
            }
        }

        Web.TyphoonFunc.getTyphoonSatePic(typhoonCode, imgType) { status, data ->
            if (status) {
                runBlocking {
                    val img = imageFolder.resolve(data!!).uploadAsImage(group, "png")
                    group.sendMessage(img)
                }
            } else {
                runBlocking { sendMessage("出错了：$data") }
            }
        }
    }
}


class TyphoonForecastCommand : SimpleCommand(
    owner = weatherMain,
    primaryName = "typhoon-forecast",
    secondaryNames = arrayOf("台风预报", "ty-fore", "ty-forecast", "tyFore")
) {
    @Handler
    suspend fun CommandSender.handle(code: String = "", imageType: String = "") {
        if (getGroupOrNull() == null) {
            sendMessage("请在群聊下执行")
            return
        }

        //如果本群未启用则退出
        val group = getGroupOrNull()!!
        if (group.id !in Config.enableGroups) {
            return
        }
        var isError = false
        //更新台风信息
        Web.TyphoonFunc.getTyphoonData { status, data ->
            if (!status) {
                runBlocking { sendMessage("更新信息时出错：$data") }
                isError = true
            }
        }
        if (isError) {
            return
        }

        //检查台风代号可用性
        val codeCheckResult = Web.TyphoonFunc.checkTyphoonCode(code)
        val typhoonCode = when (codeCheckResult.first) {
            true -> {
                codeCheckResult.second
            }

            false -> {
                sendMessage(codeCheckResult.second)
                return
            }
        }
        //TODO:预报
    }
}

class SeaSurfaceTempCommand : SimpleCommand(
    owner = weatherMain,
    primaryName = "sst",
    secondaryNames = arrayOf("海温")
) {
    @Handler
    suspend fun CommandSender.handle(areaIn: String = "") {
        val group: Group
        if (getGroupOrNull() != null) {
            group = getGroupOrNull()!!
            //如果本群未启用则退出
            if (group.id !in Config.enableGroups) {
                return
            }

            //检查海域输入
            val area = when (areaIn) {
                "" -> {
                    Config.defaultSeaArea
                }

                !in Data.seaforUse -> {
                    runBlocking { sendMessage("该海域不存在") }
                    return
                }

                else -> {
                    areaIn
                }
            }

            Web.SSTFunc.getSSTbyRTOFS(area) { imageName, err ->
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
                Web.ProxyFunc.checkProxy(Config.proxyAddress) { status ->
                    if (status) {
                        runBlocking { sendMessage("代理地址：${Config.proxyAddress}\n测试通过，代理开启") }
                        Web.ProxyFunc.enableProxy()
                        Config.isProxyEnabled = true
                        Config.save()
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
                Web.ProxyFunc.disableProxy()
                Config.isProxyEnabled = false
                Config.save()
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
                Web.ProxyFunc.checkProxy(address) { status ->
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

    @SubCommand("city")
    suspend fun CommandSender.setDefaultCity(arg: String) {
        if (getGroupOrNull() != null) {
            val groupID = getGroupOrNull()!!.id
            Web.CityWeatherFunc.getCityNumber(arg) { isSuccessful, data ->
                if (isSuccessful) {
                    runBlocking { sendMessage("已将群" + groupID + "的默认城市更改为" + arg) }
                } else {
                    runBlocking { sendMessage("出错了：$data") }
                }
            }
        } else {
            sendMessage("请在群聊环境下触发")
        }
    }

    @SubCommand("sea")
    suspend fun CommandSender.setDefaultSea(arg: String) {
        if (getGroupOrNull() != null) {
            val groupID = getGroupOrNull()!!.id
            Web.CityWeatherFunc.getCityNumber(arg) { isSuccessful, data ->
                if (isSuccessful) {
                    runBlocking { sendMessage("已将群" + groupID + "的默认城市更改为" + arg) }
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

                message += if (saveData.defaultCityPerGroup[group.id] != null) {
                    "默认城市：${saveData.defaultCityPerGroup[group.id]}"
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
        Web.TyphoonFunc.getTyphoonData { status, err ->
            if (status) {
                runBlocking {
                    sendMessage("Success")
                    var message = ""
                    val stormCount = Data.TyphoonData.count()
                    var i = 0
                    for ((code, data) in Data.TyphoonData) {
                        i += 1
                        message += "代号：$code\n"
                            .plus("名字：${data.name}\n")
                            .plus("地区：${data.basin}\n")
                            .plus("位置：${data.longitude} ${data.latitude}\n")
                            .plus("中心最大风速：${data.windSpeed}\n")
                            .plus("中心最低气压：${data.pressure}\n")
                            .plus("是否有卫星图片：${data.isSatelliteTarget}")
                        if (i < stormCount) {
                            message += "\n\n"
                        }
                    }
                    sendMessage(message)
                }
            } else {
                runBlocking { sendMessage("Err: $err") }
            }
        }
    }
}

class DevCommand : SimpleCommand(
    owner = weatherMain,
    primaryName = "dev"
) {
    @Handler
    suspend fun CommandSender.handle(code: String, picType: String) {
        if (!Data.TyphoonData.containsKey(code)) {
            sendMessage("err")
            return
        } else {
            sendMessage("找到了$code")
        }

        Web.TyphoonFunc.getTyphoonSatePic(code, picType.uppercase()) { status, data ->
            if (status) {
                runBlocking { sendMessage("Finished: $data") }
            } else {
                runBlocking { sendMessage("Err: $data") }
            }
        }
    }
}