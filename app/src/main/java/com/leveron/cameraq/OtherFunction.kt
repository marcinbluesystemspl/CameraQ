package com.leveron.cameraq

import android.R
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import kotlin.system.exitProcess

fun checkDate()
{
    var  today : LocalDate =  LocalDate.now()
    val validTill : LocalDate = LocalDate.parse("2023-06-05")

    if (today > validTill){
        //koniec programu
        exitProcess(-1)


    }
}

