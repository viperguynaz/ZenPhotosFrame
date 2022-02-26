package dev.zenrg.zenphotoframe.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zenrg.zenphotoframe.repository.GooglePhotosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {
    private val repo = GooglePhotosRepository()
    private val _authToken = MutableLiveData<String>()
    private val bearerToken: String
        get() = "Bearer ${_authToken.value}"

    val authToken: LiveData<String> = _authToken
    val mediaItemIdsLive = repo.mediaItemIdsLive
    val bitmapLive = repo.bitmapLive

    fun setToken(token: String) {
        _authToken.postValue(token)
    }

    private fun noTokenException() {
        if(_authToken.value.isNullOrEmpty()) throw Exception("MainViewModel property token not initialized. Call setToken first.")
    }

    fun getMediaItems() {
        noTokenException()
        viewModelScope.launch(Dispatchers.IO) { repo.searchMediaItems(bearerToken) }
    }

    fun getMediaItem(mediaItemId: String) {
        noTokenException()
        viewModelScope.launch(Dispatchers.IO) { repo.getMediaItem(bearerToken, mediaItemId) }
    }

    /**
     * url is the baseUrl from MediaItem: https://developers.google.com/photos/library/reference/rest/v1/mediaItems#MediaItem
     * options is a string added to baseUrl to specify image options: https://developers.google.com/photos/library/guides/access-media-items#base-urls
     */
    fun getBitmap(mediaItemId: String, options: String = "h1024") {
        noTokenException()
        viewModelScope.launch(Dispatchers.IO) {
            val mediaItem = withContext(Dispatchers.IO) { repo.getMediaItemDetail(bearerToken, mediaItemId) }
            if (mediaItem != null) {
                repo.getBitmap(bearerToken, "${mediaItem.baseUrl}=$options")
            }
        }
    }
}

/**
 * SAVE FOR NOW - example factory for creating viewModel with constructor parameter
 *  viewModel declaration: class MainViewModel(test: String) : ViewModel() {...}
 *  usage in activity/fragment: val viewModel: MainViewModel by viewModels { MainViewModelFactory("test") }
 */
//class MainViewModelFactory(private val test: String): ViewModelProvider.NewInstanceFactory() {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(test) as T
//}

