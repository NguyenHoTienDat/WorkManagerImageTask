/*
 * Copyright (c) 2018 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.android.photouploader.workers

import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import com.raywenderlich.android.photouploader.ImageUtils

private const val LOG_TAG = "CompressWorker"
private const val KEY_IMAGE_PATH = "IMAGE_PATH"
private const val KEY_ZIP_PATH = "ZIP_PATH"

class CompressWorker : Worker() {

  override fun doWork(): WorkerResult = try {
    // Sleep for debugging purposes
    Thread.sleep(3000)
    Log.d(LOG_TAG, "Compressing files!")

    val imagePaths = inputData.keyValueMap
        .filter { it.key.startsWith(KEY_IMAGE_PATH) }
        .map { it.value as String }

    val zipFile = ImageUtils.createZipFile(applicationContext, imagePaths.toTypedArray())

    outputData = Data.Builder()
        .putString(KEY_ZIP_PATH, zipFile.path)
        .build()

    Log.d(LOG_TAG, "Success!")
    WorkerResult.SUCCESS
  } catch (e: Throwable) {
    Log.e(LOG_TAG, "Error executing work: " + e.message, e)
    WorkerResult.FAILURE
  }
}