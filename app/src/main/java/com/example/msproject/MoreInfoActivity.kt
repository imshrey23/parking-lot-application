package com.example.msproject

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.android.synthetic.main.activity_more_info.*
import java.lang.Exception
import com.bumptech.glide.Glide
import com.ortiz.touchview.TouchImageView
import com.squareup.picasso.Picasso

class MoreInfoActivity : AppCompatActivity() {



    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_more_info)

        //to add back button on screen 2
        setSupportActionBar(more_info)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        more_info.setNavigationOnClickListener{
            onBackPressed()
        }

        // Extract the extras from the intent
        val parkingCharges = intent.getStringExtra("parking_charges")
        val parkingImageUrl = intent.getStringExtra("parkingImageUrl") // get the image URL

        val chargesTextView = findViewById<TextView>(R.id.chargesTextView)
        val chargeRegex = Regex("""(\d+)\$""")
        val chargeMatch = parkingCharges?.let { chargeRegex.find(it) }
        val chargePerHour = chargeMatch?.groupValues?.getOrNull(1)?.toInt() ?: 0
        val localizedChargeText = getString(R.string.pay_at_pay_station) + " " + chargePerHour + "$ " + getString(R.string.per_hr)
        chargesTextView.text = localizedChargeText




        val imageView = findViewById<ImageView>(R.id.parkingImage)
//        Picasso.get().load(parkingImageUrl).into(imageView)
        Glide.with(this)
            .load(parkingImageUrl)
            .into(imageView)
        val parkingImage = findViewById<TouchImageView>(R.id.parkingImage)
        val image_caption =
        parkingImage.setOnClickListener {
            val intent = Intent(this@MoreInfoActivity, FullScreenImageActivity::class.java)
            intent.putExtra(
                "image_url",
                parkingImageUrl
            )
            intent.putExtra("image_caption", getString(R.string.image_caption))
            startActivity(intent)
        }

    }


}