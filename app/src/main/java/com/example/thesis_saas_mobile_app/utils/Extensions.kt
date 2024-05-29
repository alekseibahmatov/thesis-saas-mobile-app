package com.example.thesis_saas_mobile_app.utils

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.thesis_saas_mobile_app.R

fun Fragment.openFragment(f: Fragment) {
    (activity as AppCompatActivity).supportFragmentManager.beginTransaction().replace(R.id.placeHolder, f).commit()
}

fun AppCompatActivity.openFragment(f: Fragment) {
    if (supportFragmentManager.fragments.isNotEmpty() && supportFragmentManager.fragments[0].javaClass === f.javaClass) return
    supportFragmentManager.beginTransaction().replace(R.id.placeHolder, f).commit()
}

fun AppCompatActivity.checkPermissions(p: String): Boolean {
    return when(PackageManager.PERMISSION_GRANTED) {
        ContextCompat.checkSelfPermission(this, p) -> true
        else -> false
    }
}