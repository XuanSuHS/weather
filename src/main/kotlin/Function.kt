package top.xuansu.mirai.weather

import java.io.File

fun deleteFolderContents(folder: File): Int {
    val files = folder.listFiles()

    if (files != null) {
        val fileNumber = files.size
        for (file in files) {
            if (file.isDirectory) {
                // 递归调用删除子文件夹内的文件
                deleteFolderContents(file)
            } else {
                // 删除文件
                file.delete()
            }
        }
        return fileNumber
    }
    return 0
}

fun sortMapWeightByBasin(basin: String): Int {
    return when (basin) {
        "wpac" -> 1
        "eastasia" -> 1
        "natl" -> 2
        "epac" -> 3
        "nio" -> 4
        "sio" -> 5
        "aus" -> 6
        else -> 999 // 在此处可以处理未知或其他情况
    }
}