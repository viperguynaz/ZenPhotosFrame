package dev.zenrg.zenphotoframe

import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageSwitcher
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.*
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import dev.zenrg.zenphotoframe.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException
import java.lang.System.currentTimeMillis
import java.util.*
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {
    private lateinit var signInClient: GoogleSignInClient
    private lateinit var imgSwitcher: ImageSwitcher
    private lateinit var bitmapDrawable: BitmapDrawable
    private lateinit var sharedPreferences: SharedPreferences

    private var random = Random()
    private val viewModel: MainViewModel by viewModels()
    private val tag = "zenx"
    private val scope = "oauth2:https://www.googleapis.com/auth/photoslibrary.readonly"
    private val job = Job()
    private val scopeIO = CoroutineScope(job + Dispatchers.IO)
    private var googleAccount: GoogleSignInAccount? = null
    private val exec: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(1)

    // used to bootstrap mediaItems while we get signed in
    private var mediaItems = mutableSetOf<String>(
        "AOUbmad5S_95sSPRn8RZldu02PDsD3z53z9XOEYHX8DY02XhDh2nAYjU_MWh9Jq4qdooFenMHqga_wLN8DhSHCXl9AvpofnqlQ",
        "AOUbmadZiypM2-kAwAZKT_gtMCl_ixFOOokvMb8oPqPsFg82swVg9qH2vf6kD04RcGYaaIkUTrv8sxa9tbmIXR4mHLxFgxRaDg",
        "AOUbmadzOHc5ab14broWyh63-MbodGLNSARhnCRMhLdd4F0ibwAG-7b7RlnD48LM8Pzc2Prp_SRVFwXyOVAHrxQ22varVw3N7g",
    )

    private var signInActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        }
    }

    private var token: String? by Delegates.observable(null) { _, _, newValue ->
        newValue?.let {
            viewModel.setToken(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getCachedItems()
        signInWithGoogle()
        setupViewModel()
        hideSystemBars()
        buildImageSwitcher()
    }

    private fun getCachedItems() {
        sharedPreferences = this.getPreferences(MODE_PRIVATE)
        val mediaItemsCached: Set<String>? = sharedPreferences.getStringSet("zen_media_items", null)
        if (mediaItemsCached != null) {
            mediaItems = mediaItemsCached.toMutableSet()
        }
    }

    private fun setupViewModel() {
        // setup viewModel observers
        viewModel.mediaItemIdsLive.observe(this) { items ->
            items?.let {
                mediaItems = it
                with(sharedPreferences.edit()) {
                    putStringSet("zen_media_items", mediaItems)
                    apply()
                }
                findViewById<TextView>(R.id.loadingText).visibility = View.GONE
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                imgSwitcher.visibility = View.VISIBLE
            }
        }
        viewModel.authToken.observe(this) {
            viewModel.getBitmap(mediaItems.elementAt(random.nextInt(mediaItems.size)))
            viewModel.getMediaItems()
        }

        /**
         * updates the imageswitcher image when a new bitmap is received
         * updates bitmapDrawable for the next swap
         * schedules the next bitmap retrieval
        */
        viewModel.bitmapLive.observe(this) {
            imgSwitcher.setImageDrawable(bitmapDrawable)
            bitmapDrawable = BitmapDrawable(resources, it)
            // schedule next bitmap retrieval
            exec.schedule({
                viewModel.getBitmap(mediaItems.elementAt(random.nextInt(mediaItems.size)))
            }, 30, TimeUnit.SECONDS)
        }
    }

    private fun buildImageSwitcher() {
        imgSwitcher = findViewById(R.id.imageSwitcher)
        bitmapDrawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources ,R.drawable.p1))
        imgSwitcher.setFactory {
            val imgView = ImageView(applicationContext)
            imgView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            imgView
        }
        imgSwitcher.inAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        imgSwitcher.outAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
        imgSwitcher.setImageDrawable(bitmapDrawable)
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view.
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                updateMargins(
                    left = insets.left,
                    bottom = insets.right,
                    right = insets.right,
                )
            }

            // Return CONSUMED so the window insets are not passed down to descendant views.
            WindowInsetsCompat.CONSUMED
        }
        val windowInsetsController: WindowInsetsControllerCompat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ViewCompat.getWindowInsetsController(window.decorView) ?: return   //findViewById(android.R.id.content)
        } else {
            ViewCompat.getWindowInsetsController(findViewById(android.R.id.content)) ?: return
        }

        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun signInWithGoogle() {
        // Build a GoogleSignInClient with the options specified by gso to refresh tokens
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.client_id_web))
            .requestScopes(Scope("https://www.googleapis.com/auth/photoslibrary.readonly"))
            .build()
        signInClient = GoogleSignIn.getClient(this, gso)
        googleAccount = GoogleSignIn.getLastSignedInAccount(this)
        setAccount(googleAccount)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun setAccount(signedInAccount: GoogleSignInAccount?) {
        try {
            googleAccount = signedInAccount
            when {
                googleAccount == null -> {
                    signInActivity.launch(signInClient.signInIntent)
                }
                googleAccount!!.expired() -> {
                    silentSignIn()
                }
                else -> {
                    // schedule a token refresh when the account expires
                    exec.schedule({
                        silentSignIn()
                    }, googleAccount!!.expiresInSeconds(), TimeUnit.SECONDS)
                    scopeIO.launch {
                        try {
                            token = GoogleAuthUtil.getToken(this@MainActivity, googleAccount!!.account!!, scope)
                        } catch (transientEx: IOException) {
                            // network or server error, the call is expected to succeed if you try again later.
                            // Don't attempt to call again immediately - the request is likely to
                            // fail, you'll hit quotas or back-off.
                        } catch (e: UserRecoverableAuthException) {
                            // Recover -- trying to access GoogleAuthUtil with expired account - launch sign-in
                            signInActivity.launch(e.intent)
                        } catch (authEx: GoogleAuthException) {
                            // Failure. The call is not expected to ever succeed so it should not be retried.
                            authEx.printStackTrace()
                        } catch (e: Exception) {
                            throw RuntimeException(e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, e.message.toString())
        }
    }

    private fun silentSignIn() {
        try {
            val task: Task<GoogleSignInAccount> = signInClient.silentSignIn()
            if (task.isSuccessful) {
                setAccount(task.result)
            } else {
                task.addOnCompleteListener {
                    try {
                        setAccount(it.result)
                    } catch (e: ApiException) {
                        Log.w(tag, "silentSignIn:addOnCompleteListener API Exception statusCode: ${e.statusCode} | message: ${e.message}")
                    }
                }
            }
        } catch(e: ApiException) {
            Log.w(tag, "silentSignIn:task.isSuccessful API Exception statusCode: ${e.statusCode} | message: ${e.message}")
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            googleAccount = completedTask.getResult(ApiException::class.java)
            setAccount(googleAccount)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(tag, "signInResult:failed code=" + e.statusCode)
        }
    }

    private fun GoogleSignInAccount.expired() : Boolean {
        return javaClass.getDeclaredField("zaj").let {
            it.isAccessible = true
            val zaj = it.getLong(this)
            return@let currentTimeMillis() / 1000L >= zaj
        }
    }

    private fun GoogleSignInAccount.expiresInSeconds() : Long {
        return javaClass.getDeclaredField("zaj").let {
            it.isAccessible = true
            val zaj = it.getLong(this)
            return@let zaj - (currentTimeMillis() / 1000L)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        exec.shutdownNow()
    }
}