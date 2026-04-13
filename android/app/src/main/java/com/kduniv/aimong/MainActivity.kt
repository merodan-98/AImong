package com.kduniv.aimong

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // [핵심] '재시작' 플래그가 없는 경우(앱을 새로 켰을 때)만 세션을 초기화합니다.
        val isRestart = intent.getBooleanExtra("IS_RESTART", false)
        
        if (savedInstanceState == null && !isRestart) {
            lifecycleScope.launch {
                sessionManager.clearSession()
                setupNavigation()
            }
        } else {
            setupNavigation()
        }
    }

    private fun setupNavigation() {
        lifecycleScope.launch {
            val userRole = sessionManager.userRole.first()

            if (userRole == null) {
                navController.setGraph(R.navigation.nav_main)
                binding.bottomNav.visibility = View.GONE
            } else {
                val navGraph = navController.navInflater.inflate(
                    if (userRole == "CHILD") R.navigation.nav_child else R.navigation.nav_parent
                )
                navController.graph = navGraph

                if (userRole == "CHILD") {
                    binding.bottomNav.visibility = View.VISIBLE
                    binding.bottomNav.setupWithNavController(navController)
                    binding.bottomNav.itemIconTintList = null
                    
                    binding.bottomNav.setOnItemSelectedListener { item ->
                        val navigated = NavigationUI.onNavDestinationSelected(item, navController)
                        for (i in 0 until binding.bottomNav.menu.size()) {
                            val view = binding.bottomNav.findViewById<View>(binding.bottomNav.menu.getItem(i).itemId)
                            view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                        }
                        val selectedView = binding.bottomNav.findViewById<View>(item.itemId)
                        selectedView.animate().scaleX(1.15f).scaleY(1.15f).setDuration(150).start()
                        navigated
                    }

                    binding.bottomNav.setOnItemReselectedListener { item ->
                        navController.popBackStack(item.itemId, false)
                    }
                } else {
                    binding.bottomNav.visibility = View.GONE
                }
            }
        }
    }
}
