package com.example.msproject.ui.moreinfo

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.example.msproject.R
import com.example.msproject.databinding.MoreInfoFragmentBinding
import com.example.msproject.ui.home.HomeViewModel


class MoreInfoFragment : Fragment(R.layout.more_info_fragment) {
    lateinit var binding: MoreInfoFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = MoreInfoFragmentBinding.inflate(inflater, container, false)


        var homeViewModel = ViewModelProviders.of(requireActivity())[HomeViewModel::class.java]
        homeViewModel.nearestParkingLotLiveData?.observe(viewLifecycleOwner, Observer {

            val chargesTextView = binding.chargesTextView
            val localizedChargeText =
                getString(R.string.pay_at_pay_station) + " " + it.parking_charges + "$ " + getString(
                    R.string.per_hr
                )
            chargesTextView.text = localizedChargeText

            val imageView = binding.parkingImage
            val imgUrl = it.image_url
            Glide.with(this)
                .load(imgUrl)
                .into(imageView)


            binding.parkingImage.setOnClickListener {
                val intent = Intent(requireActivity(), FullScreenImageActivity::class.java)
                intent.putExtra("image_url", imgUrl)
                intent.putExtra("image_caption", getString(R.string.image_caption))
                startActivity(intent)
            }

            binding.timestampTextView.text = it.timestamp
        })
        return binding.root
    }
}