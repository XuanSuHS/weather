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


object Web {
    private val proxyAddress = Config.proxyAddress.split(":")[0]
    private val proxyPort = Config.proxyAddress.split(":")[1].toInt()

    //创建OkHttpClient
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyAddress, proxyPort)))
        .build()

    fun getWeather() {
        getCookie()
        val url = getWeatherURL()
        getWeatherPic(url)
    }

    //获取Cookie
    private fun getCookie() {

        //获取Cookie
        val requestForCookie = Request.Builder()
            .url("https://www.easterlywave.com/weather")
            .addHeader("Connection", "keep-alive")
            .addHeader("Referer", "https://www.easterlywave.com/weather/")
            .get()
            .build()
        val responseForCookie = client.newCall(requestForCookie).execute()

        // 设置Cookie值
        Data.webCookie = if (responseForCookie.header("Set-Cookie") != null)
            (responseForCookie.header("Set-Cookie")!!)
        else {
            "null"
        }
        Data.webCookieValue = Data.webCookie.split(";")[0].replace("csrftoken=", "")
    }

    //获取城市对应数字
    fun getCityNumber(city: String): Pair<Int, Int> {
        val mediaType = "application/json;charset=utf-8".toMediaTypeOrNull()
        val requestBody = "{\"content\":\"$city\"}".toRequestBody(mediaType)
        val requestForCityNumber = Request.Builder()
            .url("https://www.easterlywave.com/action/weather/search")
            .header("Cookie", Data.webCookie)
            .addHeader("Content-Type", "application/json")
            .addHeader("Connection", "keep-alive")
            .addHeader("Referer", "https://www.easterlywave.com/weather/")
            .addHeader("x-csrftoken", Data.webCookieValue)
            .post(requestBody)
            .build()
        val responseForCityNumber = client.newCall(requestForCityNumber).execute()
        val responseJSON = JsonParser.parseString(responseForCityNumber.body!!.string()).asJsonObject
        var responseStatus = responseJSON.get("status").toString().toInt()
        //如果找不到结果则直接返回错误代码1
        if (responseStatus == 1) {
            return Pair(1, 0)
        }

        //找到结果则分析结果
        val suggestions = responseJSON.get("suggestions").asJsonArray
        var cityNumber = suggestions.get(0).asJsonObject.get("data").toString().replace("\"", "").toInt()

        //如果备选项不止一个则返回代码2以及备选项个数
        if (suggestions.size() > 1) {
            responseStatus = 2
            cityNumber = suggestions.size()
        }

        //如果备选项只有一个则返回代码0以及城市对应WMO代号
        return Pair(responseStatus, cityNumber)
    }

    //获取图片URL
    private fun getWeatherURL(): String {
        //获取图片URL的文件地址部分
        val mediaType = "application/json;charset=utf-8".toMediaTypeOrNull()
        val requestBody = "{\"content\":\"59287\"}".toRequestBody(mediaType)
        val requestForPicURL = Request.Builder()
            .url("https://www.easterlywave.com/action/weather/plot")
            .header("Cookie", Data.webCookie)
            .addHeader("Content-Type", "application/json")
            .addHeader("Connection", "keep-alive")
            .addHeader("Referer", "https://www.easterlywave.com/weather/")
            .addHeader("x-csrftoken", Data.webCookieValue)
            .post(requestBody)
            .build()

        //处理返回的JSON
        val responseForPicURL = client.newCall(requestForPicURL).execute()
        val responseSRC =
            JsonParser.parseString(responseForPicURL.body?.string()).asJsonObject.get("src").toString()
                .replace("\"", "")
        return "https://www.easterlywave.com".plus(responseSRC)
    }

    //获取图片
    private fun getWeatherPic(url: String) {

        //初始化文件获取相关变量
        val file = File(imageFolderPath, imageName)
        val path = Paths.get(file.path)
        var inputStream: InputStream

        //创建Request
        val request = Request.Builder()
            .url(url)
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
}