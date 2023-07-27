package top.xuansu.mirai.weather

object Data {
    val seaforUse = mapOf(
        Pair("wpac", "西太平洋"),
        Pair("epac", "东太平洋"),
        Pair("natl", "北大西洋"),
        Pair("nio", "北印度洋"),
        Pair("sio", "南印度洋"),
        Pair("aus", "南太平洋"),
        Pair("eastasia", "东亚地区"),
        Pair("micronesia", "中太平洋/密克罗尼西亚")
    )

    var ecEnsembleTime = mutableMapOf<Int, MutableSet<String>>()
    val sateImgType = setOf(
        "VIS",
        "EVIS",
        "RGB",
        "TRUECOLOR",
        "AIRMASS",
        "BW",
        "BD",
        "CC",
        "RAMMB",
        "OTT",
        "RBTOP",
        "CA",
        "AWV",
        "WV",
        "DIAS"
    )

    data class TyphoonDataClass(
        val name: String,
        val basin: String,
        val longitude: String,
        val latitude: String,
        val windSpeed: String,
        val pressure: String,
        val isSatelliteTarget: Boolean
    )

    //通过code存储台风信息
    val TyphoonData = mutableMapOf<String, TyphoonDataClass>()
    var typhoonFocus = ""
}