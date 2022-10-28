package com.leveron.cameraq

import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Debug
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.leveron.cameraq.Constants.TAG
import com.leveron.cameraq.R
import com.leveron.cameraq.databinding.ActivityMainBinding
import java.io.File
import java.io.FileFilter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
/*
D ozrobienia TODO - trzeba zrobi© odswierzenie lisdty folderow na spinnerze
TODO - trzeba wyrzuci© spinnera na ofdzielny folder
TODO trzeba ograc przypadek gdy nie ma folderow wtedy poleci crash przy zdjeciu
TODO  trzeba jeszcze poukladac ikonki





 */
    private lateinit var binding: ActivityMainBinding
    private var  imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(R.layout.activity_main)

        var spinner1 = binding.txtFolder
        var btnAdd = binding.btnAddFolder
        var txtInputFolderName = binding.txtInputFolderName
        var listOfDirs : File = getOutputDirectory()
        lateinit var folderSelected : String

        setContentView(binding.root)
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

    if (allPermissionGranted()) {
        startCamera()
    }else{
        ActivityCompat.requestPermissions(
            this, Constants.REQUIRED_PERMISSIONS,
            Constants.REQUEST_CODE_PERMISSIONS
        )


    }


        binding.btnStrzal.setOnClickListener {
            takePhoto()
        }

        binding.btnStrzal.setOnLongClickListener {
            if (spinner1.visibility == View.VISIBLE)
            {
                spinner1.visibility = View.INVISIBLE
                btnAdd.visibility = View.INVISIBLE
                txtInputFolderName.visibility = View.INVISIBLE

            }else{
                spinner1.visibility = View.VISIBLE
                btnAdd.visibility = View.VISIBLE
                txtInputFolderName.visibility = View.VISIBLE
            }


            true
        }

        binding.btnAddFolder.setOnClickListener {
            val newFolder : File
            //var newFolderString : String = listOfDirs.toString()
            //newFolderString += "\" + $folderSelected

            if (txtInputFolderName.length()==0){
                Toast.makeText(this, "Folder name required", Toast.LENGTH_SHORT).show()
            }else{
                newFolder = File(listOfDirs.toString() + "/"+ txtInputFolderName.text)
                Log.d("aaaa", listOfDirs.toString())
                Log.d("aaaa", listOfDirs.toString() + "/"+ txtInputFolderName.text)

                if (newFolder.exists()) {
                    Toast.makeText(this, "Folder already exist", Toast.LENGTH_SHORT).show()
                }else{
                    newFolder.mkdirs()
                    Toast.makeText(this, "Folder $txtInputFolderName created", Toast.LENGTH_SHORT).show()
                }



            }

        }


// Tworzymy spinner adapter z lista folderow

        var files : Array<File>
        var spinnerList = arrayListOf<String>("Foldery:")
        spinnerList.clear()
        files = listOfDirs.listFiles(FileFilter { it.isDirectory })!!


        Log.d("aaaa", listOfDirs.toString())
        for (a in  files) {
            spinnerList.add(a.name.toString())
            Log.d("aaaa", a.name.toString())

        }


        val adapter = ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, spinnerList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)


        spinner1.adapter = adapter

        spinner1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                Toast.makeText(this@MainActivity, "You selected:" +  p0?.getItemAtPosition(p2).toString(),
                    Toast.LENGTH_LONG).show()
                folderSelected = p0?.getItemAtPosition(p2).toString()

            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }


        }



    }

    private fun getOutputDirectory(): File{
        val mediaDir = externalMediaDirs.firstOrNull()?.let { mFile->
            File(mFile, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }


    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
                outputDirectory,
                SimpleDateFormat(
                    Constants.FILE_NAME_FORMAT,
                        Locale.getDefault())
                        .format(System
                            .currentTimeMillis()) + ".jpg")

        val outputOption = ImageCapture
                            .OutputFileOptions
                            .Builder(photoFile)
                            .build()

        imageCapture.takePicture(
            outputOption, ContextCompat.getMainExecutor(this),
            object :ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo saved"

                    Toast.makeText(
                        this@MainActivity,
                        "$msg $savedUri",
                        Toast.LENGTH_LONG
                    ).show()




                }

                override fun onError(exception: ImageCaptureException) {
                   Log.e(Constants.TAG, "onError: ${exception.message}", exception)
                }


            }
        )


    }


    private fun startCamera(){
        val  cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {mPreview->
                    mPreview.setSurfaceProvider(
                        binding.viewFinder.surfaceProvider

                    )

                }
            imageCapture = ImageCapture.Builder()
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle( this, cameraSelector,
                    preview, imageCapture)
            }catch (e: Exception){
            Log.d(Constants.TAG,"Camera Fail", e)

            }

        }, ContextCompat.getMainExecutor(this)  )


    }




    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray    ) {

        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS) {
            startCamera()
        } else {
            Toast.makeText(this,  "Permission not granted by User" , Toast.LENGTH_SHORT).show()
            finish()

        }
    }




    private fun allPermissionGranted() =
        Constants.REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                baseContext, it
            ) == PackageManager.PERMISSION_GRANTED
        }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }






}