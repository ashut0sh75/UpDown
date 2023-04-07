package com.example.updown

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.updown.databinding.ActivityMainBinding
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageUri: Uri? = null
    private lateinit var storageReference: StorageReference
    private lateinit var progressDialog: ProgressDialog
    private lateinit var firestoreDB: FirebaseFirestore

    private val PICK_IMAGE = 0
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_PERMISSION = 100
    private val PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check for camera and storage permissions
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION)
        }

        binding.selectImagebtn.setOnClickListener {
            selectImage()
        }

        binding.uploadimagebtn.setOnClickListener {
            uploadImage()
        }

        binding.downbtn.setOnClickListener {
            val intent = Intent(this, retrieve::class.java)
            startActivity(intent)
        }

        firestoreDB = FirebaseFirestore.getInstance()
    }

    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadImage() {
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Uploading File....")
        progressDialog.show()

        val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.CANADA)
        val now = Date()
        val fileName = formatter.format(now)
        storageReference = FirebaseStorage.getInstance().getReference("images/$fileName")

        // Add null check here
        if (imageUri != null) {
            storageReference.putFile(imageUri!!)
                .addOnSuccessListener {
                    binding.firebaseimage.setImageURI(null)
                    Toast.makeText(this, "Successfully Uploaded", Toast.LENGTH_SHORT).show()
                    if (progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }

                    // Get the download URL of the uploaded image and add it to Firestore
                    storageReference.downloadUrl.addOnSuccessListener { uri ->
                        val map = HashMap<String, Any>()
                        map["pic"] = uri.toString()
                        firestoreDB.collection("images")
                            .add(map)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Image added to Firestore", Toast.LENGTH_SHORT)
                                    .show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Error adding image to Firestore: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }.addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Failed to get download URL: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                }
                .addOnFailureListener { e ->
                    if (progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                    Toast.makeText(this, "Failed to Upload: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
        } else {
            Toast.makeText(this, "Please select an image to upload", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
        }
    }


    private fun selectImage() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Image Source")
        builder.setItems(arrayOf("Gallery", "Camera")) { _, which ->
            when (which) {
                0 -> {
                    // Launch the image picker from the gallery
                    val intent = Intent(Intent.ACTION_PICK)
                    intent.type = "image/*"
                    startActivityForResult(intent, PICK_IMAGE)
                }
                1-> {
                    // Launch the camera to take a picture
                    val imageFileName = "JPEG_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    val imageFile = File.createTempFile(
                        imageFileName,  /* prefix */
                        ".jpg",  /* suffix */
                        storageDir /* directory */
                    )
                    imageUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", imageFile)
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                    startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
        val dialog = builder.create()
        dialog.show()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE -> {
                    imageUri = data?.data
                    binding.firebaseimage.setImageURI(imageUri)
                }
                REQUEST_IMAGE_CAPTURE -> {
                    binding.firebaseimage.setImageURI(imageUri)
                }
            }
        }
    }
}


