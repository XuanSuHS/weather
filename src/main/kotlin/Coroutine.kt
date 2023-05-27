package top.xuansu.mirai.weather

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.mamoe.mirai.utils.info
import top.xuansu.mirai.weather.weatherMain.logger

fun onStart() {
    CoroutineScope(Dispatchers.IO).launch {
        Web.getCookie { err ->
            if (err != null) {
                logger.info { "获取Cookie时出错：$err" }
            } else {
                logger.info { "已重置Cookie" }
                val typhoonDataResponse = Web.TyphoonFunc.getTyphoonData()
                if (typhoonDataResponse.first) {
                    logger.info { "已加载台风信息" }
                } else {
                    logger.info { "台风信息加载失败：$${typhoonDataResponse.second}" }
                }
            }
        }
    }
}