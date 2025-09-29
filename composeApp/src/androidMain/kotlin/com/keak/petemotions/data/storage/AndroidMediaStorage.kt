package com.keak.petemotions.data.storage

import android.content.Context
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AndroidMediaStorage(
    private val context: Context
) : MediaStorage {

    private val mediaDir: File
        get() = File(context.filesDir, "media")

    override suspend fun save(bytes: ByteArray, extension: String): String = withContext(Dispatchers.IO) {
        runCatching {
            val directory = mediaDir
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, "${uuid4()}.${extension}")
            file.writeBytes(bytes)
            file.absolutePath
        }.getOrElse { throw it }
    }

    override suspend fun load(path: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(path)
            if (file.exists()) file.readBytes() else null
        }.getOrNull()
    }
}
