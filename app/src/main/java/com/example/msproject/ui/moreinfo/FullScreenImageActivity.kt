package com.example.msproject.ui.moreinfo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.msproject.R
import com.github.chrisbanes.photoview.PhotoView


class FullScreenImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        val toolbar: Toolbar = findViewById(R.id.full_image)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val photoView = findViewById<PhotoView>(R.id.photo_view)

        val imageUrl = intent.getStringExtra("image_url")
        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .into(photoView)
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

