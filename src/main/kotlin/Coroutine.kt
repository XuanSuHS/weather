package top.xuansu.mirai.weather

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun reloadCookie() {
    CoroutineScope(Dispatchers.IO).launch {
        Web.getCookie {}
    }
}