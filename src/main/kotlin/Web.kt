package top.xuansu.mirai.weather


import com.google.gson.JsonParser
import net.mamoe.mirai.utils.info
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import top.xuansu.mirai.weather.weatherMain.imageFolder
import top.xuansu.mirai.weather.weatherMain.imageFolderPath
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

    //获取Cookie
    fun getCookie() {

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .proxy(
                Proxy(
                    Proxy.Type.HTTP,
                    InetSocketAddress(Config.proxyAddress.split(":")[0], Config.proxyAddress.split(":")[1].toInt())
                )
            )
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
        Data.webCookie = if (responseForCookie.header("Set-Cookie") != null)
            (responseForCookie.header("Set-Cookie")!!)
        else {
            "null"
        }
        Data.webCookieValue = Data.webCookie.split(";")[0].replace("csrftoken=", "")
    }

    fun getWeather(city: String) {
        val cityNumber = getCityNumber(city).second
        val url = getWeatherURL(cityNumber)
        val imageName = "$cityNumber.png"
        getPic(url, imageName)
        return
    }

    fun getTyphoon(): String {
        val time = getTyphoonURL()
        val url = "https://easterlywave.com/media/typhoon/ensemble/$time/wpac.png"
        val imageName = "$time-wpac.png"
        if (!imageFolder.resolve(imageName).exists()) {
            getPic(url, imageName)
        }
        return imageName
    }

    //获取城市对应数字
    fun getCityNumber(city: String): Pair<Int, Int> {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .proxy(
                Proxy(
                    Proxy.Type.HTTP,
                    InetSocketAddress(Config.proxyAddress.split(":")[0], Config.proxyAddress.split(":")[1].toInt())
                )
            )
            .build()

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

    //获取天气图片URL
    private fun getWeatherURL(cityNumber: Int): String {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .proxy(
                Proxy(
                    Proxy.Type.HTTP,
                    InetSocketAddress(Config.proxyAddress.split(":")[0], Config.proxyAddress.split(":")[1].toInt())
                )
            )
            .build()

        //获取图片URL的文件地址部分
        val mediaType = "application/json;charset=utf-8".toMediaTypeOrNull()
        val requestBody = "{\"content\":\"$cityNumber\"}".toRequestBody(mediaType)
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

    //获取台风图片URL
    private fun getTyphoonURL(): String {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .proxy(
                Proxy(
                    Proxy.Type.HTTP,
                    InetSocketAddress(Config.proxyAddress.split(":")[0], Config.proxyAddress.split(":")[1].toInt())
                )
            )
            .build()

        val mediaType = "application/json;charset=utf-8".toMediaTypeOrNull()
        val requestBody = "".toRequestBody(mediaType)
        val URLRequest = Request.Builder()
            .url("https://www.easterlywave.com/action/typhoon/ecens")
            .header("Cookie", Data.webCookie)
            .addHeader("Content-Type", "application/json")
            .addHeader("Connection", "keep-alive")
            .addHeader("Referer", "https://www.easterlywave.com/typhoon/ensemble/")
            .addHeader("x-csrftoken", Data.webCookieValue)
            .post(requestBody)
            .build()

        val urlResponse = client.newCall(URLRequest).execute()
        val time =
            JsonParser.parseString(urlResponse.body?.string()).asJsonObject
                .get("data").asJsonArray
                .get(0).asJsonObject
                .get("basetime").toString().replace("\"", "")
        return time
        //return urlResponse.body!!.string()
    }

    //获取图片
    private fun getPic(url: String, imageName: String) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .proxy(
                Proxy(
                    Proxy.Type.HTTP,
                    InetSocketAddress(Config.proxyAddress.split(":")[0], Config.proxyAddress.split(":")[1].toInt())
                )
            )
            .build()

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