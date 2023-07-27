package top.xuansu.mirai.weather


import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import top.xuansu.mirai.weather.Data.TyphoonData
import top.xuansu.mirai.weather.weatherMain.dataFolder
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
import kotlin.collections.contains
import kotlin.collections.set


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
                    response.close()
                    return
                }
            })
        }
    }

    //获取Cookie
    fun getCookie(): Pair<Boolean, String> {
        var returnData: Pair<Boolean, String>
        //获取Cookie
        val request = Request.Builder()
            .url("https://www.easterlywave.com/weather")
            .addHeader("Connection", "keep-alive")
            .addHeader("Referer", "https://www.easterlywave.com/weather/")
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                //设置Cookie
                saveData.webCookie = if (response.header("Set-Cookie") != null)
                    (response.header("Set-Cookie")!!)
                else {
                    "null"
                }
                saveData.webCookieValue = saveData.webCookie.split(";")[0].replace("csrftoken=", "")
                returnData = Pair(true, "")
            } else {
                returnData = Pair(false, response.code.toString())
            }
            response.close()
        } catch (e: IOException) {
            returnData = Pair(false, "${e.message}")
        }
        return returnData
    }

    //获取图片
    private fun getPic(url: String, imageName: String): Pair<Boolean, String> {

        var returnData: Pair<Boolean, String>
        //初始化文件获取相关变量
        val file = File(imageFolderPath, imageName)
        val path = Paths.get(file.path)
        val inputStream: InputStream

        //创建Request
        val request = Request.Builder()
            .url(url)
            .addHeader("Connection", "keep-alive")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                if (response.body?.byteStream() != null) {
                    inputStream = response.body!!.byteStream()
                    Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING)
                    inputStream.close()
                    returnData = Pair(true, "")
                } else {
                    returnData = Pair(false, "返回内容为空")
                }
            } else {
                returnData = Pair(false, response.code.toString())
            }
            response.close()
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
        private fun getCityNumber(city: String, number: String): Pair<Boolean, String> {
            val returnData: Pair<Boolean, String>
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

            val response = client.newCall(requestForCityNumber).execute()
            if (response.isSuccessful) {
                var responseJSON = JsonObject()

                try {
                    responseJSON = JsonParser.parseString(response.body!!.string()).asJsonObject
                    response.close()
                } catch (e: JsonParseException) {
                    if (e.message != null) {
                        //获得非标准JSON，报错返回
                        returnData = Pair(false, "返回结果非标准JSON，请检查请求是否有误")
                        return returnData
                    }
                }
                //status表搜索结果是否存在
                val status = responseJSON.get("status").toString()

                //搜索结果不存在时返回错误
                if (status == "1") {
                    returnData = Pair(false, "目标城市不存在")
                    return returnData
                }

                //搜索结果
                val suggestions = responseJSON.get("suggestions").asJsonArray

                val suggestionNo = if (suggestions.size() > 1) {
                    when (val inputSuggestionNo = number.toIntOrNull()) {
                        null -> {
                            var message = "\n找到多个备选项"
                            for (i in 0 until suggestions.size()) {
                                message += "\n${i + 1}.${
                                    suggestions.get(i).asJsonObject.get("value").toString().replace("\"", "")
                                }"
                            }
                            message += "\n输入 /weather $city <编号> 来选择您想要的结果"
                            returnData = Pair(false, message)
                            return returnData
                        }

                        !in 1..suggestions.size() -> {
                            var message = "找不到该城市，请检查输入是否正确"
                            message += "\n找到多个备选项"
                            for (i in 0 until suggestions.size()) {
                                message += "\n${i + 1}.${
                                    suggestions.get(i).asJsonObject.get("value").toString().replace("\"", "")
                                }"
                            }
                            message += "\n输入 /weather $city <编号> 来选择您想要的结果"
                            returnData = Pair(false, message)
                            return returnData
                        }

                        else -> {
                            inputSuggestionNo - 1
                        }
                    }
                } else {
                    0
                }
                //搜索结果唯一时返回城市WMO代号
                val cityNumber =
                    suggestions.get(suggestionNo).asJsonObject.get("data").toString().replace("\"", "").toInt()
                returnData = Pair(true, cityNumber.toString())
            } else {
                returnData = Pair(false, response.code.toString())
            }
            return returnData
        }

        fun getWeather(city: String, number: String): Pair<Boolean, String> {
            val returnData: Pair<Boolean, String>

            //获取城市WMO
            val getCityNumberResponse = getCityNumber(city, number)
            val cityNumber = if (!getCityNumberResponse.first) {
                returnData = Pair(false, "请求城市WMO时出错：${getCityNumberResponse.second}")
                return returnData
            } else {
                getCityNumberResponse.second.toInt()
            }

            //获取城市图片URL
            //出错时返回
            val getWeatherURLResponse = getWeatherURL(cityNumber)
            if (!getWeatherURLResponse.first) {
                returnData = Pair(false, "获取URL时出错：${getWeatherURLResponse.second}")
                return returnData
            }

            //获取图片
            val weatherPicURL = getWeatherURLResponse.second
            val imageName = "$cityNumber.png"
            val getPicResponse = getPic(weatherPicURL, imageName)
            returnData = if (getPicResponse.first) {
                //图片文件获取成功
                //返回图片信息供上传
                Pair(true, imageName)
            } else {
                //图片文件获取失败
                //返回错误代码
                Pair(false, "下载图片时出错：${getPicResponse.second}")
            }
            return returnData
        }

        //获取天气图片URL
        private fun getWeatherURL(cityNumber: Int): Pair<Boolean, String> {
            var returnData: Pair<Boolean, String>
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

            try {
                val response = client.newCall(requestForPicURL).execute()
                if (response.isSuccessful) {
                    val picURI =
                        JsonParser.parseString(response.body?.string()).asJsonObject.get("src").toString()
                            .replace("\"", "")
                    response.close()
                    val weatherPicURL = "https://www.easterlywave.com$picURI"
                    returnData = Pair(true, weatherPicURL)
                } else {
                    returnData = Pair(false, response.code.toString())
                    response.close()
                }
            } catch (e: IOException) {
                returnData = Pair(false, "${e.message}")
                return returnData
            }
            return returnData
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
                        Pair(false, "此台风代号不存在\n".plus("现存台风代号：${TyphoonData.keys}"))
                    }
                }

                in TyphoonData.keys -> {
                    Pair(true, codeUp)
                }

                else -> {
                    Pair(false, "此台风代号不存在\n".plus("现存台风代号：${TyphoonData.keys}"))
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


        fun getECEnsemble(code: String): Pair<Boolean, String> {
            val ecTimeResponse = getECTime()
            if (!ecTimeResponse.first) {
                return Pair(false, ecTimeResponse.second)
            }
            val ecTime = Data.ecEnsembleTime.maxBy { it.key }.key

            val typhoonName = TyphoonData[code]!!.name
            val ecTyphoonURL = "https://www.easterlywave.com/media/typhoon/ensemble/$ecTime/$typhoonName.png"
            val ecSeaURL = "https://www.easterlywave.com/media/typhoon/ensemble/$ecTime/${Config.defaultSeaArea}.png"

            val typhoonImageName = "ec-${ecTime}-${typhoonName}.png"
            if (!dataFolder.resolve(typhoonImageName).exists()) {
                val getPicResult = getPic(ecTyphoonURL, typhoonImageName)
                if (!getPicResult.first) {
                    return Pair(false, "下载台风预报图片时出错：${getPicResult.second}")
                }
            }

            val seaImageName = "ec-${ecTime}-${Config.defaultSeaArea}.png"
            if (!dataFolder.resolve(seaImageName).exists()) {
                val getPicResult = getPic(ecSeaURL, seaImageName)
                if (!getPicResult.first) {
                    return Pair(false, "下载海洋预报图片时出错：${getPicResult.second}")
                }
            }

            return Pair(true, "${typhoonImageName}||${seaImageName}")
        }

        //获取EC预报时间
        //基于EasterlyWave
        fun getECTime(): Pair<Boolean, String> {
            Data.ecEnsembleTime.clear()
            var returnData = Pair(false, "EC基准时间获取失败")
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

            try {
                val response = client.newCall(requestForURL).execute()
                if (response.isSuccessful) {

                    val ecEnsembleData = JsonParser.parseString(response.body!!.string()).asJsonObject
                        .get("data").asJsonArray

                    for (i in ecEnsembleData) {
                        val dataItem = i.asJsonObject
                        val time = dataItem.get("basetime").toString().replace("\"", "").toInt()
                        val storms = dataItem.get("storms").asJsonArray
                        val stormNameArray = mutableSetOf<String>()
                        for (stormItem in storms) {
                            val stormName = stormItem.toString().replace("\"", "")
                            stormNameArray += stormName
                        }
                        Data.ecEnsembleTime[time] = stormNameArray
                    }
                    Data.ecEnsembleTime = Data.ecEnsembleTime.toSortedMap()
                    returnData = Pair(true, "")
                } else {
                    returnData = Pair(false, response.code.toString())
                }
                response.close()
            } catch (e: IOException) {
                Pair(false, "${e.message}")
                return returnData
            }
            return returnData
        }


        //获取现存台风数据
        //基于EasterlyWave
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

            //错误处置
            if (!response.isSuccessful) {
                return Pair(false, response.code.toString())
            } else if (response.body == null) {
                return Pair(false, "返回内容为空")
            } else {
                TyphoonData.clear()
                val responseData = response.body!!.string()
                val typhoonData = JsonParser.parseString(responseData).asJsonObject
                //设置 需要关注的台风
                Data.typhoonFocus = typhoonData.get("focus").toString().replace("\"", "")

                //详细台风信息
                val stormsArray = typhoonData.get("storms").asJsonArray
                //如果没台风，直接回调
                if (stormsArray.size() == 0) {
                    return Pair(false, "当前无台风")
                }

                for (i in stormsArray) {
                    val stormData = i.asJsonObject
                    val code = stormData.get("code").toString().replace("\"", "")
                    val name = stormData.get("name").toString().replace("\"", "")
                    val basin = stormData.get("basin").toString().replace("\"", "")
                    val longitude = stormData.get("lonstr").toString().replace("\"", "")
                    val latitude = stormData.get("latstr").toString().replace("\"", "")
                    val windSpeed = stormData.get("wind").toString().replace("\"", "").plus(" Kt")
                    val pressure = stormData.get("pressure").toString().replace("\"", "").plus(" hPa")
                    val isSatelliteTarget =
                        stormData.get("is_target").toString().replace("\"", "").toBoolean()
                    TyphoonData[code] = Data.TyphoonDataClass(
                        name,
                        basin,
                        longitude,
                        latitude,
                        windSpeed,
                        pressure,
                        isSatelliteTarget
                    )
                }
                //TODO:Sort Typhoon by BASIN
                //val sortedTyphoonData = TyphoonData.toSortedMap(compareBy { sortMapWeightByBasin() })
                returnData = Pair(true, "")
            }
            response.close()
            return returnData
        }

        fun getTyphoonSatePic(code: String, picType: String): Pair<Boolean, String> {
            val returnData: Pair<Boolean, String>

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

        fun getSST(area: String): Pair<Boolean, String> {
            val returnData: Pair<Boolean, String>
            //检查传入海域信息
            //错误时回调ERR
            if (area !in Data.seaforUse) {
                returnData = Pair(false, "该海域不存在")
                return returnData
            }

            val urlResponse = getSSTUrl()
            if (urlResponse.first) {
                val time = urlResponse.second
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
                returnData = Pair(false, "获取URL时出错：${urlResponse.second}")
            }
            return returnData
        }

        private fun getSSTUrl(): Pair<Boolean, String> {

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
            val urlResponse: Response
            try {
                urlResponse = client.newCall(request).execute()
            } catch (e: IOException) {
                returnData = Pair(false, "${e.message}")
                return returnData
            }

            if (!urlResponse.isSuccessful) {
                return Pair(false, urlResponse.code.toString())
            }

            val body = urlResponse.body!!.string()
            val times =
                JsonParser.parseString(body).asJsonObject
                    .get("times").asJsonArray
                    .get(0).toString().replace("\"", "")
            returnData = Pair(true, times)
            urlResponse.close()
            return returnData
        }
    }
}