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
                        saveData.webCookie = if (response.header("Set-Cookie") != null)
                            (response.header("Set-Cookie")!!)
                        else {
                            "null"
                        }
                        saveData.webCookieValue = saveData.webCookie.split(";")[0].replace("csrftoken=", "")
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
    private fun getPic(url: String, imageName: String): Pair<Boolean, String> {

        val returnData: Pair<Boolean, String>
        //初始化文件获取相关变量
        val file = File(imageFolderPath, imageName)
        val path = Paths.get(file.path)
        val inputStream: InputStream

        //创建Request
        val request = Request.Builder()
            .url(url)
            .addHeader("Connection", "keep-alive")
            .build()

        returnData = try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                if (response.body?.byteStream() != null) {
                    inputStream = response.body!!.byteStream()
                    Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING)
                    inputStream.close()
                    Pair(true, "")
                } else {
                    Pair(false, "返回内容为空")
                }
            } else {
                Pair(false, response.code.toString())
            }
        } catch (e: IOException) {
            returnData = Pair(false, "${e.message}")
            return returnData
        }
        return returnData
    }

    //天气相关函数
    //基于EasterlyWave
    object CityWeatherFunc {
        //获取城市WMO代号
        //搜索成功时返回true与城市代号
        //搜索失败时返回false与错误原因
        fun getCityNumber(city: String, callback: (Boolean, String) -> Unit) {

            val mediaType = "application/json;charset=utf-8".toMediaTypeOrNull()
            val requestBody = "{\"content\":\"$city\"}".toRequestBody(mediaType)
            val requestForCityNumber = Request.Builder()
                .url("https://www.easterlywave.com/action/weather/search")
                .header("Cookie", saveData.webCookie)
                .addHeader("Content-Type", "application/json")
                .addHeader("Connection", "keep-alive")
                .addHeader("Referer", "https://www.easterlywave.com/weather/")
                .addHeader("x-csrftoken", saveData.webCookieValue)
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
                            val getPicResponse = getPic(weatherPicURL, imageName)
                            if (getPicResponse.first) {
                                //图片文件获取成功
                                //返回图片信息供上传
                                callback(null, imageName)
                            } else {
                                //图片文件获取失败
                                //返回错误代码
                                callback("下载图片时出错：${getPicResponse.second}", "")
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
                .header("Cookie", saveData.webCookie)
                .addHeader("Content-Type", "application/json")
                .addHeader("Connection", "keep-alive")
                .addHeader("Referer", "https://www.easterlywave.com/weather/")
                .addHeader("x-csrftoken", saveData.webCookieValue)
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

        //检查输入台风是否满足要求
        fun checkTyphoonCode(code: String): Pair<Boolean, String> {
            val pairReturn = when (val codeUp = code.uppercase()) {
                in setOf("", "0", "default", "默认") -> {
                    if (Data.typhoonFocus != "") {
                        Pair(true, Data.typhoonFocus)
                    } else {
                        Pair(false, "此台风代号不存在\n".plus("现存台风代号：${Data.TyphoonData.keys}"))
                    }
                }

                in Data.TyphoonData.keys -> {
                    Pair(true, codeUp)
                }

                else -> {
                    Pair(false, "此台风代号不存在\n".plus("现存台风代号：${Data.TyphoonData.keys}"))
                }
            }
            return pairReturn
        }

        //检查图片是否符合需求
        fun checkImgType(imageType: String): Pair<Boolean, String> {
            val result = when (val imgIn = imageType.uppercase()) {
                in arrayOf("", "DEFAULT", "默认") -> {
                    Pair(true, Config.defaultImgType)
                }

                in Data.sateImgType -> {
                    Pair(true, imgIn)
                }

                "HELP" -> {
                    Pair(false, "支持的图片类型为：${Data.sateImgType}")
                }

                else -> {
                    Pair(false, "不支持的图片类型\n".plus("支持的图片类型为：${Data.sateImgType}"))
                }
            }
            return result
        }


        fun getECForecast(code: String): Pair<Boolean, String> {
            var returnData = Pair(false, "ERR")
            val ecTimeResponse = getECTime()
            if (!ecTimeResponse.first) {
                returnData = Pair(false, ecTimeResponse.second)
                return returnData
            }

            val ecTime = ecTimeResponse.second
            val ecURL = "https://www.easterlywave.com/media/typhoon/ensemble/$ecTime/$code.png"
            val imageName = "ec-${ecTime}-${code}.png"
            //val getPicResponse = getPic(url,imageName).
            return Pair(true, ecURL)
        }

        //获取EC预报时间
        //基于EasterlyWave
        private fun getECTime(): Pair<Boolean, String> {

            var returnData = Pair(false, "图片URL获取失败")

            val mediaType = "application/json;charset=utf-8".toMediaTypeOrNull()
            val requestBody = "".toRequestBody(mediaType)
            val requestForURL = Request.Builder()
                .url("https://www.easterlywave.com/action/typhoon/ecens")
                .header("Cookie", saveData.webCookie)
                .addHeader("Content-Type", "application/json")
                .addHeader("Connection", "keep-alive")
                .addHeader("Referer", "https://www.easterlywave.com/typhoon/ensemble/")
                .addHeader("x-csrftoken", saveData.webCookieValue)
                .post(requestBody)
                .build()

            returnData = try {
                val response = client.newCall(requestForURL).execute()
                if (response.isSuccessful) {
                    val time =
                        JsonParser.parseString(response.body!!.string()).asJsonObject
                            .get("data").asJsonArray
                            .get(0).asJsonObject
                            .get("basetime").toString().replace("\"", "")
                    Pair(true, time)
                } else {
                    Pair(false, response.code.toString())
                }
            } catch (e: IOException) {
                Pair(false, "${e.message}")
                return returnData
            }
            return returnData
        }


        //获取现存台风数据
        //基于EasterlyWave
        //存在台风时回调true和台风个数
        //不存在台风时
        fun getTyphoonData(): Pair<Boolean, String> {
            val mediaType = "application/json;charset=utf-8".toMediaTypeOrNull()
            val requestBody = "".toRequestBody(mediaType)
            val request = Request.Builder()
                .url("https://www.easterlywave.com/action/typhoon/sector")
                .header("Cookie", saveData.webCookie)
                .addHeader("Content-Type", "application/json")
                .addHeader("Connection", "keep-alive")
                .addHeader("Referer", "https://www.easterlywave.com/typhoon/")
                .addHeader("x-csrftoken", saveData.webCookieValue)
                .post(requestBody)
                .build()

            val returnData: Pair<Boolean, String>
            val response: Response
            try {
                response = client.newCall(request).execute()
            } catch (e: IOException) {
                returnData = Pair(false, "${e.message}")
                return returnData
            }

            if (response.isSuccessful) {
                if (response.body != null) {
                    Data.TyphoonData.clear()
                    val responseData = response.body!!.string()
                    val typhoonData = JsonParser.parseString(responseData).asJsonObject
                    //设置 需要关注的台风
                    Data.typhoonFocus = typhoonData.get("focus").toString().replace("\"", "")

                    //详细台风信息
                    val stormsArray = typhoonData.get("storms").asJsonArray
                    val stormCount = stormsArray.size()
                    //如果没台风，直接回调
                    if (stormCount == 0) {
                        returnData = Pair(false, "当前无台风")
                        return returnData
                    }

                    for (i in 0 until stormCount) {
                        val stormData = stormsArray.get(i).asJsonObject
                        val code = stormData.get("code").toString().replace("\"", "")
                        val name = stormData.get("name").toString().replace("\"", "")
                        val basin = stormData.get("basin").toString().replace("\"", "")
                        val longitude = stormData.get("lonstr").toString().replace("\"", "")
                        val latitude = stormData.get("latstr").toString().replace("\"", "")
                        val windSpeed = stormData.get("wind").toString().replace("\"", "").plus(" Kt")
                        val pressure = stormData.get("pressure").toString().replace("\"", "").plus(" hPa")
                        val isSatelliteTarget =
                            stormData.get("is_target").toString().replace("\"", "").toBoolean()
                        Data.TyphoonData[code] = Data.TyphoonDataClass(
                            name,
                            basin,
                            longitude,
                            latitude,
                            windSpeed,
                            pressure,
                            isSatelliteTarget
                        )
                    }
                    returnData = Pair(true, "")
                } else {
                    returnData = Pair(false, "返回内容为空")
                }

            } else {
                returnData = Pair(false, response.code.toString())
            }
            return returnData
        }


        fun getTyphoonSatePic(codeIn: String, picType: String): Pair<Boolean, String> {
            val returnData: Pair<Boolean, String>

            //检查台风代号可用性
            val codeCheckResult = checkTyphoonCode(codeIn)
            val code = when (codeCheckResult.first) {
                true -> {
                    codeCheckResult.second
                }

                false -> {
                    returnData = Pair(false, codeCheckResult.second)
                    return returnData
                }
            }

            //是否有卫星图片
            if (!Data.TyphoonData[code]!!.isSatelliteTarget) {
                returnData = Pair(false, "此台风不存在卫星图片")
                return returnData
            }

            //从Dapiya网站下载指定卫星图片
            val url = "https://data.dapiya.top/history/$code/$picType/${code}_${picType}.png"
            val imageName = "${code}_${picType}.png"
            val getPicResponse = getPic(url, imageName)
            returnData = if (getPicResponse.first) {
                Pair(true, imageName)
            } else {
                Pair(false, getPicResponse.second)
            }

            return returnData
        }
    }

    //海温相关函数
    object SSTFunc {

        fun getSSTbyRTOFS(area: String): Pair<Boolean, String> {
            val returnData: Pair<Boolean, String>
            //检查传入海域信息
            //错误时回调ERR
            if (area !in Data.seaforUse) {
                returnData = Pair(false, "该海域不存在")
                return returnData
            }

            val getSSTByRTOFSURLResponse = getSSTbyRTOFSUrl()
            if (getSSTByRTOFSURLResponse.first) {
                val time = getSSTByRTOFSURLResponse.second
                //图片URL获取成功
                //根据URL信息获取图片文件
                val url = "https://www.easterlywave.com/media/typhoon/sst/$time/$area.png"
                val imageName = "$time-$area.png"

                returnData = if (imageFolder.resolve(imageName).exists()) {
                    Pair(true, imageName)
                } else {
                    val getPicResponse = getPic(url, imageName)
                    if (getPicResponse.first) {
                        //图片文件获取成功
                        //返回图片信息供上传
                        Pair(true, imageName)
                    } else {
                        //图片文件获取失败
                        //返回错误信息
                        Pair(false, "下载图片时出错：${getPicResponse.second}")
                    }
                }
            } else {
                //图片URL获取失败
                //返回错误信息
                returnData = Pair(false, "获取URL时出错：${getSSTByRTOFSURLResponse.second}")
            }
            return returnData
        }

        private fun getSSTbyRTOFSUrl(): Pair<Boolean, String> {

            val mediaType = "application/json;charset=utf-8".toMediaTypeOrNull()
            val requestBody = "".toRequestBody(mediaType)
            val request = Request.Builder()
                .url("https://www.easterlywave.com/action/typhoon/sst")
                .header("Cookie", saveData.webCookie)
                .addHeader("Content-Type", "application/json")
                .addHeader("Connection", "keep-alive")
                .addHeader("Referer", "https://www.easterlywave.com/typhoon/sst/")
                .addHeader("x-csrftoken", saveData.webCookieValue)
                .post(requestBody)
                .build()

            val returnData: Pair<Boolean, String>
            val response: Response
            try {
                response = client.newCall(request).execute()
            } catch (e: IOException) {

                returnData = Pair(false, "${e.message}")
                return returnData
            }

            returnData = if (response.isSuccessful) {
                val body = response.body!!.string()
                val times =
                    JsonParser.parseString(body).asJsonObject
                        .get("times").asJsonArray
                        .get(0).toString().replace("\"", "")
                Pair(true, times)
            } else {
                Pair(false, response.code.toString())
            }

            return returnData
        }
    }
}