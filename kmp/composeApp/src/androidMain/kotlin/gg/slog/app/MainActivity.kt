package gg.slog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContextHolder.activityOrApp = this
        enableEdgeToEdge()
        setContent { App() }
    }

    override fun onDestroy() {
        if (AppContextHolder.activityOrApp === this) AppContextHolder.activityOrApp = null
        super.onDestroy()
    }
}
