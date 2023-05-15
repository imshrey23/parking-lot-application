package com.example.msproject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView


class FullScreenImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        // Set up the toolbar with back button
        val toolbar: Toolbar = findViewById(R.id.full_image)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Get the image view from the layout
        val photoView = findViewById<PhotoView>(R.id.photo_view)

        // Load the image into the photo view
        val imageUrl = intent.getStringExtra("image_url")
        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .into(photoView)
        }

    }

    // Handle the back button click
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

