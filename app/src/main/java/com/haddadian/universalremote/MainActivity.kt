package com.haddadian.universalremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.haddadian.universalremote.ui.UniversalRemoteApp

/** Android entry point. Runtime permissions are requested by the feature that needs them. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { UniversalRemoteApp() }
    }
}
