package com.leveron.cameraq

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import com.leveron.cameraq.Constants.APP_NAME
import com.leveron.cameraq.databinding.ActivityMainBinding
import java.io.File
import java.io.FileFilter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors



class MainActivity : AppCompatActivity() {

    /*
    Do zrobienia

    DONE - trzeba wyrzuci© spinnera na odzielny procedure
    TODO - przełączenie pomiędzy zoomami z 0,0 na  zoom 0,5   / dwustanowy guzik /
    TODO - zrobić wersje zapisu zdjec dla API30

    DONE - po dodaniu folderu nowego ustaw na niego focus - ZROBIONE
    DONE - trzeba zrobi© odswierzenie lisdty folderow na spinnerze - ZROBIONE
    DONE- jak sie chowa spinner to aktualny folder na gorze sie wyświetli - ZROBIONE
    DONE - trzeba jeszcze poukladac ikonki - ZROBIONE
    DONE - posortowanie na liście od a do z - ZROBIONE
    DONE -  trzeba ograc przypadek gdy nie ma folderow wtedy poleci crash przy zdjeciu  -> jak nie ma folderu to wgrywamy do głównego aplikacji -> naprawione na API >30
    DONE  - toast - zminimalizowac i skrócić.  max 2-3 wyrazy
    DONE  - toast dać niżej bo wchodzi na przycisk

    TODO - trzeba dodać wywołanie nie galeri a konkretnego zdjęcia z galeri aby mozna było skasowac dane
     */
    private lateinit var binding: ActivityMainBinding
    private var  imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    var folderSelected =""
    lateinit var folderSelectedPath: String
    lateinit var mAdView : AdView
    lateinit var lastPictureFile: File
    private var lastPictureUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    private var storage = "Ntex"
    lateinit var listOfDirs : File
    lateinit var spinner1 : Spinner

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(R.layout.activity_main)

        //sprawdzam datę i wersję programu
        checkDate()


        setContentView(binding.root)



    if (allPermissionGranted()) {
        startCamera()
    }else{
        ActivityCompat.requestPermissions(
            this, Constants.REQUIRED_PERMISSIONS,
            Constants.REQUEST_CODE_PERMISSIONS
        )
    }




     if (Constants.ADVIEW) {
         //aktywuje reklame
         MobileAds.initialize(this) {}

         //mAdView = findViewById(R.id.adView)
         mAdView = findViewById(R.id.adView)
         //mAdView = binding.adView
         val adRequest = AdRequest.Builder().build()
         mAdView.loadAd(adRequest)
         //ustawianie zmiennych
     }

        outputDirectory = getOutputDirectory()
        folderSelectedPath = ""
        cameraExecutor = Executors.newSingleThreadExecutor()


        spinner1 = binding.txtFolder
        val btnAdd = binding.btnAddFolder
        val btnLastPicture = binding.btnLastPicture
        val txtInputFolderName = binding.txtInputFolderName
        //var listOfDirs : File = getOutputDirectory()
        listOfDirs = getOutputDirectory()

        reloadSpinnerAdapter(text="")
/*
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

*/
        binding.btnStrzal.setOnClickListener {
            takePhoto()
        }

        binding.btnStrzal.setOnLongClickListener {
            if (spinner1.visibility == View.VISIBLE)
            {
                spinner1.visibility = View.INVISIBLE
                btnAdd.visibility = View.INVISIBLE
                btnLastPicture?.visibility = View.INVISIBLE
                txtInputFolderName.visibility = View.INVISIBLE


            }else{
                spinner1.visibility = View.VISIBLE
                btnAdd.visibility = View.VISIBLE
                btnLastPicture?.visibility = View.VISIBLE
                txtInputFolderName.visibility = View.VISIBLE
            }


            true
        }


        binding.btnLastPicture?.setOnClickListener{
          //show last picture
            if (lastPictureFile.toString() == ""){
                Snackbar.make(binding.root, R.string.take_first_photo , Snackbar.LENGTH_SHORT).show()
            }else {
                var uri1 : Uri
                uri1 = MediaStore.Images.Media.EXTERNAL_CONTENT_URI


                val intent = Intent(Intent.ACTION_VIEW )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                Log.d("eee",uri1.toString())
                intent.data = uri1

                startActivity(intent)

            }

            // TODO - trzeba dodać wywołanie nie galeri a konkretnego zdjęcia z galeri aby mozna było skasowac dane



        }
        binding.btnAddFolder.setOnClickListener {
            val newFolder : File


            if (txtInputFolderName.length() == 0){
                //Toast.makeText(this, R.string.folder_name_required, Toast.LENGTH_SHORT).show()
                Snackbar.make(binding.root, R.string.folder_name_required , Snackbar.LENGTH_SHORT).show()
            }else{
                listOfDirs = getOutputDirectory()
                newFolder = File(listOfDirs.toString() + File.separator + txtInputFolderName.text)
                Log.d("cccc", listOfDirs.toString())
                Log.d("cccc", listOfDirs.toString() + File.separator + txtInputFolderName.text)

                if (newFolder.exists()) {
                    //Toast.makeText(this, R.string.folder_already_exist, Toast.LENGTH_SHORT).show()
                    Snackbar.make(binding.root, R.string.folder_already_exist , Snackbar.LENGTH_SHORT).show()
                }else{
                    newFolder.mkdirs()
                    //Toast.makeText(this, "Folder " + txtInputFolderName.text + " created", Toast.LENGTH_SHORT).show()
                    Snackbar.make(binding.root, "Folder " + txtInputFolderName.text + " created", Snackbar.LENGTH_SHORT).show()
                }
                //ukryj klawiature
            it.hideKeyboard()
                //dodałeś folder to przeładuj listę folderów w adapterze spinner
            reloadSpinnerAdapter(txtInputFolderName.text.toString())
                //czysc folder
            txtInputFolderName.text = null

            }



        }




