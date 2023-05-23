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
    private var client = if (Config.isProxyEnabled) {
        val proxyAdd = Config.proxyAddress.split(":")[0]
        val proxyPort = Config.proxyAddress.split(":")[1].toInt()
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .proxy(
                Proxy(
                    Proxy.Type.HTTP,
                    InetSocketAddress(proxyAdd, proxyPort)
                )
            )
            .build()
    } else {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun enableProxy() {
        val proxyAdd = Config.proxyAddress.split(":")[0]
        val proxyPort = Config.proxyAddress.split(":")[1].toInt()
        client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .proxy(
                Proxy(
                    Proxy.Type.HTTP,
                    InetSocketAddress(proxyAdd, proxyPort)
                )
            )
            .build()
    }

    fun disableProxy() {
        client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    //检查代理是否可用
    fun checkProxy(proxyAdd: String, callback: (Boolean) -> Unit) {

        val proxyClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .proxy(
                Proxy(
                    Proxy.Type.HTTP,
                    InetSocketAddress(proxyAdd.split(":")[0], proxyAdd.split(":")[1].toInt())
                )
            )
            .build()

        val request = Request.Builder()
            .url("https://www.easterlywave.com/weather")
            .addHeader("Connection", "keep-alive")
            .addHeader("Referer", "https://www.easterlywave.com/weather/")
            .get()
            .build()

        proxyClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 请求失败时的回调
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                // 请求成功时的回调
                callback(true)
            }
        })
    }

    //获取Cookie
    fun getCookie(callback: (String?) -> Unit) {

        //获取Cookie
        val request = Request.Builder()
            .url("https://www.easterlywave.com/weather")
            .addHeader("Connection", "keep-alive")
            .addHeader("Referer", "https://www.easterlywave.com/weather/")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 请求失败时的回调
                callback(e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                // 请求成功时的回调
                response.use {
                    if (response.isSuccessful) {
                        //设置Cookie
                        Data.webCookie = if (response.header("Set-Cookie") != null)
                            (response.header("Set-Cookie")!!)
                        else {
                            "null"
                        }
                        Data.webCookieValue = Data.webCookie.split(";")[0].replace("csrftoken=", "")
                        callback(null)
                    } else {
                        callback(response.code.toString())
                    }
                }
            }
        })
    }

    fun getWeather(city: String) {
        val cityNumber = getCityNumber(city).second
        val url = getWeatherURL(cityNumber)
        val imageName = "$cityNumber.png"
        getPic(url, imageName) {}
        return
    }

    fun getTyphoon(callback: (String?, String?) -> Unit) {
        getTyphoonURL { time, urlErr ->
            if (urlErr == null) {
                val url = "https://easterlywave.com/media/typhoon/ensemble/$time/wpac.png"
                val imageName = "$time-wpac.png"
                if (!imageFolder.resolve(imageName).exists()) {
                    getPic(url, imageName) { picErr ->
                        if (picErr == null) {
                            callback(imageName, null)
                        } else {
                            callback(null, "下载图片时出错：$picErr")
                        }
                    }
                }
            } else {
                callback(null, "获取URL时出错：$urlErr")
            }
        }
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

    //获取天气图片URL
    private fun getWeatherURL(cityNumber: Int): String {

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
    //回调值第一个为成功时数据
    //第二个是出错时错误代码
    private fun getTyphoonURL(callback: (String?, String?) -> Unit) {

        val mediaType = "application/json;charset=utf-8".toMediaTypeOrNull()
        val requestBody = "".toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://www.easterlywave.com/action/typhoon/ecens")
            .header("Cookie", Data.webCookie)
            .addHeader("Content-Type", "application/json")
            .addHeader("Connection", "keep-alive")
            .addHeader("Referer", "https://www.easterlywave.com/typhoon/ensemble/")
            .addHeader("x-csrftoken", Data.webCookieValue)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 请求失败时的回调
                callback(null, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                // 请求成功时的回调
                response.use {
                    if (response.isSuccessful) {
                        val time =
                            JsonParser.parseString(response.body!!.string()).asJsonObject
                                .get("data").asJsonArray
                                .get(0).asJsonObject
                                .get("basetime").toString().replace("\"", "")
                        callback(time, null)
                    } else {
                        callback(null, response.code.toString())
                    }
                }
            }
        })
    }

    //获取图片
    private fun getPic(url: String, imageName: String, callback: (String?) -> Unit) {

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
                callback(e.message)
            }

            //图片下载成功
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful && response.body?.byteStream() != null) {
                    inputStream = response.body!!.byteStream()
                    Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING)
                    inputStream.close()
                    callback(null)
                } else if (response.body?.byteStream() == null) {
                    callback("返回内容为空")
                }
            }
        })
        //
        //   End of the Part
        //
    }
}