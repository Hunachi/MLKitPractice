package io.github.hunachi.mlkitpractice

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.max


class MainActivity : AppCompatActivity(), ImagePickFragment.ImagePickListener {
    
    private var bitmap: Bitmap? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val detectors = listOf(
            TEXT_DETECTION,
            FACE_DETECTION,
            BARCODE_DETECTION,
            LABELING,
            CLOUD_LABELING,
            CLOUD_TEXT_DETECTION
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
        
        overlay.targetWidth = targetWidth
        overlay.targetHeight = targetHeight
    }
    
    private fun detect(bitmap: Bitmap) {
        
        val detectorName = detectorSpinner.selectedItem as String
        when (detectorName) {
            TEXT_DETECTION       -> {
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
                                    overlay.add(
                                        GraphicData(
                                            element.text,
                                            element.boundingBox ?: Rect(),
                                            resources,
                                            Color.BLUE
                                        )
                                    )
                                    Log.d(TAG, "${element.text}, ${element.boundingBox}")
                                }
                            }
                        }
                        
                        if (text.blocks.size <= 0) {
                            Toast.makeText(this, "cannot find any tests", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        detectButton.isEnabled = true
                        e.printStackTrace()
                    }
            }
            FACE_DETECTION       -> {
                detectButton.isEnabled = false
                
                val image = FirebaseVisionImage.fromBitmap(bitmap)
                
                val option = FirebaseVisionFaceDetectorOptions.Builder()
                    .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                    .setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                    .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                    .setMinFaceSize(0.15f)
                    .setTrackingEnabled(true)
                    .build()
                
                FirebaseVision.getInstance()
                    .getVisionFaceDetector(option)
                    .detectInImage(image)
                    .addOnSuccessListener { faces ->
                        detectButton.isEnabled = true
                        
                        overlay.clear()
                        
                        for (face in faces) {
                            overlay.add(
                                GraphicData(
                                    "",
                                    face.boundingBox ?: Rect(),
                                    resources,
                                    Color.BLUE
                                )
                            )
                            Log.d(TAG, "${face.smilingProbability}, ${face.boundingBox}")
                        }
                        
                        if (faces.size <= 0) {
                            Toast.makeText(this, "cannot find any faces", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        detectButton.isEnabled = true
                        e.printStackTrace()
                    }
            }
            BARCODE_DETECTION    -> {
                detectButton.isEnabled = false
                
                val image = FirebaseVisionImage.fromBitmap(bitmap)
                
                val option = FirebaseVisionBarcodeDetectorOptions.Builder()
                    .setBarcodeFormats(
                        FirebaseVisionBarcode.FORMAT_EAN_8,
                        FirebaseVisionBarcode.FORMAT_EAN_13
                    )
                    .build()
                
                FirebaseVision.getInstance()
                    .getVisionBarcodeDetector(option)
                    .detectInImage(image)
                    .addOnSuccessListener { barcodes ->
                        detectButton.isEnabled = true
                        
                        overlay.clear()
                        
                        for (barcode in barcodes) {
                            overlay.add(
                                GraphicData(
                                    barcode.rawValue ?: "",
                                    barcode.boundingBox ?: Rect(),
                                    resources,
                                    Color.BLUE
                                )
                            )
                            Log.d(TAG, "${barcode.rawValue}, ${barcode.boundingBox}")
                        }
                        if (barcodes.size <= 0) {
                            Toast.makeText(this, "cannot find any barcodes", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    .addOnFailureListener { e ->
                        detectButton.isEnabled = true
                        e.printStackTrace()
                    }
            }
            LABELING             -> {
                detectButton.isEnabled = false
                
                val image = FirebaseVisionImage.fromBitmap(bitmap)
                
                val option = FirebaseVisionLabelDetectorOptions.Builder()
                    .setConfidenceThreshold(0.8f)
                    .build()
                
                FirebaseVision.getInstance()
                    .getVisionLabelDetector(option)
                    .detectInImage(image)
                    .addOnSuccessListener { labels ->
                        detectButton.isEnabled = true
                        
                        overlay.clear()
                        
                        for (label in labels) {
                            //TODO  dekitara hyuoji
                            Log.d("MainActivity", "${label.label}, ${label.confidence}")
                        }
                        if (labels.size <= 0) {
                            Toast.makeText(this, "cannot find any labels", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    .addOnFailureListener { e ->
                        detectButton.isEnabled = true
                        e.printStackTrace()
                    }
            }
            CLOUD_LABELING       -> {
                detectButton.isEnabled = false
                
                val image = FirebaseVisionImage.fromBitmap(bitmap)
                
                val options = FirebaseVisionCloudDetectorOptions.Builder()
                    .setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
                    .setMaxResults(15)
                    .build()
                
                FirebaseVision.getInstance()
                    .getVisionCloudLabelDetector(options)
                    .detectInImage(image)
                    .addOnSuccessListener { labels ->
                        detectButton.isEnabled = true
                        
                        overlay.clear()
                        
                        for (label in labels) {
                            Log.d(TAG, "${label.label}, ${label.confidence}")
                        }
                        if (labels.size <= 0) {
                            Toast.makeText(this, "cannot find any labels", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    .addOnFailureListener { e ->
                        detectButton.isEnabled = true
                        e.printStackTrace()
                    }
            }
            CLOUD_TEXT_DETECTION -> {
                detectButton.isEnabled = false
                
                val image = FirebaseVisionImage.fromBitmap(bitmap)
                
                val options = FirebaseVisionCloudDetectorOptions.Builder()
                    .setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
                    .setMaxResults(15)
                    .build()
                
                FirebaseVision.getInstance()
                    .getVisionCloudTextDetector(options)
                    .detectInImage(image)
                    .addOnSuccessListener { cloudText ->
                        detectButton.isEnabled = true
                        
                        overlay.clear()
                        
                        for (page in cloudText.pages) {
                            for (block in page.blocks) {
                                for (paragraph in block.paragraphs) {
                                    for (word in paragraph.words) {
                                        val text =
                                            word.symbols.joinToString(separator = "") { it.text }
                                        overlay.add(
                                            GraphicData(
                                                text,
                                                word.boundingBox ?: Rect(),
                                                resources,
                                                Color.BLUE
                                            )
                                        )
                                        Log.d(TAG, "$text, ${word.boundingBox}")
                                    }
                                }
                            }
                        }
                        if (cloudText.pages.size <= 0) {
                            Toast.makeText(this, "cannot find any tests", Toast.LENGTH_SHORT).show()
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
        const val TAG = "MainActivity"
        
        private const val TEXT_DETECTION = "Text"
        private const val FACE_DETECTION = "Face"
        private const val BARCODE_DETECTION = "Barcode"
        private const val LABELING = "Label"
        private const val CLOUD_LABELING = "Cloud label"
        private const val CLOUD_TEXT_DETECTION = "Cloud text"
        
    }
    
}