        reloadSpinnerAdapter(text="")
        spinner1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                //Toast.makeText(this@MainActivity, "You selected:" +  p0?.getItemAtPosition(p2).toString(), Toast.LENGTH_SHORT).show()
                Snackbar.make(binding.root,  "You selected:" + storage + "-" + p0?.getItemAtPosition(p2).toString(), Snackbar.LENGTH_SHORT).show()

                folderSelected = p0?.getItemAtPosition(p2).toString()
                //ustaw folder na gornej belce
                setTitle()

                //ustawe nowa sciezke do plikow
                listOfDirs = getOutputDirectory()
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
        var mFile = File(mediaDir + "/" + resources.getString(R.string.app_name) + "/" + storage)

        if (mFile.exists() == true) {
            return mFile
        }else{
            mFile.mkdirs()
            return mFile
        }
    }


    private fun setTitle(){
        title = "CQ: " + storage.take(4) + " - " + folderSelected.take(10)

    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        if (folderSelectedPath == "") {
            folderSelectedPath = getOutputDirectory().toString()
        }
        val fileName :String = SimpleDateFormat(
            Constants.FILE_NAME_FORMAT,
            Locale.getDefault()).format(System.currentTimeMillis()) + ".jpg"



        val photoFile = File(
                folderSelectedPath,
           //     outputDirectory,
                fileName)
        lastPictureFile = photoFile
        var contentValues = ContentValues()

        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME,fileName)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")

        if (Build.VERSION.SDK_INT>=29) {
            contentValues.put(
                MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM +
                        File.separator + APP_NAME + File.separator + storage + File.separator + folderSelected
            )
            lastPictureUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            lastPictureUri = Uri.withAppendedPath(lastPictureUri, APP_NAME + File.separator + storage + File.separator + folderSelected + File.separator + fileName)

        }
        val fileUri : Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI


        var outputOption = ImageCapture
            .OutputFileOptions
            .Builder(contentResolver,fileUri ,contentValues)
            .build()
        //jeżeli stary Android to inaczej definiuj outputOption inaczej nie bedzie odpowiedniego permission i zdjecie sie nie uda
        if (Build.VERSION.SDK_INT <=28) {
            outputOption = ImageCapture
            .OutputFileOptions
            .Builder(photoFile)
            .build()
        }

        imageCapture.takePicture(
            outputOption, ContextCompat.getMainExecutor(this),
            object :ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                    val savedUri = photoFile.name.toString()
                    val msg = R.string.photo_saved

                    val csoundCameraShot = MediaPlayer.create(this@MainActivity,R.raw.camera_shot)
                    csoundCameraShot.start()

                   /*
                    Toast.makeText(
                        this@MainActivity,
                        "$msg $savedUri",
                        Toast.LENGTH_SHORT).show()
                    */
                    Snackbar.make(binding.root, "$msg $savedUri" , Snackbar.LENGTH_SHORT).show()





                }

                override fun onError(exception: ImageCaptureException) {
                   Log.e(Constants.TAG, "onError: ${exception.message}", exception)

                    val csoundCameraShot = MediaPlayer.create(this@MainActivity,R.raw.camera_error)
                    csoundCameraShot.start()

                    /*
                    Toast.makeText(
                        this@MainActivity,
                        "Error:" + exception.message,
                        Toast.LENGTH_SHORT
                    ).show()   */
                    Snackbar.make(binding.root, "Error:" + exception.message, Snackbar.LENGTH_SHORT).show()

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


    fun View.hideKeyboard() {
        val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
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
            //Snackbar.make(binding.root,  "Permission not granted by User" , Snackbar.LENGTH_LONG).show()
            finish()

        }
    }
//podpinam menu oraz sprawdzam akcje menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_details,menu)
        //menu?.add("Ala ma kota")
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.toString() == "Ntex") { storage = "Ntex"}
        if(item.toString() == "Norwood") { storage = "Norwood"}
        if(item.toString() == "Drutex") { storage = "Drutex"}
        if(item.toString() == "Other") { storage = "Other"}
        setTitle()
        reloadSpinnerAdapter(text="")

        return super.onOptionsItemSelected(item)
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



    private fun reloadSpinnerAdapter(text : String) {
        var files: Array<File>
        var spinnerList = arrayListOf<String>("Foldery:")
        spinnerList.clear()
        listOfDirs = getOutputDirectory()

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





}