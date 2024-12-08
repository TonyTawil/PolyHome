package com.antoinetawil.polyhome.Activities

import android.os.Bundle
import androidx.drawerlayout.widget.DrawerLayout
import com.antoinetawil.polyhome.R
import com.antoinetawil.polyhome.Utils.BaseActivity
import com.antoinetawil.polyhome.Utils.HeaderUtils

class StatisticsActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        // Set up the drawer and header
        drawerLayout = findViewById(R.id.drawer_layout)
        HeaderUtils.setupHeaderWithDrawer(this, drawerLayout)
    }
}
