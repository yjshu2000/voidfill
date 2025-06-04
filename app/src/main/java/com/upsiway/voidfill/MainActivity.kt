package com.upsiway.voidfill

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.upsiway.voidfill.databinding.ActivityMainBinding
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    private lateinit var canvasView: TileCanvasView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set fullscreen mode
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // Create canvas view
        setContentView(R.layout.activity_main)
        canvasView = findViewById(R.id.canvasView)

        val canvasView = findViewById<TileCanvasView>(R.id.canvasView)
        val zoomInButton = findViewById<Button>(R.id.zoomInButton)
        val zoomOutButton = findViewById<Button>(R.id.zoomOutButton)
        val zoomDisplay = findViewById<TextView>(R.id.zoomDisplay)

        // Initial zoom text
        zoomDisplay.text = getString(R.string.zoom_display, canvasView.getZoomPercent())

        zoomInButton.setOnClickListener {
            canvasView.zoomIn()
            zoomDisplay.text = getString(R.string.zoom_display, canvasView.getZoomPercent())

        }

        zoomOutButton.setOnClickListener {
            canvasView.zoomOut()
            zoomDisplay.text = getString(R.string.zoom_display, canvasView.getZoomPercent())

        }

        val panStepX = resources.displayMetrics.widthPixels / 5f
        val panStepY = resources.displayMetrics.heightPixels / 5f

        findViewById<View>(R.id.topEdge).setOnClickListener {
            canvasView.panBy(0f, panStepY)
        }
        findViewById<View>(R.id.bottomEdge).setOnClickListener {
            canvasView.panBy(0f, -panStepY)
        }
        findViewById<View>(R.id.leftEdge).setOnClickListener {
            canvasView.panBy(panStepX, 0f)
        }
        findViewById<View>(R.id.rightEdge).setOnClickListener {
            canvasView.panBy(-panStepX, 0f)
        }


    }

    override fun onPause() {
        super.onPause()
        // Ensure any pending tile saves are completed
        canvasView.savePendingTiles()
    }
}

/*
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}*/
