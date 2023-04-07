package top.xuansu.mirai.weather

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import net.mamoe.mirai.utils.info

object weatherMain : KotlinPlugin(
    JvmPluginDescription(
        id = "top.xuansu.mirai.weather",
        name = "Weather",
        version = "0.1.0",
    ) {
        author("XuanSu")
    }
) {
    override fun onEnable() {
        //初始化命令
        WeatherCommand().register()

        //初始化Config
        reloadPluginConfig(Config)

        //检查并创建图片储存文件夹
        val imageFolder = dataFolder.resolve("img")
        when {
            imageFolder.exists() -> logger.info("ImgFolder: ${imageFolder.path}")
            else -> {
                logger.info("Can't find img folder")
                imageFolder.mkdirs()
                logger.info("Create ImgFolder: ${imageFolder.path}")
            }
        }

        globalEventChannel().subscribeAlways<GroupMessageEvent> {
            Config.commands.forEachIndexed { _, cmd ->
                if (message.content.startsWith(cmd)) {
                    CoroutineScope(Dispatchers.IO).launch {
                        // 指令列表的第一个指令默认为随即调用API
                        // 指令列表的非第一个指令依照顺序依次调用对应的API

                        //val url = getWeatherURL()

                        // 获取图片
                        val img = imageFolder.resolve(Config.imageName).uploadAsImage(group, "png")
                        group.sendMessage(img)
                    }
                }
            }
        }

        logger.info { "Plugin loaded" }
    }
}