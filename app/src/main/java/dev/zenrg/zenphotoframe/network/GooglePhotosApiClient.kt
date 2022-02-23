package dev.zenrg.zenphotoframe.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.zenrg.zenphotoframe.models.ErrorBody
import dev.zenrg.zenphotoframe.models.MediaItem
import dev.zenrg.zenphotoframe.models.MediaSearchRequest
import dev.zenrg.zenphotoframe.models.MediaSearchResponse
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

private const val BASE_URL = "https://photoslibrary.googleapis.com/v1/"

/**
 * Build the Moshi object that Retrofit will be using, making sure to add the Kotlin adapter for
 * full Kotlin compatibility.
 */
private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

/**
 * Use the Retrofit builder to build a retrofit object using a Moshi converter with our Moshi
 * object.
 */
private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()

/**
 * A public interface that exposes the [listMediaItems] method
 */
interface GooglePhotosApiClient {
    /**
     * Returns a [List] of [MediaItem]
     * The @POST annotation indicates the "mediaItems:search" endpoint will be requested with the POST HTTP method
     * Usage: searchMediaItems(token = "Bearer ${authToken}")
     */
    @Headers(
        "Accept: application/vnd.zenrg.v1.full+json",
        "User-Agent: dev.zenrg.imageswitcher"
    )
    @GET("mediaItems")
    suspend fun listMediaItems(@Header("Authorization") token: String,
                       @Query("pageToken") nextPageToken: String? = null,
                       @Query("pageSize") pageSize: Int? = null,
                       @Query("excludeNonAppCreatedData") excludeNonAppCreatedData: Boolean? = null,
    ): Response<MediaSearchResponse>

    @Headers(
        "Accept: application/vnd.zenrg.v1.full+json",
        "User-Agent: dev.zenrg.imageswitcher"
    )
    @POST("./mediaItems:search")
    suspend fun searchMediaItems(@Header("Authorization") token: String,
                                 @Body request: MediaSearchRequest
    ): Response<MediaSearchResponse>
}

/**
 * A public Api object that exposes the lazy-initialized Retrofit client for Google Photos
 * and a converter for the response.errorBody
 */
object GooglePhotosApi {
    val retrofitClient: GooglePhotosApiClient by lazy { retrofit.create(GooglePhotosApiClient::class.java) }
    val errorConverter: Converter<ResponseBody, ErrorBody> by lazy {
        retrofit.responseBodyConverter(ErrorBody::class.java, arrayOfNulls<Annotation>(0))
    }
}
