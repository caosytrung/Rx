package vn.hdn.rxsample.screen

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import vn.hdn.rxsample.R
import vn.hdn.rxsample.databinding.ActHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.act_home)
        Glide.with(this)
            .load(Uri.parse("file:///android_asset/img_logo.png"))
            .into(binding.imgIcon);
        binding.btnDodgeMultiClicks.setOnClickListener {
            startActivity(Intent(this, DodgeMultiClicksActivity::class.java))
        }
        binding.btnDebouceSearchView.setOnClickListener {
            startActivity(Intent(this, SearchCountryActivity::class.java))
        }
        binding.btnSequenceUpload.setOnClickListener {
            startActivity(Intent(this, SequenceUploadActivity::class.java))
        }
        binding.btnOptimizeBodyMail.setOnClickListener {
            startActivity(Intent(this, OptimizeMailLoadActivity::class.java))
        }
    }
}