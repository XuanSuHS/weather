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
    secondaryNames = arrayOf("天气"),
    description = "获取城市天气信息"
) {
    @Handler
    suspend fun CommandSender.handle(city: String, number: String = "") {
        val group: Group
        if (getGroupOrNull() != null) {
            group = getGroupOrNull()!!
            //如果本群未启用则退出
            if (group.id !in Config.enableGroups) {
                return
            }

            val getWeatherResponse = Web.CityWeatherFunc.getWeather(city, number)
            if (getWeatherResponse.first) {
                runBlocking {
                    val imageName = getWeatherResponse.second
                    val img = imageFolder.resolve(imageName).uploadAsImage(group, "png")
                    group.sendMessage(img)
                }
            } else {
                runBlocking { group.sendMessage(getWeatherResponse.second) }
            }
        }
    }
}

class TyphoonCommand : SimpleCommand(
    owner = weatherMain,
    primaryName = "typhoon",
    secondaryNames = arrayOf("台风", "ty"),
    description = "获取台风信息"
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

            //更新台风信息
            val typhoonDataResponse = Web.TyphoonFunc.getTyphoonData()
            if (!typhoonDataResponse.first) {
                runBlocking { sendMessage("更新信息时出错：$${typhoonDataResponse.second}") }
                return
            }

            //特殊输入则输出帮助信息
            when (codeIn.lowercase()) {
                //输出帮助信息
                in setOf("help", "帮助") -> {
                    var message = "显示当前活动台风基础信息\n"
                        .plus("命令格式：/typhoon [台风代号]\n")
                        .plus("当前可用台风：")
                    val typhoonList = Data.TyphoonData.keys.toList()
                    typhoonList.forEach {
                        message += "[$it] "
                    }
                    sendMessage(message)
                    return
                }

                //输出台风列表
                in setOf("list", "列表") -> {
                    var message = "当前可用台风"
                    val typhoonList = Data.TyphoonData.keys.toList()
                    typhoonList.forEach {
                        message += "[$it] "
                    }
                    sendMessage(message)
                    return
                }
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
            val getTyphoonSatePicResponse = Web.TyphoonFunc.getTyphoonSatePic(typhoonCode, imgType)
            if (getTyphoonSatePicResponse.first) {
                runBlocking {
                    val img = imageFolder.resolve(getTyphoonSatePicResponse.second).uploadAsImage(group, "png")
                    group.sendMessage(PlainText(message) + img)
                }
            } else {
                runBlocking { sendMessage("下载图片时出错：${getTyphoonSatePicResponse.second}") }
            }
        }
    }
}

class TyphoonImgCommand : SimpleCommand(
    owner = weatherMain,
    primaryName = "typhoon-img",
    secondaryNames = arrayOf("台风图片", "ty-img", "tyImg"),
    description = "获取台风卫星图片"
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

        //更新台风信息
        val typhoonDataResponse = Web.TyphoonFunc.getTyphoonData()
        if (!typhoonDataResponse.first) {
            runBlocking { sendMessage("更新信息时出错：$${typhoonDataResponse.second}") }
            return
        }

        //特殊输入则输出特定信息
        when (codeIn.lowercase()) {
            in setOf("help", "帮助") -> {
                var message = "显示当前活动台风卫星图片\n"
                    .plus("命令格式：/typhoon-img [台风代号] [图片类型]\n")
                    .plus("当前可用台风：")
                val typhoonList = Data.TyphoonData.keys.toList()
                typhoonList.forEach {
                    if (Data.TyphoonData[it]!!.isSatelliteTarget) {
                        message += "[$it] "
                    }
                }
                sendMessage(message)
                return
            }

            in setOf("list", "列表") -> {
                var message = "当前可用台风："
                val typhoonList = Data.TyphoonData.keys.toList()
                typhoonList.forEach {
                    if (Data.TyphoonData[it]!!.isSatelliteTarget) {
                        message += "[$it] "
                    }
                }
                sendMessage(message)
                return
            }
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

        if (!Data.TyphoonData[typhoonCode]!!.isSatelliteTarget) {
            val message = "该台风无图片"
            sendMessage(message)
            return
        }

        val getSatePicResponse = Web.TyphoonFunc.getTyphoonSatePic(typhoonCode, imgType)
        if (getSatePicResponse.first) {
            runBlocking {
                val img = imageFolder.resolve(getSatePicResponse.second).uploadAsImage(group, "png")
                group.sendMessage(img)
            }
        } else {
            runBlocking { sendMessage("出错了：${getSatePicResponse.second}") }
        }
    }
}


