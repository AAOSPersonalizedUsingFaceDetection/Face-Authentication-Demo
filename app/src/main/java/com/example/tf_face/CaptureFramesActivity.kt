package com.example.tf_face

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CaptureFramesActivity : AppCompatActivity() {
    private var cameraHelper: CameraHelper? = null
    private var textureView: TextureView? = null
    private var captureButton: Button? = null
    private var statusText: TextView? = null
    private var databaseInitializer: DatabaseInitializer? = null
    private var faceDetector: BlazeFaceDetector? = null
    private var currentFrame = 0
    private val instructions = listOf(
        "Position 1: Face in the middle",
        "Position 2: Face tilted right",
        "Position 3: Face tilted left",
        "Position 4: Face tilted up",
        "Position 5: Face tilted down"
    )
    private val CAMERA_PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTheme()
        try {
            setContentView(R.layout.activity_capture_frames)
        } catch (e: Exception) {
            Log.e("CaptureFramesActivity", "Failed to set content view", e)
            Toast.makeText(this, "Error loading UI: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Initialize views
        textureView = findViewById(R.id.cameraTextureView)
        captureButton = findViewById(R.id.btnCapturePosition)
        statusText = findViewById(R.id.statusTextView)

        if (textureView == null || captureButton == null || statusText == null) {
            Log.e("CaptureFramesActivity", "Failed to initialize views")
            Toast.makeText(this, "UI initialization error", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        databaseInitializer = DatabaseInitializer(this)
        faceDetector = BlazeFaceDetector(this)
        val userName = intent.getStringExtra("user_name") ?: "user_${System.currentTimeMillis()}"

        // Initialize camera
        cameraHelper = CameraHelper(this, textureView!!)

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            initialize()
        }

        // Set initial instruction
        statusText?.text = instructions[currentFrame]

        captureButton?.setOnClickListener {
            captureFrame(userName)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initialize()
        } else {
            statusText?.text = "Camera permission denied"
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
        }
    }

    private fun initialize() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                databaseInitializer?.initializeDatabaseIfNeeded()
                cameraHelper?.startCamera()
                showNextInstruction()
            } catch (e: Exception) {
                statusText?.text = "Error initializing: ${e.message}"
                Toast.makeText(this@CaptureFramesActivity, "Initialization error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showNextInstruction() {
        if (currentFrame < instructions.size) {
            statusText?.text = instructions[currentFrame]
        }
    }


private fun captureFrame(userName: String) {
    CoroutineScope(Dispatchers.Main).launch {
        try {
            val bitmap = textureView?.bitmap ?: run {
                Log.w("CaptureFramesActivity", "Bitmap is null")
                statusText?.text = "Failed to capture frame"
                Toast.makeText(this@CaptureFramesActivity, "Camera not ready", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val faces = faceDetector?.detect(bitmap) ?: emptyList()
            if (faces.isEmpty()) {
                Log.w("CaptureFramesActivity", "No faces detected")
                statusText?.text = "No face detected. Please try again."
                Toast.makeText(this@CaptureFramesActivity, "No face detected", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val largestFace = faces.maxByOrNull { it.width() * it.height() } ?: return@launch
            val croppedFace = faceDetector?.cropFace(bitmap, largestFace) ?: return@launch
            val imageName = "${userName}_${currentFrame + 1}.jpeg"

            // Get the theme from intent extras
            val theme = intent.getStringExtra("user_theme") ?: "light"
            val success = databaseInitializer?.addFace(userName, croppedFace, imageName, theme) ?: false

            if (success) {
                currentFrame++
                Log.d("CaptureFramesActivity", "Captured frame $currentFrame for $userName")
                statusText?.text = "Captured frame $currentFrame for $userName"
                Toast.makeText(this@CaptureFramesActivity, "Captured frame $currentFrame", Toast.LENGTH_SHORT).show()
                if (currentFrame >= 5) {
                    Toast.makeText(this@CaptureFramesActivity, "All frames captured for $userName", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@CaptureFramesActivity, GreetingActivity::class.java)
                    intent.putExtra("user_name", userName)
                    startActivity(intent)
                    finish()
                } else {
                    showNextInstruction()
                }
            } else {
                Log.w("CaptureFramesActivity", "Failed to capture frame $currentFrame")
                statusText?.text = "Failed to capture frame $currentFrame for $userName"
                Toast.makeText(this@CaptureFramesActivity, "Failed to capture frame", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("CaptureFramesActivity", "Error capturing frame", e)
            statusText?.text = "Error capturing frame: ${e.message}"
            Toast.makeText(this@CaptureFramesActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
    private fun applyTheme() {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val theme = sharedPreferences.getString("theme", "light")
        when (theme) {
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        faceDetector?.close()
    }
}