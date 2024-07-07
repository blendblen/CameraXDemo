package com.blendspace.example.cameraxdemo

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.blendspace.example.cameraxdemo.databinding.ActivityMainBinding
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
        viewBinding.switchButton.setOnClickListener { switchCamera() }
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSION)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun takePhoto() {
//        Log.d(TAG, "taskPhoto")
        val imageCap = imageCapture ?: return
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.CHINA).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()
        imageCap.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object: ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val msg = "success to capture photo: ${outputFileResults.savedUri}"
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "failed to capture photo: $exception", exception)
            }
        })

    }

    private fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera()

    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener ( {
            Log.d(TAG, "startCamera: ")
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(this,
                                                            cameraSelector,
                                                            preview,
                                                            imageCapture)
                val cameraInfo = camera.cameraInfo
                val cameraControl = camera.cameraControl
//                cameraControl.enableTorch(true)
                // 创建手势监听器
                val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                        override fun onScroll(
                            e1: MotionEvent?,
                            e2: MotionEvent,
                            distanceX: Float,
                            distanceY: Float
                        ): Boolean {
//                            super.onScroll(e1, e2, distanceX, distanceY)
                            val curZoomRatio = cameraInfo.zoomState.value?.zoomRatio ?: 1f
                            val delta = (e1?.y ?: 0f) - e2.y
                            val scale = delta / viewBinding.viewFinder.height.toFloat()
                            val zoomRatio = curZoomRatio + scale
                            val maxZoomRatio = cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
                            cameraControl.setZoomRatio(zoomRatio.coerceIn(0f, maxZoomRatio))
                            return true
                        }
                    })
                viewBinding.viewFinder.setOnTouchListener {
                    _, event ->
                        gestureDetector.onTouchEvent(event)
                        true


                }
            } catch (e: Exception) {
                Log.e(TAG, "startCamera, e = $e")
            }
        }, ContextCompat.getMainExecutor(this) )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "permission not granted by the user", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "CameraDemo"
        private const val REQUEST_CODE_PERMISSION = 1001
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
               add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.READ_MEDIA_IMAGES)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
        const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    }


}