package top.xuansu.mirai.weather


import net.mamoe.mirai.utils.info
import okhttp3.*
import top.xuansu.mirai.weather.weatherMain.logger
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

fun getWeatherURL(): String {
    return "bad"
}


fun getWeatherPic(imagePath: String) {

    //初始化文件获取相关变量
    val fileName = Config.imageName
    val file = File(imagePath, fileName)
    val path = Paths.get(file.path)
    var inputStream: InputStream?
    var fileOutputStream: FileOutputStream?
    val fileReader = ByteArray(4096)
    //如果文件存在则删除
    if (Files.exists(path)) {
        logger.info { "File Exists, deleting it" }
        val result = Files.deleteIfExists(path)
        if (result) {
            logger.info {
                "Delete Successful"
            }
        }
    }


    val url = "https://www.easterlywave.com/media/2023040412/23_210-113_482.png"

    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(Config.proxyAddress, Config.proxyPort)))
        .build()

    val request = Request.Builder()
        .url(url)
        .addHeader("Connection", "keep-alive")
        .build()

    logger.info{"Downloading Image"}
    client.newCall(request).enqueue(object :Callback{
        //图片下载失败
        override fun onFailure(call: Call, e: IOException) {
            logger.info { "Download Error" }
        }

        //图片下载成功
        override fun onResponse(call: Call, response: Response) {
            logger.info {"Download Successful"}
            inputStream = response.body?.byteStream()
            fileOutputStream = FileOutputStream(file)
            //读取的长度
            var read: Int
            var sum: Long = 0
            while (inputStream?.read(fileReader).also { read = it!! } != -1) {
                //写入本地文件
                fileOutputStream!!.write(fileReader, 0, read)
                //获取当前进度
                sum += read.toLong()
            }
            //结束后，刷新清空文件流
            fileOutputStream!!.flush()
            inputStream?.close()
            fileOutputStream?.close()
        }
    })
}