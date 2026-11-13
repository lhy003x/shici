package com.shici.app.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.shici.app.R
import com.shici.app.databinding.ActivityMainBinding
import com.shici.app.ui.home.HomeFragment
import com.shici.app.ui.settings.SettingsFragment
import com.shici.app.ui.stats.StatsFragment
import com.shici.app.ui.words.WordsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.nav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.tab_words -> WordsFragment()
                R.id.tab_stats -> StatsFragment()
                R.id.tab_settings -> SettingsFragment()
                else -> HomeFragment()
            }
            show(fragment)
            true
        }

        if (savedInstanceState == null) {
            show(HomeFragment())
        }
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.container, fragment)
            .commit()
    }

    fun selectTab(id: Int) {
        binding.nav.selectedItemId = id
    }

    fun hideNav(hidden: Boolean) {
        binding.nav.visibility = if (hidden) View.GONE else View.VISIBLE
    }
}
