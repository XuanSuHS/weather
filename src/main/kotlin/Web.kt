package top.xuansu.mirai.weather


import com.google.gson.JsonParser
import net.mamoe.mirai.utils.info
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import top.xuansu.mirai.weather.weatherMain.imageFolderPath
import top.xuansu.mirai.weather.weatherMain.imageName
import top.xuansu.mirai.weather.weatherMain.logger
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

fun getWeatherPic() {

    //
    //  本部分为获取图片URL部分
    //

    //创建OkHttpClient
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(Config.proxyAddress, Config.proxyPort)))
        .build()

    //获取Cookie
    val requestForCookie = Request.Builder()
        .url("https://www.easterlywave.com/weather")
        .addHeader("Connection", "keep-alive")
        .addHeader("Referer", "https://www.easterlywave.com/weather/")
        .get()
        .build()
    val responseForCookie = client.newCall(requestForCookie).execute()

    // 设置Cookie值
    val urlCookie = if (responseForCookie.header("Set-Cookie") != null)
        (responseForCookie.header("Set-Cookie")!!)
    else {
        "null"
    }

    val urlCookieValue = urlCookie.split(";")[0].replace("csrftoken=", "")

    //获取图片URL的文件地址部分
    val mediaType = "application/json;charset=utf-8".toMediaTypeOrNull()
    val requestBody = "{\"content\":\"59287\"}".toRequestBody(mediaType)
    val requestForPicURL = Request.Builder()
        .url("https://www.easterlywave.com/action/weather/plot")
        .header("Cookie", urlCookie)
        .addHeader("Content-Type", "application/json")
        .addHeader("Connection", "keep-alive")
        .addHeader("Referer", "https://www.easterlywave.com/weather/")
        .addHeader("x-csrftoken", urlCookieValue)
        .post(requestBody)
        .build()

    //处理返回的JSON
    val responseForPicURL = client.newCall(requestForPicURL).execute()
    val responseSRC =
        JsonParser.parseString(responseForPicURL.body?.string()).asJsonObject.get("src").toString().replace("\"", "")
    val picURL = "https://www.easterlywave.com".plus(responseSRC)

    //
    // End of the Part
    //

    // -----------------------------------------------------------------------------------------

    //
    //   本部分为下载图片部分
    //

    //初始化文件获取相关变量
    val file = File(imageFolderPath, imageName)
    val path = Paths.get(file.path)
    var inputStream: InputStream

    //创建Request
    val request = Request.Builder()
        .url(picURL)
        .addHeader("Connection", "keep-alive")
        .build()

    logger.info { "Downloading Image" }
    client.newCall(request).enqueue(object : Callback {
        //图片下载失败
        override fun onFailure(call: Call, e: IOException) {
            logger.info { "Download Error" }
        }

        //图片下载成功
        override fun onResponse(call: Call, response: Response) {
            logger.info { "Download Successful" }
            if (response.body?.byteStream() != null) {
                inputStream = response.body!!.byteStream()
                Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING)
                inputStream.close()
            }
        }
    })
    //
    //   End of the Part
    //
    return
}