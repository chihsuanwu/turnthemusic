package com.nclab.audiorecognition

import java.io.File

object FileManager {
    private lateinit var file: File
    fun loadFile(path: String) {
        file = File(path)
    }

    fun readBytes(): ByteArray {
        return file.readBytes()
    }
}