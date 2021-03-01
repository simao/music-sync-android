package eu.zio.musicsync

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import timber.log.Timber
import timber.log.Timber.DebugTree


class MainActivity : AppCompatActivity() {

    private lateinit var mAppBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Should go to Application
        Timber.plant(DebugTree())

        setContentView(R.layout.activity_main)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_artist_list, R.id.navigation_dashboard, R.id.navigation_notifications))
        setupActionBarWithNavController(navController, mAppBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.nav_host_fragment)
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
//        return navController.navigateUp()
    }
}