class TyphoonForecastCommand : SimpleCommand(
    owner = weatherMain,
    primaryName = "typhoon-ensemble",
    secondaryNames = arrayOf("台风预报", "ty-ensemble", "tyEns", "tyens")
) {
    @Handler
    suspend fun CommandSender.handle(code: String = "") {
        if (getGroupOrNull() == null) {
            sendMessage("请在群聊下执行")
            return
        }

        //如果本群未启用则退出
        val group = getGroupOrNull()!!
        if (group.id !in Config.enableGroups) {
            return
        }

        //更新台风信息
        val typhoonDataResponse = Web.TyphoonFunc.getTyphoonData()
        if (!typhoonDataResponse.first) {
            runBlocking { sendMessage("更新信息时出错：$${typhoonDataResponse.second}") }
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

        val getECForecastResult = Web.TyphoonFunc.getECEnsemble(typhoonCode)
        if (getECForecastResult.first) {
            val foreCastResult = getECForecastResult.second.split("||")
            runBlocking {
                val typhoonImg = imageFolder.resolve(foreCastResult[0]).uploadAsImage(group, "png")
                val seaImg = imageFolder.resolve(foreCastResult[1]).uploadAsImage(group, "png")
                group.sendMessage(typhoonImg + "\n" + seaImg)
            }
        } else {
            runBlocking { group.sendMessage("出错了：${getECForecastResult.second}") }
        }
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

                in Data.seaforUse -> {
                    areaIn
                }

                else -> {
                    runBlocking { sendMessage("该海域不存在") }
                    return
                }
            }

            val getSSTResponse = Web.SSTFunc.getSST(area)
            if (getSSTResponse.first) {
                runBlocking {
                    val img = imageFolder.resolve(getSSTResponse.second).uploadAsImage(group, "png")
                    group.sendMessage(img)
                }
            } else {
                runBlocking { sendMessage(getSSTResponse.second) }
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

            "check" -> {
                if (!Config.isProxyEnabled) {
                    runBlocking { sendMessage("当前未开启代理") }
                    return
                }
                Web.ProxyFunc.checkProxy(Config.proxyAddress) { status ->
                    if (status) {
                        runBlocking { sendMessage("当前地址为 ${Config.proxyAddress} 代理有效") }
                    } else {
                        runBlocking { sendMessage("当前地址为 ${Config.proxyAddress} 代理地址无效，请检查") }
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

                sendMessage(message)
            } else {
                sendMessage("本群未启用天气插件")
            }
        }
    }

    @SubCommand("resetCookie")
    suspend fun CommandSender.resetCookie() {
        val getCookieResult = Web.getCookie()
        if (getCookieResult.first) {
            runBlocking { sendMessage("Cookie更新完毕") }
        } else {
            runBlocking { sendMessage("Cookie更新时出错：${getCookieResult.second}") }
        }
    }

    @SubCommand("reload")
    suspend fun CommandSender.configReload() {
        Config.reload()
        val fileDeleteResult = deleteFolderContents(imageFolder)
        if (fileDeleteResult != 0) {
            sendMessage("清除了${fileDeleteResult}张缓存图片文件")
        }
        sendMessage("配置文件重载成功")
    }

    @SubCommand("dev")
    suspend fun CommandSender.dev() {
        sendMessage("NOPE")
    }
}

class GetEnsembleCommand : SimpleCommand(
    owner = weatherMain,
    primaryName = "getensemble",
    secondaryNames = arrayOf("getens")
) {
    @Handler
    suspend fun CommandSender.handle(arg: String = "") {
        var ens = arg.lowercase()
        if (ens == "") {
            ens = Config.defaultEnsemble
        }
        when (ens.lowercase()) {
            in arrayOf("ec", "ecmwf", "ecens") -> {
                val ecResult = Web.TyphoonFunc.getECTime()
                if (!ecResult.first) {
                    sendMessage("获取ECMWF基准时间失败:${ecResult.second}")
                }
                var messageOut = "当前ECMWF可用基准时间："
                for (i in Data.ecEnsembleTime) {
                    messageOut += "\n${i.key}:"
                    for (j in i.value) {
                        messageOut += " $j"
                    }
                }
                sendMessage(messageOut)
            }

            in arrayOf("gfs", "gefs") -> {
                sendMessage("还没做捏")
            }

            else -> {
                sendMessage("暂不支持此机构或输入错误")
            }
        }
    }
}

class DevCommand : SimpleCommand(
    owner = weatherMain,
    primaryName = "dev",
) {
    @Handler
    suspend fun CommandSender.handle(code: String) {
        sendMessage("${code}NOPE")
    }
}