package com.raywenderlich.android.photouploader.worker

import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import com.raywenderlich.android.photouploader.ImageUtils

/**
 * Worker này thực thi filter ảnh được pick từ thư viênj
 */
class FilterWorker : Worker() {
    override fun doWork(): Result  = try {

        //nhận input data - từ WorkRequest gọi cái này
        val imageUriString = inputData.getString(KEY_IMAGE_URI)
        val imageIndex = inputData.getInt(KEY_IMAGE_INDEX, 0)

        val bitmap = MediaStore.Images.Media.getBitmap(applicationContext.contentResolver, Uri.parse(imageUriString))

        val filteredBitmap = ImageUtils.applySepiaFilter(bitmap)
        val filteredImageUri = ImageUtils.writeBitmapToFile(applicationContext, filteredBitmap)

        outputData =
                Data.Builder()
                        .putString(IMAGE_PATH_PREFIX + imageIndex, filteredImageUri.toString())
                        .build()

        Log.d(LOG_TAG, "Success!")
        Result.SUCCESS
    } catch (e: Throwable) {
        Log.e(LOG_TAG, "Error executing work: " + e.message, e)
        Result.FAILURE
    }

    companion object {
        private const val LOG_TAG = "FilterWorker"
        private const val IMAGE_PATH_PREFIX = "IMAGE_PATH_"
        const val KEY_IMAGE_URI = "IMAGE_URI"
        const val KEY_IMAGE_INDEX = "IMAGE_INDEX"
    }
}