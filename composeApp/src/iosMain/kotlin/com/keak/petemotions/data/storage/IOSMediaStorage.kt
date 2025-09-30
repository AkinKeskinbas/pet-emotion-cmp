package com.keak.petemotions.data.storage

import com.benasher44.uuid.uuid4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.posix.memcpy

class IOSMediaStorage : MediaStorage {

    private val fileManager = NSFileManager.defaultManager
    private val mediaDirectory: String by lazy {
        val directories = NSSearchPathForDirectoriesInDomains(
            directory = NSDocumentDirectory,
            domainMask = NSUserDomainMask,
            expandTilde = true
        )
        val documentsDir = (directories.firstOrNull() as? String) ?: NSHomeDirectory()
        val mediaPath = (documentsDir as NSString).stringByAppendingPathComponent("media")
        if (!fileManager.fileExistsAtPath(mediaPath)) {
            fileManager.createDirectoryAtPath(mediaPath, true, null, null)
        }
        mediaPath
    }

    override suspend fun save(bytes: ByteArray, extension: String): String = withContext(Dispatchers.Default) {
        val fileName = "${uuid4()}.$extension"
        val targetPath = (mediaDirectory as NSString).stringByAppendingPathComponent(fileName)
        val data = bytes.toNSData()
        data.writeToFile(targetPath, true)
        fileName
    }

    override suspend fun load(path: String): ByteArray? = withContext(Dispatchers.Default) {
        val resolvedPath = if (path.startsWith("/")) {
            path
        } else {
            (mediaDirectory as NSString).stringByAppendingPathComponent(path)
        }
        NSData.dataWithContentsOfFile(resolvedPath)?.toByteArray()
    }
}

private fun ByteArray.toNSData(): NSData = usePinned {
    NSData.dataWithBytes(it.addressOf(0), this.size.toULong())
}

private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    val byteArray = ByteArray(length)
    if (length > 0) {
        byteArray.usePinned {
            memcpy(it.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
        }
    }
    return byteArray
}
