package com.leveron.cameraq

import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Debug
import android.os.Environment
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
import com.leveron.cameraq.Constants.APP_NAME
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
Dozrobienia

TODO - trzeba wyrzuci© spinnera na odzielny folder


TODO - toast - zminimalizowac i skrócić.  max 2-3 wyrazy
TODO - toast dać niżej bo wchodzi na przycisk
TODO - przełączenie pomiędzy zoomami z 0,0 na  zoom 0,5   / dwustanowy guzik /

DONE - po dodaniu folderu nowego ustaw na niego focus - ZROBIONE
DONE - trzeba zrobi© odswierzenie lisdty folderow na spinnerze - ZROBIONE
DONE- jak sie chowa spinner to aktualny folder na gorze sie wyświetli - ZROBIONE
DONE - trzeba jeszcze poukladac ikonki - ZROBIONE
DONE - posortowanie na liście od a do z - ZROBIONE
DONE -  trzeba ograc przypadek gdy nie ma folderow wtedy poleci crash przy zdjeciu  -> jak nie ma folderu to wgrywamy do głównego aplikacji
 */
    private lateinit var binding: ActivityMainBinding
    private var  imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    lateinit var folderSelected : String
    lateinit var folderSelectedPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(R.layout.activity_main)

        //sprawdzam datę i wersję programu
        checkDate()





        setContentView(binding.root)
        outputDirectory = getOutputDirectory()
        folderSelectedPath = ""
        cameraExecutor = Executors.newSingleThreadExecutor()

    if (allPermissionGranted()) {
        startCamera()
    }else{
        ActivityCompat.requestPermissions(
            this, Constants.REQUIRED_PERMISSIONS,
            Constants.REQUEST_CODE_PERMISSIONS
        )


    }

        var spinner1 = binding.txtFolder
        var btnAdd = binding.btnAddFolder
        var txtInputFolderName = binding.txtInputFolderName
        var listOfDirs : File = getOutputDirectory()



        fun reloadSpinnerAdapter(text : String) {


            var files: Array<File>
            var spinnerList = arrayListOf<String>("Foldery:")
            spinnerList.clear()

                files = listOfDirs.listFiles(FileFilter { it.isDirectory })!!
                files.sort()



            Log.d("aaaa", listOfDirs.toString())
            for (a in files) {
                spinnerList.add(a.name.toString())
                Log.d("aaaa", a.name.toString())

            }


            val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, spinnerList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)


            spinner1.adapter = adapter

            if (text.isNotEmpty())  spinner1.setSelection(adapter.getPosition(text))

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


            if (txtInputFolderName.length()==0){
                Toast.makeText(this, "Folder name required", Toast.LENGTH_SHORT).show()
            }else{
                newFolder = File(listOfDirs.toString() + "/"+ txtInputFolderName.text)
                Log.d("cccc", listOfDirs.toString())
                Log.d("cccc", listOfDirs.toString() + "/"+ txtInputFolderName.text)

                if (newFolder.exists()) {
                    Toast.makeText(this, "Folder already exist", Toast.LENGTH_SHORT).show()
                }else{
                    newFolder.mkdirs()
                    Toast.makeText(this, "Folder " + txtInputFolderName.text + " created", Toast.LENGTH_SHORT).show()
                }



            }
        //dodałeś folder to przeładuj listę folderów w adapterze spinner
            reloadSpinnerAdapter(txtInputFolderName.text.toString())
            //czysc folder
            txtInputFolderName.text = null


        }




        reloadSpinnerAdapter(text="")
        spinner1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                Toast.makeText(this@MainActivity, "You selected:" +  p0?.getItemAtPosition(p2).toString(),
                    Toast.LENGTH_SHORT).show()
                folderSelected = p0?.getItemAtPosition(p2).toString()
                //ustaw folder na gornej belce
                title = APP_NAME + " - " + folderSelected

                //ustawe nowa sciezke do plikow
                folderSelectedPath = listOfDirs.toString() + "/" + folderSelected

            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }


        }



    }

    private fun getOutputDirectory(): File{
        /*var mediaDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath().let { mFile ->
            File(mFile, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }   */
        var mediaDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()
        var mFile = File(mediaDir + "/" + resources.getString(R.string.app_name))

        if (mFile.exists() == true) {
            return mFile
        }else{
            mFile.mkdirs()
            return mFile
        }



        /*
        val mediaDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).let { mFile->
            File(mFile, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }*/

        Log.d("bbbb", mFile.toString())
        Log.d("bbbb", "tu:" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath())
        //Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()
       // return if (mediaDir != null && mediaDir.exists())
       //     mediaDir else filesDir

    }


    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        if (folderSelectedPath == "") {
            folderSelectedPath = getOutputDirectory().toString()
            folderSelectedPath = folderSelectedPath + "/"
        }
        folderSelectedPath = folderSelectedPath + "/"
        val photoFile = File(
                folderSelectedPath,
           //     outputDirectory,
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

                    val savedUri = photoFile.name.toString()
                    val msg = "Photo saved"

                    Toast.makeText(
                        this@MainActivity,
                        "$msg $savedUri",
                        Toast.LENGTH_SHORT
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

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