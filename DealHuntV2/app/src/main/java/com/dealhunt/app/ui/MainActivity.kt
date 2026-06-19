package com.dealhunt.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dealhunt.app.R
import com.dealhunt.app.databinding.ActivityMainBinding
import com.dealhunt.app.ui.fragments.*

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        if (s == null) show(HomeFragment())
        b.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home     -> show(HomeFragment())
                R.id.nav_explore  -> show(ExploreFragment())
                R.id.nav_wishlist -> show(WishlistFragment())
                R.id.nav_ai       -> show(AiFragment())
                R.id.nav_profile  -> show(ProfileFragment())
            }
            true
        }
    }

    private fun show(f: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, f).commit()
    }
}
