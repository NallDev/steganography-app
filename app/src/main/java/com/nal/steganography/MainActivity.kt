package com.nal.steganography

import android.Manifest
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ayush.imagesteganographylibrary.Text.AsyncTaskCallback.TextEncodingCallback
import com.ayush.imagesteganographylibrary.Text.ImageSteganography
import com.ayush.imagesteganographylibrary.Text.TextEncoding
import com.nal.steganography.Utility.uriToFile
import com.nal.steganography.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.random.Random

class MainActivity : AppCompatActivity(), TextEncodingCallback, View.OnClickListener {
    private var getFile: File? = null
    private var filepath: Uri? = null
    private var imageEncode: Bitmap? = null
    private var _binding : ActivityMainBinding? = null
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
                    "Tidak mendapatkan permission.",
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

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        binding.btnOpen.setOnClickListener(this)
        binding.btnDecode.setOnClickListener(this)
    }

    override fun onStartTextEncoding() {
        Toast.makeText(this,"Lagi Encoded", Toast.LENGTH_SHORT).show()
    }

    override fun onCompleteTextEncoding(p0: ImageSteganography?) {
        if (p0 != null && p0.isEncoded()) {
            binding.ivDecode.setImageBitmap(imageEncode)
            Toast.makeText(this,"Encoded", Toast.LENGTH_SHORT).show()
        }
        else{
            Toast.makeText(this,"Not Encoded", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onClick(view: View?) {
        when(view?.id){
            R.id.btn_open -> {
                openGalley()
            }

            R.id.btn_decode -> {
                val secret_key = binding.etPassword
                val message= binding.etMessage
                imageEncode = BitmapFactory.decodeFile(getFile?.path)
                if(filepath!=null){
                    if (binding.etMessage.text != null){
                        if(binding.etMessage.text != null){
                            val imageSteganography = ImageSteganography(
                                message.text.toString(),
                                secret_key.text.toString(),
                                imageEncode
                            )
                            val textEncoding = TextEncoding(this@MainActivity, this@MainActivity)
                            textEncoding.execute(imageSteganography)

                            val saveImageEncode = Thread(Runnable {
                                kotlin.run { saveToStorage(imageEncode) }
                            })
                            saveImageEncode.start()
                        }
                        else{
                            Toast.makeText(this,"Masukan Message", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else{
                        Toast.makeText(this,"Masukan Key", Toast.LENGTH_SHORT).show()
                    }
                }
                else{
                    Toast.makeText(this,"Masukan Gambar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveToStorage(originalImage: Bitmap?) {
        val number1 = Random.nextInt(1,1000)
        val number2 = Random.nextInt(1,1000)
        val fOut: OutputStream
        val nama = "Encoded$number1$number2.PNG"
        val file = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ), nama
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
            val f = file
            mediaScanIntent.data = Uri.fromFile(f)
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
            filepath = result.data?.data as Uri

            val myFile = uriToFile(filepath!!, this@MainActivity)

            getFile = myFile

            Log.e("size", myFile.length().toString())

            binding.ivDecode.setImageURI(filepath)
        }
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        private const val REQUEST_CODE_PERMISSIONS = 10
    }
}