package com.raywenderlich.android.photouploader.worker

import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import com.raywenderlich.android.photouploader.ImageUtils

/**
 * Sau khi task 1 filter hhoàn thành, sẽ output ra các image đó cho worker này zip chúng lại
 * Input data ở task này chính là lấy từ output của task lọc, do đây là task chain
 */
class CompressWorker : Worker() {
    override fun doWork(): Result = try {
        //từ input data, lọc ra những image nào begin = "..."
        val imagePaths = inputData.keyValueMap
                .filter { it.key.startsWith(KEY_IMAGE_PATH) }
                .map { it.value as String }

        //zip lại
        val zipFile = ImageUtils.createZipFile(applicationContext, imagePaths.toTypedArray())

        // zip xong truyền path của zip file cho worker sau
        outputData = Data.Builder()
                .putString(KEY_ZIP_PATH, zipFile.path)
                .build()

        Log.d(LOG_TAG, "Success!")
        Result.SUCCESS
    } catch (e: Throwable) {
        Log.e(LOG_TAG, "Error executing work: " + e.message, e)
        Result.FAILURE
    }

    companion object {
        private const val LOG_TAG = "CompressWorker"
        private const val KEY_IMAGE_PATH = "IMAGE_PATH"
        private const val KEY_ZIP_PATH = "ZIP_PATH"
    }
}