package dev.zenrg.zenphotoframe.viewmodel

import androidx.lifecycle.*
import dev.zenrg.zenphotoframe.repository.GooglePhotosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val repo = GooglePhotosRepository()
    private val _authToken = MutableLiveData<String>()
    val authToken: LiveData<String> = _authToken
    val mediaItemsLive = repo.mediaItemsLive

    fun setToken(token: String) {
        _authToken.postValue(token)
    }

    fun getMediaItems() {
        if(_authToken.value.isNullOrEmpty()) throw Exception("MainViewModel property token not initialized. Call setToken first.")
        viewModelScope.launch(Dispatchers.IO) { repo.searchMediaItems("Bearer ${_authToken.value}") }
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

