package dev.zenrg.zenphotoframe

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dev.zenrg.zenphotoframe.models.MediaItem
import dev.zenrg.zenphotoframe.viewmodel.MainViewModel
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val tag = "zenx"

    private var mediaItems = mutableListOf<MediaItem>()
    private var token: String? by Delegates.observable(null) { property, oldValue, newValue ->
        Log.d(tag, "observable ${property.name}: $oldValue -> $newValue")
        newValue?.let { viewModel.getMediaItems() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel.setToken(this)
        viewModel.mediaItemsLive.observe(this) { items ->
            items?.let {
                mediaItems = it
            }
        }
        viewModel.authToken.observe(this) {
            token = it
        }
    }
}