package dev.zenrg.zenphotoframe

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
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
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {

    private lateinit var signInClient: GoogleSignInClient
    private lateinit var imgSwitcher: ImageSwitcher
    private lateinit var bitmapDrawable: BitmapDrawable
    private lateinit var timer: Timer
    private var random = Random()
    private val viewModel: MainViewModel by viewModels()
    private val tag = "zenx"
    private val scope = "oauth2:https://www.googleapis.com/auth/photoslibrary.readonly"
    private val job = Job()
    private val scopeIO = CoroutineScope(job + Dispatchers.IO)

    private var googleAccount: GoogleSignInAccount? = null

    private var signInActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        }
    }

    private var mediaItems = mutableListOf<String>()
    private var token: String? by Delegates.observable(null) { property, oldValue, newValue ->
        Log.d(tag, "observable ${property.name}: $oldValue -> $newValue")
        newValue?.let { viewModel.setToken(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        hideSystemBars()
        imgSwitcher = findViewById(R.id.imageSwitcher)
        bitmapDrawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources ,R.drawable.p1))

        // Build a GoogleSignInClient with the options specified by gso to refresh tokens
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.client_id_web))
            .requestScopes(Scope("https://www.googleapis.com/auth/photoslibrary.readonly"))
            .build()
        signInClient = GoogleSignIn.getClient(this, gso)
        googleAccount = GoogleSignIn.getAccountForScopes(this, Scope(scope))
        when {
            googleAccount == null -> {
                silentSignIn()
            }
            googleAccount!!.isExpired -> {
                signInActivity.launch(signInClient.signInIntent)
            }
            else -> {
                setAccount(googleAccount)
                Log.d(tag, "GoogleSignIn.getAccountForScopes email: ${googleAccount!!.email}")
            }
        }

        // setup viewModel observers
        viewModel.mediaItemIdsLive.observe(this) { items ->
            items?.let {
                mediaItems = it
                viewModel.getBitmap(mediaItems[random.nextInt(mediaItems.size)])
                findViewById<TextView>(R.id.loadingText).visibility = View.GONE
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                buildImageSwitcher()
                imgSwitcher.visibility = View.VISIBLE
                timer = fixedRateTimer(name="imageLooper", initialDelay = 30000, period=20000, action= {
                    runOnUiThread {
                        imgSwitcher.setImageDrawable(bitmapDrawable)
                        viewModel.getBitmap(mediaItems[random.nextInt(mediaItems.size)])
                    }
                })

            }
        }
        viewModel.authToken.observe(this) {
            Log.d(tag, "viewModel.authToken set -- token: $it}")
            viewModel.getMediaItems()
        }

        // updates the imageswitcher image when a new bitmap is received
        viewModel.bitmapLive.observe(this) {
            bitmapDrawable = BitmapDrawable(resources, it)
        }
    }

    private fun buildImageSwitcher() {
        imgSwitcher.setFactory {
            val imgView = ImageView(applicationContext)
            imgView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            imgView
        }

        imgSwitcher.setImageResource(R.drawable.p1)
        viewModel.getBitmap(mediaItems[random.nextInt(mediaItems.size)])
        imgSwitcher.inAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        imgSwitcher.outAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
    }

    private fun hideSystemBars() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view. Here the system is setting
            // only the bottom, left, and right dimensions, but apply whichever insets are
            // appropriate to your layout. You can also update the view padding
            // if that's more appropriate.
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                updateMargins(
                    left = insets.left,
                    bottom = insets.right,
                    right = insets.right,
                )
            }

            // Return CONSUMED if you don't want want the window insets to keep being
            // passed down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        val windowInsetsController =
            ViewCompat.getWindowInsetsController(findViewById(android.R.id.content)) ?: return   //findViewById(android.R.id.content)
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun silentSignIn() {
        val task: Task<GoogleSignInAccount> = signInClient.silentSignIn()
        if (task.isSuccessful) {
            setAccount(task.result)
        } else {
            task.addOnCompleteListener {
                setAccount(it.result)
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun setAccount(signedInAccount: GoogleSignInAccount?) {
        try {
            googleAccount = signedInAccount
            if (googleAccount == null || googleAccount!!.isExpired) {
                signInActivity.launch(signInClient.signInIntent)
            } else {
                scopeIO.launch {
                    try {
                        token = GoogleAuthUtil.getToken(this@MainActivity, googleAccount!!.account!!, scope)
                        Log.d(tag, "setAccount token: $token")
                    } catch (transientEx: IOException) {
                        // network or server error, the call is expected to succeed if you try again later.
                        // Don't attempt to call again immediately - the request is likely to
                        // fail, you'll hit quotas or back-off.

                    } catch (e: UserRecoverableAuthException) {
                        // Recover -- trying to access GoogleAuthUtil with expired account - launch sign-in
                        Log.d(tag, "${e.message!!} --- launching sign-in")
                        signInActivity.launch(e.intent)
                    } catch (authEx: GoogleAuthException) {
                        // Failure. The call is not expected to ever succeed so it should not be retried.
                        authEx.printStackTrace()
                    } catch (e: Exception) {
                        throw RuntimeException(e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, e.message.toString())
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

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        timer.cancel()
    }
}