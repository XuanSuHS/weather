package top.xuansu.mirai.weather


import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import top.xuansu.mirai.weather.weatherMain.imageFolder
import top.xuansu.mirai.weather.weatherMain.imageFolderPath
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

    //代理相关函数
    object ProxyFunc {
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
                    return
                }

                override fun onResponse(call: Call, response: Response) {
                    // 请求成功时的回调
                    callback(true)
                    return
                }
            })
        }
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
                return
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
                        return
                    } else {
                        callback(response.code.toString())
                        return
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

        client.newCall(request).enqueue(object : Callback {
            //图片下载失败
            override fun onFailure(call: Call, e: IOException) {
                callback(e.message)
            }

            //图片下载成功
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    if (response.body?.byteStream() != null) {
                        inputStream = response.body!!.byteStream()
                        Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING)
                        inputStream.close()
                        callback(null)
                    } else {
                        callback("返回内容为空")
                    }
                } else {
                    callback(response.code.toString())
                }
            }
        })
    }

    //天气相关函数
    object CityWeatherFunc {
        //获取城市WMO代号
        //搜索成功时返回true与城市代号
        //搜索失败时返回false与错误原因
        fun getCityNumber(city: String, callback: (Boolean, String) -> Unit) {

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

            client.newCall(requestForCityNumber).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // 请求失败时的回调
                    callback(false, "${e.message}")
                    return
                }

                override fun onResponse(call: Call, response: Response) {
                    // 请求成功时的回调
                    response.use {
                        if (response.isSuccessful) {
                            var responseJSON = JsonObject()
                            try {
                                responseJSON = JsonParser.parseString(response.body!!.string()).asJsonObject
                            } catch (e: JsonParseException) {
                                if (e.message != null) {
                                    //获得非标准JSON，报错返回
                                    callback(false, "返回结果非标准JSON，请检查请求是否有误")
                                    return
                                }
                            }
                            //status表搜索结果是否存在
                            val status = responseJSON.get("status").toString()

                            //搜索结果不存在时返回错误
                            if (status == "1") {
                                callback(false, "目标城市不存在")
                                return
                            }

                            //搜索结果个数
                            val suggestions = responseJSON.get("suggestions").asJsonArray
                            //不止一个时返回错误
                            if (suggestions.size() > 1) {
                                callback(false, "目标城市过多，请再精确些")
                                return
                            }

                            //搜索结果唯一时返回城市WMO代号
                            val cityNumber =
                                suggestions.get(0).asJsonObject.get("data").toString().replace("\"", "").toInt()
                            callback(true, cityNumber.toString())
                        } else {
                            callback(false, response.code.toString())
                        }
                    }
                }
            })
        }

        fun getWeather(city: String, callback: (String?, String) -> Unit) {

            //获取城市WMO
            getCityNumber(city) { isSuccess, data ->
                if (isSuccess) {
                    //成功返回WMO
                    val cityNumber = data.toInt()

                    //获取城市图片URL
                    getWeatherURL(cityNumber) { picURI, urlErr ->
                        if (urlErr == null) {
                            //图片URL获取成功
                            //返回图片信息
                            val weatherPicURL = "https://www.easterlywave.com$picURI"
                            val imageName = "$cityNumber.png"
                            //获取图片
                            getPic(weatherPicURL, imageName) { picErr ->
                                if (picErr == null) {
                                    //图片文件获取成功
                                    //返回图片信息供上传
                                    callback(null, imageName)
                                } else {
                                    //图片文件获取失败
                                    //返回错误代码
                                    callback("下载图片时出错：$picErr", "")
                                }
                            }
                        } else {
                            //图片URL获取失败
                            //返回错误代码
                            callback("获取URL时出错：$urlErr", "")
                        }
                    }
                } else {
                    //执行时出错
                    callback("请求城市WMO时出错：$data", "")
                }
            }
        }

        //获取天气图片URL
        //callback第一个为回调数据，在此为图片URI
        //callback第二个为错误信息，无错误时为null
        private fun getWeatherURL(cityNumber: Int, callback: (String, String?) -> Unit) {

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
            client.newCall(requestForPicURL).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // 请求失败时的回调
                    callback("null", e.message)
                    return
                }

                override fun onResponse(call: Call, response: Response) {
                    // 请求成功时的回调
                    response.use {
                        if (response.isSuccessful) {
                            val weatherPicURI =
                                JsonParser.parseString(response.body?.string()).asJsonObject.get("src").toString()
                                    .replace("\"", "")
                            callback(weatherPicURI, null)
                            return
                        } else {
                            callback("null", response.code.toString())
                            return
                        }
                    }
                }
            })
        }
    }


    //台风相关函数
    object TyphoonFunc {
        fun getTyphoon(area: String, callback: (String?, String?) -> Unit) {
            //检查传入海域信息
            //错误时回调ERR
            if (area !in Data.seaforUse) {
                callback(null, "该海域不存在")
                return
            }

            getTyphoonURL { time, urlErr ->
                if (urlErr == null) {
                    //图片URL获取成功
                    //根据URL信息获取图片文件
                    val url = "https://easterlywave.com/media/typhoon/ensemble/$time/$area.png"
                    val imageName = "$time-$area.png"
                    if (imageFolder.resolve(imageName).exists()) {
                        callback(imageName, null)
                        return@getTyphoonURL
                    } else {
                        getPic(url, imageName) { picErr ->
                            if (picErr == null) {
                                //图片文件获取成功
                                //返回图片信息供上传
                                callback(imageName, null)
                                return@getPic
                            } else {
                                //图片文件获取失败
                                //返回错误信息
                                callback(null, "下载图片时出错：$picErr")
                                return@getPic
                            }
                        }
                    }
                } else {
                    //图片URL获取失败
                    //返回错误信息
                    callback(null, "获取URL时出错：$urlErr")
                    return@getTyphoonURL
                }
            }
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
    }

    //海温相关函数
    object SSTFunc {

        fun getSST(area: String, callback: (String?, String?) -> Unit) {
            //检查传入海域信息
            //错误时回调ERR
            if (area !in Data.seaforUse) {
                callback(null, "该海域不存在")
                return
            }

            getSSTUrl { time, urlErr ->
                if (urlErr == null) {
                    //图片URL获取成功
                    //根据URL信息获取图片文件
                    val url = "https://www.easterlywave.com/media/typhoon/sst/$time/$area.png"
                    val imageName = "$time-$area.png"
                    if (imageFolder.resolve(imageName).exists()) {
                        callback(imageName, null)
                        return@getSSTUrl
                    } else {
                        getPic(url, imageName) { picErr ->
                            if (picErr == null) {
                                //图片文件获取成功
                                //返回图片信息供上传
                                callback(imageName, null)
                                return@getPic
                            } else {
                                //图片文件获取失败
                                //返回错误信息
                                callback(null, "下载图片时出错：$picErr")
                                return@getPic
                            }
                        }
                    }
                } else {
                    //图片URL获取失败
                    //返回错误信息
                    callback(null, "获取URL时出错：$urlErr")
                    return@getSSTUrl
                }
            }
        }

        private fun getSSTUrl(callback: (String, String?) -> Unit) {

            val mediaType = "application/json;charset=utf-8".toMediaTypeOrNull()
            val requestBody = "".toRequestBody(mediaType)
            val request = Request.Builder()
                .url("https://www.easterlywave.com/action/typhoon/sst")
                .header("Cookie", Data.webCookie)
                .addHeader("Content-Type", "application/json")
                .addHeader("Connection", "keep-alive")
                .addHeader("Referer", "https://www.easterlywave.com/typhoon/sst/")
                .addHeader("x-csrftoken", Data.webCookieValue)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // 请求失败时的回调
                    callback("", e.message)
                }

                override fun onResponse(call: Call, response: Response) {
                    // 请求成功时的回调
                    response.use {
                        if (response.isSuccessful) {
                            val body = response.body!!.string()
                            val times =
                                JsonParser.parseString(body).asJsonObject
                                    .get("times").asJsonArray
                                    .get(0).toString().replace("\"", "")
                            callback(times, null)
                        } else {
                            callback("", response.code.toString())
                        }
                    }
                }
            })
        }
    }
}