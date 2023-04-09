package top.xuansu.mirai.weather

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
import java.io.File

object weatherMain : KotlinPlugin(
    JvmPluginDescription(
        id = "top.xuansu.mirai.weather",
        name = "Weather",
        version = "0.1.1",
    ) {
        author("XuanSu")
    }
) {
    var imageFolderPath = ""
    const val imageName = "WeatherImg.png"
    lateinit var imageFolder: File

    override fun onEnable() {
        //----------------------
        //初始化命令
        WeatherCommand().register()
        ConfigureCommand().register()
        //----------------------


        //初始化Config
        reloadPluginConfig(Config)

        //检查并创建图片储存文件夹
        imageFolder = dataFolder.resolve("img")
        when {
            imageFolder.exists() -> logger.info("ImgFolder: ${imageFolder.path}")
            else -> {
                logger.info("Can't find img folder")
                imageFolder.mkdirs()
                logger.info("Create ImgFolder: ${imageFolder.path}")
            }
        }
        imageFolderPath = imageFolder.path

        //初始化下载图片
        CoroutineScope(Dispatchers.IO).launch {
            Web.getWeather()
        }

        //监听文字寻找触发指令
        globalEventChannel().subscribeAlways<GroupMessageEvent> {
            Config.commands.forEachIndexed { _, cmd ->
                if (message.content.startsWith(cmd)) {

                    //如果本群未启用则退出
                    if (group.id !in Config.enableGroups) {
                        return@subscribeAlways
                    }

                    // 上传图片
                    Web.getWeather()
                    delay(700)
                    val img = imageFolder.resolve(imageName).uploadAsImage(group, "png")
                    group.sendMessage(img)
                }
            }
        }

        logger.info { "Plugin loaded" }
    }
}