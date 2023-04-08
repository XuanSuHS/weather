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
import top.xuansu.mirai.weather.weatherMain.save

class WeatherCommand : SimpleCommand(
    owner = weatherMain,
    primaryName = "weather",
    secondaryNames = arrayOf("天气")
) {
    @Handler
    suspend fun CommandSender.handle() {
        getWeatherPic()
        delay(700)
        val group: Group
        val img: Image
        if (getGroupOrNull() != null) {
            group = getGroupOrNull()!!
            img = imageFolder.resolve(weatherMain.imageName).uploadAsImage(group, "png")
            group.sendMessage(img)
        }
    }
}

class ProxySetCommand : CompositeCommand(
    owner = weatherMain,
    primaryName = "wt-set"
) {
    @SubCommand("address")
    suspend fun CommandSender.address(arg: String) {
        val addressRegex = "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\$".toRegex()
        if (!addressRegex.containsMatchIn(arg)) {
            sendMessage("请输入正确的IPv4地址")
            return
        }
        Config.proxyAddress = arg
        Config.save()
    }

    @SubCommand("port")
    suspend fun CommandSender.port(arg: Int) {
        if (arg <= 0 || arg >= 65536) {
            sendMessage("请输入正确的端口")
            return
        }
        Config.proxyPort = arg
        Config.save()
    }
}