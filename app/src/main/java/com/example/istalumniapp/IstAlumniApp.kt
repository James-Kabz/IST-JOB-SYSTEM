package com.example.istalumniapp

import android.app.Application
import com.example.istalumniapp.data.AppContainer
import com.google.firebase.firestore.FirebaseFirestore
import com.example.istalumniapp.data.DefaultAppContainer
class IstAlumniApp : Application()
{
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        val firestore = FirebaseFirestore.getInstance()
        container = DefaultAppContainer(applicationContext)
    }
}