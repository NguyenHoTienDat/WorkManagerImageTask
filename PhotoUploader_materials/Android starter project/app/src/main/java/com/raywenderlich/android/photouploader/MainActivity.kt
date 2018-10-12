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

package com.raywenderlich.android.photouploader

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.raywenderlich.android.photouploader.worker.CompressWorker
import com.raywenderlich.android.photouploader.worker.FilterWorker
import com.raywenderlich.android.photouploader.worker.FilterWorker.Companion.KEY_IMAGE_INDEX
import com.raywenderlich.android.photouploader.worker.FilterWorker.Companion.KEY_IMAGE_URI
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val GALLERY_REQUEST_CODE = 300
        private const val PERMISSIONS_REQUEST_CODE = 301

        private const val MAX_NUMBER_REQUEST_PERMISSIONS = 2

        private const val IMAGE_TYPE = "image/*"
        private const val IMAGE_CHOOSER_TITLE = "Select Picture"

        private const val UNIQUE_WORK_NAME = "UNIQUE_WORK_NAME"

        private val PERMISSIONS = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private var permissionRequestCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initUi()

        requestPermissionsIfNecessary()
    }

    private fun initUi() {
        uploadGroup.visibility = View.GONE

        pickPhotosButton.setOnClickListener { showPhotoPicker() }
    }

    /**
     * Mở thư viện ảnh
     */
    private fun showPhotoPicker() {
        val intent = Intent().apply {
            type = IMAGE_TYPE
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            action = Intent.ACTION_OPEN_DOCUMENT
        }

        startActivityForResult(Intent.createChooser(intent, IMAGE_CHOOSER_TITLE), GALLERY_REQUEST_CODE)
    }

    private fun requestPermissionsIfNecessary() {
        if (!hasRequiredPermissions()) {
            askForPermissions()
        }
    }

    private fun askForPermissions() {
        if (permissionRequestCount < MAX_NUMBER_REQUEST_PERMISSIONS) {
            permissionRequestCount += 1

            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_REQUEST_CODE)
        } else {
            pickPhotosButton.isEnabled = false
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissionResults = PERMISSIONS.map { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PERMISSION_GRANTED
        }

        return permissionResults.all { isGranted -> isGranted }
    }

    override fun onRequestPermissionsResult(
            code: Int,
            permissions: Array<String>,
            result: IntArray) {
        super.onRequestPermissionsResult(code, permissions, result)
        if (code == PERMISSIONS_REQUEST_CODE) {
            requestPermissionsIfNecessary()
        }
    }

    /**
     * Nhận về data ảnh
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, dataItent: Intent?) {
        if (dataItent != null && resultCode == Activity.RESULT_OK && requestCode == GALLERY_REQUEST_CODE) {
            val applySepiaFilter = buildSepiaFilterRequests(dataItent)

            val workManager = WorkManager.getInstance()
            val zipFiles = OneTimeWorkRequest.Builder(CompressWorker::class.java).build()

            //do task filter chỉ là task đầu tiên. Sau đó ta con có task tiếp theo là zip chúng lại
            // nên phải dùng Task Chain, bắt đầu của chuỗi là task lọc này
            workManager.beginUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, applySepiaFilter).then(zipFiles).enqueue()

            //dùng beginUniqueWork mà kp begin bt
            //vd ta ấn update -> tắt app -> bât app và ấn update lại
            //thực chất đang có 2 task chain, 1 chain lần trc chưa đc ex và 1 chain vừa đc ấn
            //nếu begin như bt thì cả 2 cái sẽ đều đc chạy k cần thiết.Ở đây ta luôn Replace cái mới cho cái cũ
        }
    }

    /**
     * Nhận data từ activity result để tạo Work Request.
     * Như đã nói thì fun beginWith() của chain, nếu ta truyền vào chỉ 1 task thì có thể request đó là ở dạng single hay list
     * Nhưng nếu nó begin với từ 2 task trở nên thì phải có kiểu là List như fun này.
     * Tất nhiên nếu muốn thực hiện >=2 task từ đầu mà k muốn dùng list thì ta cứ pass lần lượt fun(task1,task2,..)
     *
     * Khi chọn ảnh thì sẽ có 2 case, 1 là chỉ chọn 1 ảnh, 2 là chọn nhiều
     */
    private fun buildSepiaFilterRequests(dataItent: Intent): List<OneTimeWorkRequest> {
        val filterRequests = mutableListOf<OneTimeWorkRequest>()

        //nếu trong case chọn nhiều ảnh thì sẽ loop
        dataItent.clipData?.run {
            for (i in 0 until itemCount) {
                val imageUri = getItemAt(i).uri

                val filterRequest = OneTimeWorkRequest.Builder(FilterWorker::class.java)
                        .setInputData(buildInputDataForFilter(imageUri, i))
                        .build()
                filterRequests.add(filterRequest)
            }
        }

        //nếu chỉ chọn 1 ảnh
        dataItent.data?.run {
            val filterWorkRequest = OneTimeWorkRequest.Builder(FilterWorker::class.java)
                    .setInputData(buildInputDataForFilter(this, 0))
                    .build()

            filterRequests.add(filterWorkRequest)
        }

        return filterRequests
    }

    /**
     * Fun tạo ra input data -> nhận infor về image, tạo input -> pass cho filter request -> pass cho worker
     */
    private fun buildInputDataForFilter(imageUri: Uri?, index: Int): Data {
        val builder = Data.Builder()
        if (imageUri != null) {
            builder.putString(KEY_IMAGE_URI, imageUri.toString())
            builder.putInt(KEY_IMAGE_INDEX, index)
        }
        return builder.build()
    }
}
