package top.xuansu.mirai.weather

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.mamoe.mirai.utils.info
import top.xuansu.mirai.weather.weatherMain.logger

fun onStart() {
    CoroutineScope(Dispatchers.IO).launch {
        val getCookieResult = Web.getCookie()
        if (!getCookieResult.first) {
            logger.info { "Error getting Cookie：${getCookieResult.second}" }
            return@launch
        }
        logger.info { "Cookies Reset" }
        val typhoonDataResponse = Web.TyphoonFunc.getTyphoonData()
        if (typhoonDataResponse.first) {
            logger.info { "Typhoon Data Loaded" }
        } else {
            logger.info { "Error loading typhoon Data：${typhoonDataResponse.second}" }
        }
    }
}