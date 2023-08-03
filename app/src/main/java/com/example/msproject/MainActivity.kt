package com.example.msproject

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.msproject.databinding.ActivityMainBinding
import com.example.msproject.ui.home.HomeMapFragment
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val homeMapFragment = HomeMapFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container_view, homeMapFragment)
            .commit()
    }
}