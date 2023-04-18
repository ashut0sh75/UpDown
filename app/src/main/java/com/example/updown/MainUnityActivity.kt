package com.example.updown
import android.content.Intent
import android.os.Bundle
import com.unity3d.player.UnityPlayerActivity

class MainUnityActivity : UnityPlayerActivity() {
    // Setup activity layout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this@MainUnityActivity, com.unity3d.player.UnityPlayerActivity::class.java)
        intent.putExtra("name", "value")
        startActivity(intent)
    }

}