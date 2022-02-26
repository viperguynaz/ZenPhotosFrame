package dev.zenrg.zenphotoframe.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.MutableLiveData
import dev.zenrg.zenphotoframe.models.*
import dev.zenrg.zenphotoframe.network.GooglePhotosApi
import dev.zenrg.zenphotoframe.network.GooglePhotosApi.errorConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("BlockingMethodInNonBlockingContext")
class GooglePhotosRepository {
    private val client = GooglePhotosApi.retrofitClient
    val mediaItemIdsLive = MutableLiveData<MutableList<String>>()
    val mediaItemLive = MutableLiveData<MediaItem>()
    val bitmapLive = MutableLiveData<Bitmap>()
    private val mediaItemIds = mutableListOf<String>()
    private val albums:List<String> = listOf(
        "AOUbmae2mrHDBRKIyxpZDCFomIZIG3X8cEJSVCq8eFPqLqHUpkwqD-KcwIOY_-AhXVWcha5WBJdk",     // Groot
        "AOUbmae6_j2_W6dHgo9L6ZBv_Fk-Q3w8r4Bvr6pit_Y_CsQdrouVypJCaI2q_62zm4sJUQZGwExb",     // Roxie
        "AOUbmafP5V-h-LJVEyUgVUS0Oc1FKOBNh0v2_O4QY3HASFBkRvSeeqf8So0dH6yVzKXy_1W8pi2m",     // Sophia
    )

    suspend fun searchMediaItems(token: String) {
        for (album in albums) {
            val mediaSearchRequest = MediaSearchRequest(album)
            var mediaSearchResponse: MediaSearchResponse
            var moreToRead = true
            while (moreToRead) {
                val response = client.searchMediaItems(token, mediaSearchRequest)
                if (response.isSuccessful) {
                    if (response.body() != null) {
                        mediaSearchResponse = response.body()!!
                        mediaSearchResponse.let {
                            it.mediaItems?.let {
                                // filter out items that are not images
                                items -> mediaItemIds.addAll(items
                                    .filter{ item -> item.mediaMetadata?.photo != null}
                                    .map{ item -> item.id!! })
                            }
                            mediaSearchRequest.pageToken = mediaSearchResponse.nextPageToken
                            moreToRead = !mediaSearchResponse.nextPageToken.isNullOrEmpty()
                        }
                    } else {
                        moreToRead = false
                        mediaSearchRequest.pageToken = null

                        withContext(Dispatchers.IO) {
                            val errorBody = errorConverter.convert(response.errorBody()!!)
                            Log.e(
                                "GooglePhotosRepository.searchMediaItems",
                                "error: ${errorBody?.error?.message}"
                            )
                        }
                    }
                }
            }
        }
        mediaItemIdsLive.postValue(mediaItemIds)
    }

    suspend fun getMediaItem(token: String, mediaItemId: String) {
        val response = client.getMediaItem(token, mediaItemId)
        if (response.isSuccessful && response.body() != null) {
            mediaItemLive.postValue(response.body())
        }
    }

    suspend fun getMediaItemDetail(token: String, mediaItemId: String) : MediaItem? {
        val response = client.getMediaItem(token, mediaItemId)
        return if (response.isSuccessful && response.body() != null) {
            response.body()
        } else {
            null
        }
    }

    suspend fun getBitmap(token: String, url: String) {
        val response = client.getImage(token, url)
        if (response.isSuccessful && response.body() != null) {
            bitmapLive.postValue(BitmapFactory.decodeStream(response.body()!!.byteStream()))
        }
    }
}
