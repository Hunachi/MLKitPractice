package io.github.hunachi.mlkitpractice

import android.graphics.Bitmap
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ArrayAdapter
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.max


class MainActivity : AppCompatActivity(), ImagePickFragment.ImagePickListener {
    
    private var bitmap: Bitmap? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val detectors = listOf(
            TEXT_DETECTION
            // TODO: 3
        )
        detectorSpinner.adapter =
                ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, detectors)
                    .apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
        
        detectButton.setOnClickListener {
            bitmap?.let { detect(it) }
        }
    }
    
    override fun onImagePicked(imageUri: Uri) {
        val imageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
        
        val scaleFactor = max(
            imageBitmap.width.toFloat() / imageView.width.toFloat(),
            imageBitmap.height.toFloat() / imageView.height.toFloat()
        )
        
        val targetWidth = (imageBitmap.width / scaleFactor).toInt()
        val targetHeight = (imageBitmap.height / scaleFactor).toInt()
        
        bitmap = Bitmap.createScaledBitmap(
            imageBitmap,
            targetWidth,
            targetHeight,
            true
        )
        
        imageView.setImageBitmap(bitmap)
    }
    
    private fun detect(bitmap: Bitmap) {
        
        val detectorName = detectorSpinner.selectedItem as String
        when (detectorName) {
            TEXT_DETECTION -> {
                detectButton.isEnabled = false
                
                val image = FirebaseVisionImage.fromBitmap(bitmap)
                
                FirebaseVision.getInstance()
                    .visionTextDetector
                    .detectInImage(image)
                    .addOnSuccessListener { text ->
                        detectButton.isEnabled = true
                        
                        for (block in text.blocks) {
                            for (line in block.lines) {
                                for (element in line.elements) {
                                    Log.d("MainActivity", "${element.text}, ${element.boundingBox}")
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        detectButton.isEnabled = true
                        e.printStackTrace()
                    }
            }
        }
    }
    
    companion object {
        private const val TEXT_DETECTION = "Text"
    }
    
}