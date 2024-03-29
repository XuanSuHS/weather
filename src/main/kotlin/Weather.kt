package top.xuansu.mirai.weather

import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig
import net.mamoe.mirai.console.plugin.jvm.reloadPluginData
import net.mamoe.mirai.utils.info
import java.io.File

object weatherMain : KotlinPlugin(
    JvmPluginDescription(
        id = "top.xuansu.mirai.weather",
        name = "Weather",
        version = "0.1.5-B1",
    ) {
        author("XuanSu")
    }
) {
    var imageFolderPath = ""
    lateinit var imageFolder: File

    override fun onEnable() {
        //----------------------
        //初始化命令
        WeatherCommand().register()
        ConfigureCommand().register()
        TyphoonCommand().register()
        TyphoonImgCommand().register()
        TyphoonForecastCommand().register()
        SeaSurfaceTempCommand().register()
        GetEnsembleCommand().register()
        DevCommand().register()
        //----------------------


        //初始化Config
        reloadPluginConfig(Config)
        reloadPluginData(saveData)

        //检查并创建图片储存文件夹
        imageFolder = dataFolder.resolve("img")
        when {
            imageFolder.exists() -> {
                val fileDeleteResult = deleteFolderContents(imageFolder)
                logger.info("ImgFolder: ${imageFolder.path}")
                if (fileDeleteResult != 0) {
                    logger.info { "Deleted $fileDeleteResult cached files" }
                }
            }

            else -> {
                logger.info("Can't find img folder")
                imageFolder.mkdirs()
                logger.info("Create ImgFolder: ${imageFolder.path}")
            }
        }
        imageFolderPath = imageFolder.path

        //初始化下载图片
        onStart()

        logger.info { "Plugin loaded" }
    }

    override fun onDisable() {
        CommandManager.INSTANCE.unregisterAllCommands(weatherMain)
        logger.info { "Plugin Unloaded" }
    }
}