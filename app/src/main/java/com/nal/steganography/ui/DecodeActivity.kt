package com.nal.steganography.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.ayush.imagesteganographylibrary.Text.AsyncTaskCallback.TextDecodingCallback
import com.ayush.imagesteganographylibrary.Text.ImageSteganography
import com.ayush.imagesteganographylibrary.Text.TextDecoding
import com.nal.steganography.R
import com.nal.steganography.utility.lightStatusBar
import com.nal.steganography.utility.uriToFile
import com.nal.steganography.databinding.ActivityDecodeBinding

class DecodeActivity : AppCompatActivity(), TextDecodingCallback, View.OnClickListener {
    private var filepath: Uri? = null
    private var originalImage: Bitmap? = null

    private var _binding: ActivityDecodeBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityDecodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lightStatusBar(window)

        binding.btnOpen.setOnClickListener(this)
        binding.btnDecode.setOnClickListener(this)
        binding.tvEncode.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btn_open -> {
                val intent = Intent()
                intent.action = Intent.ACTION_GET_CONTENT
                intent.type = "image/*"
                val chooser = Intent.createChooser(intent, "Choose a Picture")
                launcherIntentGallery.launch(chooser)
            }
            R.id.btn_decode -> {
                if (filepath != null) {
                    val imagesteganographydecode =
                        ImageSteganography(binding.etPassword.text.toString(), originalImage)
                    val textDecoding = TextDecoding(this, this)
                    textDecoding.execute(imagesteganographydecode)
                } else {
                    Toast.makeText(this, this.getString(R.string.insert_image), Toast.LENGTH_SHORT).show()
                }
            }
            R.id.tv_encode -> {
                onBackPressed()
            }
        }
    }

    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            filepath = result.data?.data

            originalImage = MediaStore.Images.Media.getBitmap(this.contentResolver,filepath!!)

            val size = uriToFile(filepath!!, this)
            val getSize = size.length()/1000

            binding.tvSize.text = this.getString(R.string.file_size, getSize.toString())
            binding.ivDecode.setImageBitmap(originalImage)
        }
    }

    override fun onStartTextEncoding() {

    }

    override fun onCompleteTextEncoding(imageSteganography: ImageSteganography?) {
        if(imageSteganography!=null && imageSteganography.isDecoded) {
            if (!imageSteganography.isSecretKeyWrong){
                binding.etMessage.setText(imageSteganography.message)
                binding.etPassword.text = null
                Toast.makeText(this, this.getString(R.string.decode_success), Toast.LENGTH_SHORT).show()
            }
            else{
                binding.etMessage.text = null
                Toast.makeText(this,this.getString(R.string.wrong_password),Toast.LENGTH_SHORT).show()
            }
        }
        else{
            binding.etMessage.text = null
            Toast.makeText(this, this.getString(R.string.not_decode),Toast.LENGTH_SHORT).show()
        }
    }
}