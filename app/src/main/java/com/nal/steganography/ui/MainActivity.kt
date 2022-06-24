package com.nal.steganography.ui

import android.Manifest
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ayush.imagesteganographylibrary.Text.AsyncTaskCallback.TextEncodingCallback
import com.ayush.imagesteganographylibrary.Text.ImageSteganography
import com.ayush.imagesteganographylibrary.Text.TextEncoding
import com.nal.steganography.R
import com.nal.steganography.Utility.LoadingDialog
import com.nal.steganography.Utility.lightStatusBar
import com.nal.steganography.Utility.uriToFile
import com.nal.steganography.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.random.Random

class MainActivity : AppCompatActivity(), TextEncodingCallback, View.OnClickListener {
    private var filepath: Uri? = null
    private var encodedImage: Bitmap? = null
    private var originalImage: Bitmap? = null

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                Toast.makeText(
                    this,
                    this.getString(R.string.not_permit),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lightStatusBar(window)

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        binding.btnOpen.setOnClickListener(this)
        binding.btnDecode.setOnClickListener(this)
        binding.tvDecode.setOnClickListener(this)
    }

    override fun onStartTextEncoding() {
        Toast.makeText(this, this.getString(R.string.still_encode), Toast.LENGTH_SHORT).show()
    }

    override fun onCompleteTextEncoding(imageSteganography: ImageSteganography?) {
        if (imageSteganography != null && imageSteganography.isEncoded) {
            encodedImage = imageSteganography.encoded_image
            binding.ivEncode.setImageBitmap(encodedImage)
            Toast.makeText(this, this.getString(R.string.still_encode), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, this.getString(R.string.not_encode), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btn_open -> {
                openGalley()
            }

            R.id.btn_decode -> {
                tryEncode()
            }

            R.id.tv_decode -> {
                val intent = Intent(this, DecodeActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun tryEncode() {
        val loading = LoadingDialog(this)
        val secretKey = binding.etPassword
        val message = binding.etMessage
        if (filepath != null) {
            if (!message.text.isNullOrEmpty()) {
                loading.startLoading()
                val imageSteganography = ImageSteganography(
                    message.text.toString(),
                    secretKey.text.toString(),
                    originalImage
                )
                val textEncoding = TextEncoding(this@MainActivity, this@MainActivity)
                textEncoding.execute(imageSteganography)

                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        loading.isDismiss()
                        val saveImageEncode = Thread {
                            kotlin.run { saveToStorage(encodedImage) }
                        }
                        if (encodedImage.toString().isNotEmpty()) {
                            saveImageEncode.start()
                            loading.dialogSuccess()
                            binding.apply {
                                etPassword.text = null
                                etMessage.text = null
                                tvSize.text = null
                                ivEncode.setImageDrawable(null)
                            }
                        } else {
                            loading.dialogFailed()
                        }
                    },
                    4000
                )
            } else {
                Toast.makeText(
                    this,
                    this.getString(R.string.insert_your_message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(this, this.getString(R.string.insert_image), Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveToStorage(originalImage: Bitmap?) {
        val number1 = Random.nextInt(1, 1000)
        val number2 = Random.nextInt(1, 1000)
        val fOut: OutputStream
        val name = "Encoded$number1$number2.PNG"
        val file = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ), name
        )

        fOut = FileOutputStream(file)
        originalImage?.compress(
            Bitmap.CompressFormat.PNG,
            10,
            fOut
        )

        fOut.flush()
        fOut.close()
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
            mediaScanIntent.data = Uri.fromFile(file)
            this.sendBroadcast(mediaScanIntent)
        }
    }

    private fun openGalley() {
        val intent = Intent()
        intent.action = ACTION_GET_CONTENT
        intent.type = "image/*"
        val chooser = Intent.createChooser(intent, "Choose a Picture")
        launcherIntentGallery.launch(chooser)
    }

    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            filepath = result.data?.data

            val input: InputStream? = contentResolver.openInputStream(filepath!!)
            originalImage = BitmapFactory.decodeStream(input)
            input?.close()

            val size = uriToFile(filepath!!, this)
            val getSize = size.length()/1000

            binding.tvSize.text = this.getString(R.string.file_size, getSize.toString())
            binding.ivEncode.setImageBitmap(originalImage)
        }
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        private const val REQUEST_CODE_PERMISSIONS = 10
    }
}