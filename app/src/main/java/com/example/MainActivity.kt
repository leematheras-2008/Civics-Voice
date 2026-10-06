package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.AuthRepository
import com.example.data.repository.ComplaintRepository
import com.example.ui.CivicPulseApp
import com.example.ui.MainViewModel
import com.example.ui.theme.CivicPulseTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val authRepo = AuthRepository(applicationContext)
                val complaintRepo = ComplaintRepository(applicationContext)
                return MainViewModel(authRepo, complaintRepo) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CivicPulseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CivicPulseApp(viewModel = viewModel)
                }
            }
        }
    }
}
