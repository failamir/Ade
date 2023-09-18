package infix.imrankst1221.codecanyon.ui.home

/**
 * Created by Md Imran Choudhury on 10/Aug/2018.
 * All rights received InfixSoft
 * Contact email: imrankst1221@gmail.com
 */

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.app.Service
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.MailTo
import android.net.Network
import android.net.Uri
import android.net.http.SslError
import android.os.*
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.*
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.material.internal.ViewUtils.dpToPx
import com.google.android.material.navigation.NavigationView
import infix.imrankst1221.codecanyon.R
import infix.imrankst1221.codecanyon.databinding.ActivityHomeBinding
import infix.imrankst1221.codecanyon.setting.ThemeConfig
import infix.imrankst1221.codecanyon.setting.WebviewGPSTrack
import infix.imrankst1221.rocket.library.setting.AppDataInstance
import infix.imrankst1221.rocket.library.setting.SpanningLinearLayoutManager
import infix.imrankst1221.rocket.library.setting.ThemeBaseActivity
import infix.imrankst1221.rocket.library.utility.Constants
import infix.imrankst1221.rocket.library.utility.PreferenceUtils
import infix.imrankst1221.rocket.library.utility.UtilMethods
import java.io.File
import java.io.IOException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.system.exitProcess


class HomeActivity : ThemeBaseActivity(),
    NavigationView.OnNavigationItemSelectedListener {
    private val TAG: String = "---HomeActivity"

    lateinit var mBinding: ActivityHomeBinding
    var mFileCamMessage: String? = null
    var mFilePath: ValueCallback<Array<Uri>>? = null
    private var isDoubleBackToExit = false
    private lateinit var mAboutUsPopup: PopupWindow

    private var mOnGoingDownload: Long? = null
    private var mDownloadManger: DownloadManager? = null
    private var isViewLoaded: Boolean = false
    private var isApplicationAlive = true
    private var isInitialWebViewLoad = false

    private var mFileManagerImagePath: String? = null
    private var mFileManagerVideoPath: String? = null
    private var mFileMessage: ValueCallback<Array<Uri>>? = null
    private val FCR = 1

    private var mJsRequestCount = 0
    private var mSmartAdsIncrement: Int = 0
    private var mAdDefaultDelay: Long = 0L
    private var mAdBannerDelay: Long = 0L
    private var mAdInterstitialDelay: Long = 0L
    private var mAdRewardedDelay: Long = 0L
    private var mRewardedVideoAd: RewardedAd? = null
    private var mInterstitialAd: InterstitialAd? = null

    private val CODE_AUDIO_CHOOSER = 10101
    private val CODE_QR_SCAN = 10102
    private val REQUEST_EXTERNAL_STORAGE = 10103

    private lateinit var bottomTabAdapter: BottomTabAdapter
    private var mWebViewPop: WebView? = null
    private var mWebViewPopBuilder: AlertDialog? = null
    private var mPermissionRequest: PermissionRequest? = null

    private var mLastUrl: String = ""
    private var mSuccessLoadedUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this

        initView()
        initDefaultURL()
        initSliderMenu()

        ThemeConfig(mContext, this).initThemeColor()
        ThemeConfig(mContext, this).initThemeStyle()

        if (PreferenceUtils.getInstance()
                .getBooleanValue(Constants.KEY_PERMISSION_DIALOG_ACTIVE, true)
        ) {
            Handler(Looper.myLooper()!!).postDelayed({
                requestAllPermissions()
            }, (resources.getInteger(R.integer.permission_timer) * 1000).toLong())
        }

        if (savedInstanceState == null) {
            if (UtilMethods.isConnectedToInternet(mContext)) {
                loadBaseWebView()
            } else {
                isInitialWebViewLoad = false
                showNoInternet()
            }
        }

        showLoader()

        initClickEvent()

        initAdMob()

        initExtraConfig()
    }


    private fun initExtraConfig() {
    }

    @SuppressLint("RestrictedApi", "RequiresFeature")
    private fun initView() {
        mBinding.txtNoInternetTitle.text = PreferenceUtils.getInstance()
            .getStringValue(Constants.KEY_NO_INTERNET_TITLE, "").toString()
            .ifEmpty { getString(R.string.label_noInternet) }
        mBinding.txtNoInternetDetail.text = PreferenceUtils.getInstance()
            .getStringValue(Constants.KEY_NO_INTERNET_DETAILS, "").toString()
            .ifEmpty { getString(R.string.massage_no_internet) }
        mBinding.btnTryAgain.text = PreferenceUtils.getInstance()
            .getStringValue(Constants.KEY_TRY_AGAIN_TEXT, "").toString()
            .ifEmpty { getString(R.string.label_tryAgain) }
        mBinding.txtErrorTitle.text = PreferenceUtils.getInstance()
            .getStringValue(Constants.KEY_ERROR_TITLE, "").toString()
            .ifEmpty { getString(R.string.error_title) }
        mBinding.txtErrorDetail.text = PreferenceUtils.getInstance()
            .getStringValue(Constants.KEY_ERROR_TEXT, "").toString()
            .ifEmpty { getString(R.string.error_occurred) }
        mBinding.btnErrorTryAgain.text = PreferenceUtils.getInstance()
            .getStringValue(Constants.KEY_TRY_AGAIN_TEXT, "").toString()
            .ifEmpty { getString(R.string.label_tryAgain) }

        val navigationHeader = mBinding.navigationView.getHeaderView(0)
        val navigationTitle =
            navigationHeader.findViewById<View>(R.id.txt_navigation_title) as TextView
        navigationTitle.text =
            PreferenceUtils.getInstance().getStringValue(Constants.KEY_MENU_HEADER_TITLE, "")
        val navigationDetails =
            navigationHeader.findViewById<View>(R.id.txt_navigation_detail) as TextView
        navigationDetails.text =
            PreferenceUtils.getInstance().getStringValue(Constants.KEY_MENU_HEADER_DETAILS, "")

        // pull to refresh enable
        mBinding.swapView.isEnabled = PreferenceUtils.getInstance()
            .getBooleanValue(Constants.KEY_PULL_TO_REFRESH_ENABLE, false)

        mDownloadManger = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        // webview swap conflict
        /**if(PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_PULL_TO_REFRESH_ENABLE, false)) {
        mBinding.webView.viewTreeObserver.addOnScrollChangedListener {
        this.mBinding.swapView.isEnabled = mBinding.webView.scrollY === 0
        }
        }*/

        // bottom tab
        if (AppDataInstance.appConfig.navigationTab.size > 0) {
            mBinding.layoutFooterBar.visibility = View.VISIBLE
            bottomTabAdapter = BottomTabAdapter(
                AppDataInstance.appConfig.navigationTab,
                object : OnBottomTabItemClick {
                    override fun itemClicked(position: Int) {
                        menuItemAction(AppDataInstance.appConfig.navigationTab[position].url)
                    }
                })

            val screenWidth = resources.displayMetrics.run { widthPixels / density }
            val tabWidth =
                resources.getDimension(R.dimen.tabsBarWidth) / resources.displayMetrics.density
            if (AppDataInstance.appConfig.navigationTab.size * tabWidth < screenWidth) {
                mBinding.rvBottomTab.layoutManager =
                    SpanningLinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false)
            } else {
                mBinding.rvBottomTab.layoutManager =
                    LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false)
            }
            mBinding.rvBottomTab.adapter = bottomTabAdapter
        } else {
            runOnUiThread {
                val layoutParam: RelativeLayout.LayoutParams =
                    mBinding.layoutFooterAds.layoutParams as RelativeLayout.LayoutParams
                layoutParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                mBinding.layoutFooterAds.layoutParams = layoutParam
            }
        }

        // on full screen keyboard showing status bar issue
        mBinding.rootContainer.viewTreeObserver.addOnGlobalLayoutListener {
            if (mBinding.rootContainer != null) {
                val heightDiff: Int =
                    mBinding.rootContainer.rootView.height - mBinding.rootContainer.height
                if (heightDiff > dpToPx(mContext, 100)) {
                    setActiveFullScreen()
                } else {
                    setActiveFullScreen()
                }
            }
        }

        // On internet back automatically reload
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val connectivityManager =
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.registerDefaultNetworkCallback(object :
                ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (mBinding.layoutNoInternet.visibility == View.VISIBLE &&
                        UtilMethods.isConnectedToInternet(mContext)
                    ) {
                        try {
                            runOnUiThread {
                                mBinding.btnTryAgain.performClick()
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }
            })
        }
    }

    private fun initAdMob() {
        val adMobId =
            PreferenceUtils.getInstance().getStringValue(Constants.ADMOB_ID, "").toString()
                .ifEmpty { getString(R.string.admob_app_id) }
        if (adMobId.isNotEmpty()) {
            val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val myApiKey = ai.metaData.getString("com.google.android.gms.ads.APPLICATION_ID")
            ai.metaData.putString(
                "com.google.android.gms.ads.APPLICATION_ID",
                adMobId
            )

            //MobileAds.initialize(this, adMobId)
            MobileAds.initialize(this) { initializationStatus ->
                val statusMap =
                    initializationStatus.adapterStatusMap
                for (adapterClass in statusMap.keys) {
                    val status = statusMap[adapterClass]
                    Log.d(
                        "MyApp", String.format(
                            "Adapter name: %s, Description: %s, Latency: %d",
                            adapterClass, status!!.description, status.latency
                        )
                    )
                }
            }

            val testDeviceIds = listOf("")
            val configuration =
                RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
            MobileAds.setRequestConfiguration(configuration)

            mAdDefaultDelay = (PreferenceUtils.getInstance()
                .getIntegerValue(Constants.ADMOB_KEY_AD_DELAY, 60)).toLong() * 1000
            mAdBannerDelay = (PreferenceUtils.getInstance()
                .getIntegerValue(
                    Constants.ADMOB_KEY_BANNER_AD_DELAY,
                    mAdDefaultDelay.toInt()
                )).toLong() * 1000
            mAdInterstitialDelay = (PreferenceUtils.getInstance()
                .getIntegerValue(
                    Constants.ADMOB_KEY_INTERSTITIAL_AD_DELAY,
                    mAdDefaultDelay.toInt()
                )).toLong() * 1000
            mAdRewardedDelay = (PreferenceUtils.getInstance()
                .getIntegerValue(
                    Constants.ADMOB_KEY_REWARDED_AD_DELAY,
                    mAdDefaultDelay.toInt()
                )).toLong() * 1000

            val adMobBannerID =
                PreferenceUtils.getInstance().getStringValue(Constants.ADMOB_KEY_AD_BANNER, "")
            if (adMobBannerID!!.isEmpty()) {
                mBinding.layoutFooterAds.visibility = View.GONE
                Log.d(TAG, "Banner adMob ID is empty!")
            } else {
                initBannerAdMob(adMobBannerID)
            }

            val interstitialAdID = PreferenceUtils.getInstance()
                .getStringValue(Constants.ADMOB_KEY_AD_INTERSTITIAL, "")
            if (interstitialAdID!!.isEmpty()) {
                Log.d(TAG, "Interstitial adMob ID is empty!")
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    initInterstitialAdMob(interstitialAdID)
                }, mAdInterstitialDelay)
            }

            val rewardedAdID =
                PreferenceUtils.getInstance().getStringValue(Constants.ADMOB_KEY_AD_REWARDED, "")
            if (rewardedAdID!!.isEmpty()) {
                Log.d(TAG, "Rewarded adMob ID is empty!")
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    initRewardedAdMob(rewardedAdID)
                }, mAdRewardedDelay)
            }
        }
    }

    /**
     * Set adMob with Banner ID which get from adMob account
     * When ID is "" (empty) then ad will hide
     * */
    private fun initBannerAdMob(adMobBannerID: String) {
        val adMobBanner = AdView(this)
        adMobBanner.setAdSize(AdSize.BANNER)
        adMobBanner.adUnitId = adMobBannerID
        val adRequest: AdRequest = AdRequest.Builder().build()

        adMobBanner.adListener = object : AdListener() {
            override fun onAdLoaded() {
                runOnUiThread {
                    mBinding.viewBannerAds.visibility = View.VISIBLE
                    if (mBinding.layoutFooterAds.visibility == View.GONE) {
                        mBinding.layoutFooterAds.visibility = View.VISIBLE
                        val slideUp: Animation =
                            AnimationUtils.loadAnimation(mContext, R.anim.anim_slide_up)
                        mBinding.layoutFooterAds.startAnimation(slideUp)
                    }
                }
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                UtilMethods.printLog(TAG, "Banner ad error code: ${adError.code}")
                UtilMethods.printLog(TAG, "Banner ad error message: ${adError.message}")
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isApplicationAlive)
                        initBannerAdMob(adMobBannerID)
                }, mAdBannerDelay)
            }

            override fun onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }

            override fun onAdClicked() {
                // Code to be executed when the user clicks on an ad.
            }

            override fun onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }
        }

        if (isApplicationAlive) {
            adMobBanner.loadAd(adRequest)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            mBinding.viewBannerAds.addView(adMobBanner, params)
        }
    }

    private fun initInterstitialAdMob(interstitialAdID: String) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(mContext,
            interstitialAdID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Admob Interstitial error code: ${adError.code}")
                    Log.e(TAG, "Admob Interstitial error message: ${adError.message}")

                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isApplicationAlive) {
                            initInterstitialAdMob(interstitialAdID)
                        }
                    }, mAdInterstitialDelay + mSmartAdsIncrement)
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    mInterstitialAd!!.show(this@HomeActivity)
                }

            })
        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                mSmartAdsIncrement += 40000
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isApplicationAlive) {
                        initInterstitialAdMob(interstitialAdID)
                    }
                }, mAdInterstitialDelay + mSmartAdsIncrement)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.d(TAG, "RewardedAd failed error code: " + adError.code)
                Log.d(TAG, "RewardedAd failed error message: " + adError.message)
                super.onAdFailedToShowFullScreenContent(adError)
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial Ad was shown.")
            }
        }
    }

    private fun initRewardedAdMob(rewardedAdID: String) {
        mRewardedVideoAd = null
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(mContext,
            rewardedAdID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "RewardedAd failed error code: " + adError.code)
                    Log.d(TAG, "RewardedAd failed error message: " + adError.message)
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isApplicationAlive) {
                            initRewardedAdMob(rewardedAdID)
                        }
                    }, mAdRewardedDelay + mSmartAdsIncrement)
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedVideoAd = rewardedAd
                    mRewardedVideoAd!!.show(this@HomeActivity) {
                        fun onUserEarnedReward(rewardItem: RewardItem) {
                            val rewardAmount = rewardItem.amount
                            val rewardType = rewardItem.type
                            Log.d(TAG, "Reward type: $rewardType")
                            Log.d(TAG, "Reward amount: $rewardAmount")
                        }
                    }
                }
            })

        mRewardedVideoAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded Ad was shown.")
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.d(TAG, "RewardedAd failed error code: " + adError.code)
                Log.d(TAG, "RewardedAd failed error message: " + adError.message)
                super.onAdFailedToShowFullScreenContent(adError)
            }

            override fun onAdDismissedFullScreenContent() {
                mSmartAdsIncrement += 60000

                Handler(Looper.getMainLooper()).postDelayed({
                    if (isApplicationAlive) {
                        initRewardedAdMob(rewardedAdID)
                    }
                }, mAdRewardedDelay + mSmartAdsIncrement)
            }
        }
    }

    private fun showWebView() {
        mBinding.webView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.transparent))
        mBinding.webView.visibility = View.VISIBLE
    }

    private fun hideWebView() {
        mBinding.webView.visibility = View.GONE
    }

    private fun showErrorView() {
        hideLoader()
        mBinding.layoutError.visibility = View.VISIBLE
    }

    private fun hideErrorView() {
        mBinding.layoutError.visibility = View.GONE
    }

    private fun showNoInternet() {
        hideLoader()
        mBinding.layoutNoInternet.visibility = View.VISIBLE
    }

    private fun hideNoInternet() {
        mBinding.layoutNoInternet.visibility = View.GONE
    }

    private fun showLoader() {
        if (PreferenceUtils.getInstance().getStringValue(
                Constants.KEY_LOADER,
                Constants.LOADER_HIDE
            ) != Constants.LOADER_HIDE
        ) {
            mBinding.layoutProgress.visibility = View.VISIBLE
        }

        Handler(Looper.getMainLooper()).postDelayed(
            {
                if (mBinding.layoutProgress.visibility == View.VISIBLE) {
                    hideLoader()
                }
            },
            PreferenceUtils.getInstance().getIntegerValue(Constants.KEY_LOADER_DELAY, 10)
                .toLong() * 1000
        )
    }

    private fun hideLoader() {
        mBinding.layoutProgress.visibility = View.GONE
    }

    private fun webViewGoBack() {
        if (mBinding.webView.canGoBack()) {
            mBinding.webView.goBack()
        }
    }

    private fun webViewGoForward() {
        if (mBinding.webView.canGoForward()) {
            mBinding.webView.goForward()
        }
    }

    @SuppressLint("WrongConstant")
    private fun initClickEvent() {
        // try again
        mBinding.btnTryAgain.setOnClickListener {
            if (UtilMethods.isConnectedToInternet(mContext)) {
                if (isInitialWebViewLoad) {
                    mBinding.webView.reload()
                } else {
                    loadBaseWebView()
                }
            } else {
                UtilMethods.showSnackbar(
                    mBinding.rootContainer,
                    getString(R.string.massage_no_internet)
                )
            }
        }

        mBinding.swapView.setOnRefreshListener {
            //webviewReload()
            mBinding.webView.reload()
            showLoader()
            Handler(Looper.getMainLooper()).postDelayed({
                mBinding.swapView.isRefreshing = false
            }, 2000)
        }

        // menu click toggle left
        mBinding.imgLeftMenu.setOnClickListener {
            // slier menu open from left
            val params = DrawerLayout.LayoutParams(
                DrawerLayout.LayoutParams.WRAP_CONTENT,
                DrawerLayout.LayoutParams.MATCH_PARENT
            )
            val gravityCompat: Int

            if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_RTL_ACTIVE, false)) {
                params.gravity = Gravity.END
                gravityCompat = GravityCompat.END
                mBinding.navigationView.layoutParams = params
            } else {
                params.gravity = Gravity.START
                gravityCompat = GravityCompat.START
                mBinding.navigationView.layoutParams = params
            }

            val navigationLeftMenu = PreferenceUtils.getInstance()
                .getStringValue(Constants.KEY_LEFT_MENU_STYLE, Constants.LEFT_MENU_SLIDER)
            if (navigationLeftMenu == Constants.LEFT_MENU_SLIDER) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (mBinding.drawerLayout.isDrawerOpen(gravityCompat)) {
                        mBinding.drawerLayout.closeDrawer(gravityCompat)
                    } else {
                        mBinding.drawerLayout.openDrawer(gravityCompat)
                    }
                }, 100)
            } else if (navigationLeftMenu == Constants.LEFT_MENU_RELOAD) {
                // request for reload again website
                webviewReload()
            } else if (navigationLeftMenu == Constants.LEFT_MENU_SHARE) {
                UtilMethods.shareTheApp(
                    mContext,
                    "Download " + getString(R.string.app_name) + "" +
                            " app from play store. Click here: " + "" +
                            "https://play.google.com/store/apps/details?id=" + packageName
                )
            } else if (navigationLeftMenu == Constants.LEFT_MENU_HOME) {
                isViewLoaded = false
                loadBaseWebView()
            } else if (navigationLeftMenu == Constants.LEFT_MENU_EXIT) {
                exitHomeScreen()
            } else if (navigationLeftMenu == Constants.LEFT_MENU_NAVIGATION) {
                webViewGoBack()
            }
        }

        mBinding.imgLeftMenu1.setOnClickListener {
            val navigationLeftMenu = PreferenceUtils.getInstance()
                .getStringValue(Constants.KEY_LEFT_MENU_STYLE, Constants.LEFT_MENU_SLIDER)
            if (navigationLeftMenu == Constants.LEFT_MENU_NAVIGATION) {
                webViewGoForward()
            }
        }

        // menu click toggle right
        mBinding.imgRightMenu.setOnClickListener {
            // slier menu open from left
            val params = DrawerLayout.LayoutParams(
                DrawerLayout.LayoutParams.WRAP_CONTENT,
                DrawerLayout.LayoutParams.MATCH_PARENT
            )
            val gravityCompat: Int

            /*if (PreferenceUtils.getInstance().getBooleanValue(Constants.KEY_RTL_ACTIVE, false)) {
                params.gravity = Gravity.START
                gravityCompat = GravityCompat.START
                mBinding.navigationView.layoutParams = params
            } else {
                params.gravity = Gravity.END
                gravityCompat = GravityCompat.END
                mBinding.navigationView.layoutParams = params
            }*/

            params.gravity = Gravity.END
            gravityCompat = GravityCompat.END
            mBinding.navigationView.layoutParams = params

            val navigationRightMenu = PreferenceUtils.getInstance()
                .getStringValue(Constants.KEY_RIGHT_MENU_STYLE, Constants.RIGHT_MENU_SLIDER)
            if (navigationRightMenu == Constants.RIGHT_MENU_SLIDER) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (mBinding.drawerLayout.isDrawerOpen(gravityCompat)) {
                        mBinding.drawerLayout.closeDrawer(gravityCompat)
                    } else {
                        mBinding.drawerLayout.openDrawer(gravityCompat)
                    }
                }, 100)
            } else if (navigationRightMenu == Constants.RIGHT_MENU_RELOAD) {
                // request for reload again website
                webviewReload()
            } else if (navigationRightMenu == Constants.RIGHT_MENU_SHARE) {
                UtilMethods.shareTheApp(
                    mContext,
                    "Download " + getString(R.string.app_name) + "" +
                            " app from play store. Click here: " + "" +
                            "https://play.google.com/store/apps/details?id=" + packageName
                )
            } else if (navigationRightMenu == Constants.RIGHT_MENU_HOME) {
                isViewLoaded = false
                loadBaseWebView()
            } else if (navigationRightMenu == Constants.RIGHT_MENU_EXIT) {
                exitHomeScreen()
            } else if (navigationRightMenu == Constants.RIGHT_MENU_NAVIGATION) {
                webViewGoForward()
            }
        }

        mBinding.imgRightMenu1.setOnClickListener {
            val navigationRightMenu = PreferenceUtils.getInstance()
                .getStringValue(Constants.KEY_RIGHT_MENU_STYLE, Constants.RIGHT_MENU_SLIDER)
            if (navigationRightMenu == Constants.RIGHT_MENU_NAVIGATION) {
                webViewGoBack()
            }
        }

        // on error reload again
        mBinding.btnErrorTryAgain.setOnClickListener {
            // request for reload again website
            //successLoadedUrl = ""
            isViewLoaded = false
            //mBinding.webView.clearCache(true)
            //mBinding.webView.clearHistory()
            if (mSuccessLoadedUrl != "") {
                loadWebView(mSuccessLoadedUrl)
            } else if (mLastUrl != "") {
                loadWebView(mLastUrl)
            } else {
                loadBaseWebView()
            }
        }

        // on error go home
        mBinding.btnErrorHome.setOnClickListener {
            // request for reload again website
            mSuccessLoadedUrl = ""
            isViewLoaded = false
            mBinding.webView.clearCache(true)
            mBinding.webView.clearHistory()

            loadBaseWebView()
        }


        // show or hide adMob
        /*mBinding.btnAdShow.setOnClickListener {
            if(mBinding.viewBannerAds.visibility == View.GONE){
                mBinding.viewBannerAds.visibility = View.VISIBLE
                mBinding.imgAdShow.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_down_arrow))
            }else{
                mBinding.viewBannerAds.visibility = View.GONE
                mBinding.imgAdShow.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_up_arrow))
            }
        }*/
    }

    private fun loadBaseWebView() {
        initDefaultURL()

        if (AppDataInstance.deepLinkUrl.isNotEmpty()) {
            loadWebView(AppDataInstance.deepLinkUrl)
            AppDataInstance.deepLinkUrl = ""
        } else if (AppDataInstance.notificationUrl.isNotEmpty()) {
            when (Constants.WEBVIEW_OPEN_TYPE.valueOf(AppDataInstance.notificationUrlOpenType.toUpperCase())) {
                Constants.WEBVIEW_OPEN_TYPE.EXTERNAL -> {
                    loadWebView(mDefaultURL)
                    Handler(Looper.getMainLooper()).postDelayed({
                        UtilMethods.browseUrlExternal(mContext, AppDataInstance.notificationUrl)
                        AppDataInstance.notificationUrl = ""
                    }, 5000)
                }
                Constants.WEBVIEW_OPEN_TYPE.CUSTOM_TAB -> {
                    loadWebView(mDefaultURL)
                    Handler(Looper.getMainLooper()).postDelayed({
                        UtilMethods.browseUrlCustomTab(mContext, AppDataInstance.notificationUrl)
                        AppDataInstance.notificationUrl = ""
                    }, 5000)
                }
                else -> {
                    loadWebView(AppDataInstance.notificationUrl)
                    AppDataInstance.notificationUrl = ""
                }
            }
        } else {
            if (resources.getBoolean(R.bool.enable_notification_uuid_attach)) {
                val firebaseUserToken =
                    PreferenceUtils.getStringValue(Constants.KEY_FIREBASE_TOKEN, "")
                val oneSignalUserToken =
                    PreferenceUtils.getStringValue(Constants.KEY_ONE_SIGNAL_USER_ID, "")
                if (!firebaseUserToken.isNullOrEmpty()) {
                    mDefaultURL += "?firebase_token=${
                        PreferenceUtils.getStringValue(
                            Constants.KEY_FIREBASE_TOKEN,
                            ""
                        )
                    }"
                    if (!oneSignalUserToken.isNullOrEmpty()) {
                        mDefaultURL += "&onesignal_user_id=${
                            PreferenceUtils.getStringValue(
                                Constants.KEY_ONE_SIGNAL_USER_ID,
                                ""
                            )
                        }"
                    }
                } else {
                    if (!oneSignalUserToken.isNullOrEmpty()) {
                        mDefaultURL += "?onesignal_user_id=${
                            PreferenceUtils.getStringValue(
                                Constants.KEY_ONE_SIGNAL_USER_ID,
                                ""
                            )
                        }"
                    }
                }
            }

            if (resources.getBoolean(R.bool.enable_device_uuid_attach)) {
                val firebaseUserToken =
                    PreferenceUtils.getStringValue(Constants.KEY_FIREBASE_TOKEN, "")
                val oneSignalUserToken =
                    PreferenceUtils.getStringValue(Constants.KEY_ONE_SIGNAL_USER_ID, "")
                if (resources.getBoolean(R.bool.enable_notification_uuid_attach) &&
                    (!firebaseUserToken.isNullOrEmpty() || !oneSignalUserToken.isNullOrEmpty())
                ) {
                    mDefaultURL += "&device_uuid=${
                        UUID.randomUUID()
                    }"
                } else {
                    mDefaultURL += "?device_uuid=${
                        UUID.randomUUID()
                    }"
                }
            }

            UtilMethods.printLog(TAG, "Base URL $mDefaultURL")
            loadWebView(mDefaultURL)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadWebView(loadUrl: String) {
        showWebView()
        hideNoInternet()
        hideLoader()
        hideErrorView()
        isInitialWebViewLoad = true

        mBinding.webView.webChromeClient = CustomChromeClient()
        initConfigureWebView(
            mBinding.webView,
            getString(R.string.user_agent_string),
            resources.getBoolean(R.bool.enable_google_login),
            resources.getBoolean(R.bool.enable_website_cookies)
        )
        initWebClient()
        initExtraConfig()

        // set download listener
        mBinding.webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            if (Build.VERSION.SDK_INT in 23..29) {
                if (askForPermission(PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE, true)) {
                    //UtilMethods.browseUrlCustomTab(mContext, url)
                    startDownload(url, contentDisposition, userAgent, mimetype)
                }
            } else {
                //UtilMethods.browseUrlCustomTab(mContext, url)
                startDownload(url, contentDisposition, userAgent, mimetype)
            }
        }

        mBinding.webView.loadUrl(loadUrl)
    }

    private fun webviewReload() {
        isViewLoaded = false
        mBinding.webView.loadUrl("about:blank")
        //mBinding.webView.clearCache(true)
        //mBinding.webView.clearHistory()
        mBinding.webView.reload()
    }

    //not used, optional
    fun clearCache() {
        mBinding.webView.clearCache(true)
        this.deleteDatabase("webview.db")
        this.deleteDatabase("webviewCache.db")
        mBinding.webView.clearCache(false)
    }

    private fun initWebClient() {
        mBinding.webView.webViewClient = object : WebViewClient() {
            // shouldOverrideUrlLoading only call on real device
            override fun shouldOverrideUrlLoading(webView: WebView?, url: String?): Boolean {
                return shouldOverrideUrlHandler(webView!!, url!!, false)
            }

            override fun shouldOverrideUrlLoading(
                webView: WebView,
                request: WebResourceRequest
            ): Boolean {
                return shouldOverrideUrlHandler(webView, request.url.toString(), false)
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (UtilMethods.isConnectedToInternet(mContext) || url.startsWith("file:///android_asset")) {
                    isViewLoaded = false
                    showWebView()
                    hideErrorView()
                    showLoader()

                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            hideNoInternet()
                            if (mBinding.layoutProgress.visibility == View.VISIBLE) {
                                hideLoader()
                            }
                        },
                        PreferenceUtils.getInstance()
                            .getIntegerValue(Constants.KEY_LOADER_DELAY, 10).toLong() * 1000
                    )
                } else {
                    hideWebView()
                    showNoInternet()
                    hideLoader()
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                manageCookie(
                    url,
                    mBinding.webView,
                    resources.getBoolean(R.bool.enable_push_custom_cookies)
                )

                if (mBinding.layoutError.visibility != View.VISIBLE) {
                    showWebView()
                }

                isViewLoaded = true
                hideLoader()
                mSuccessLoadedUrl = url

                if (url.startsWith("https://m.facebook.com/v2.7/dialog/oauth")) {
                    if (mWebViewPop != null) {
                        mWebViewPop!!.visibility = View.GONE
                        mWebViewPop = null
                    }
                    mBinding.webView.loadUrl(mLastUrl)
                    return
                }

                // disable Google Adsense
                val javaScript =
                    "javascript:(function() { document.getElementById('google_image_div').remove();})()"
                mBinding.webView.loadUrl(javaScript)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    UtilMethods.printLog(TAG, "${error!!.description}")

                    if (error.description.contains("net::ERR_UNKNOWN_URL_SCHEME")
                        || error.description.contains("net::ERR_FILE_NOT_FOUND")
                        || error.description.contains("net::ERR_CONNECTION_REFUSED")
                    ) {
                        hideWebView()
                        hideLoader()
                        return
                    } else if (error.description.contains("net::ERR_TIMED_OUT")
                        || error.description.contains("net::ERR_CONNECTION_RESET")
                        || error.description.contains("net::ERR_NAME_NOT_RESOLVED")
                    // || error.description.contains("net::ERR_ADDRESS_UNREACHABLE")
                    // || error.description.contains("net::ERR_TOO_MANY_REDIRECTS")
                    // || error.description.contains("net::ERR_SPDY_PROTOCOL_ERROR")
                    ) {
                        showErrorView()
                        return
                    } else if (error.description.contains("net::ERR_CONNECTION_TIMED_OUT")) {
                        // show offline page

                        return
                    } else if (error.description.contains("net::ERR_CLEARTEXT_NOT_PERMITTED")) {
                        return
                    } else if (error.description.contains("net::ERR_INTERNET_DISCONNECTED")) {
                        //hideWebView()
                        showNoInternet()
                    }
                } else {
                    UtilMethods.printLog(TAG, error.toString())
                }

                // you can load offline page here
                //mBinding.webView.loadUrl("file:///android_asset")
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                UtilMethods.printLog(TAG, "HTTP Error: ${errorResponse.toString()}")
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                super.onReceivedSslError(view, handler, error)
                UtilMethods.printLog(TAG, "SSL Error: ${error.toString()}")
            }

        }
    }

    private fun shouldOverrideUrlHandler(webView: WebView, url: String, isPopup: Boolean): Boolean {
        UtilMethods.printLog(TAG, "OverrideUrl (1/1): $url")

        if(url.equals("about:blank")){
            return true
        }

        // already success loaded
        if (mSuccessLoadedUrl == url || url.startsWith("file:///android_asset")) {
            UtilMethods.printLog(TAG, "Page already loaded!")
            // Some JS call like login is not working for few website
            return true
        }

        if (url.contains("google.navigation:")) {
            val gmmIntentUri = Uri.parse(url)
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
            return true
        }

        if (url.startsWith("tel:")) {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse(url)
            try {
                startActivity(intent)
            } catch (ex: ActivityNotFoundException) {
                UtilMethods.browseUrlExternal(mContext, url)
                UtilMethods.showShortToast(mContext, "Activity Not Found")
            }
            return true

        } else if (url.startsWith("sms:")) {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse(url)
            try {
                startActivity(intent)
            } catch (ex: ActivityNotFoundException) {
                UtilMethods.browseUrlExternal(mContext, url)
                UtilMethods.showShortToast(mContext, "Activity Not Found")
            }
            return true

        } else if (url.startsWith("mailto:")) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "message/rfc822"

            val mailTo: MailTo = MailTo.parse(url)
            //val addressMail = url.replace("mailto:", "")
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(mailTo.to))
            intent.putExtra(Intent.EXTRA_CC, mailTo.cc)
            intent.putExtra(Intent.EXTRA_SUBJECT, mailTo.subject)
            //mail.putExtra(Intent.EXTRA_TEXT, mailTo.body)
            try {
                startActivity(intent)
            } catch (ex: Exception) {
                Log.d(TAG, "" + ex.message)
                UtilMethods.browseUrlExternal(mContext, url)
                UtilMethods.showShortToast(mContext, "Activity Not Found")
            }
            return true

        } else if (url.contains("intent:")) {
            try {
                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                    return true
                }
                //try to find fallback url
                val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                if (fallbackUrl != null) {
                    mBinding.webView.loadUrl(fallbackUrl)
                    return true
                }
                //invite to install
                val marketIntent = Intent(Intent.ACTION_VIEW).setData(
                    Uri.parse("market://details?id=" + intent.getPackage()!!)
                )
                if (marketIntent.resolveActivity(packageManager) != null) {
                    try {
                        startActivity(intent)
                    } catch (ex: ActivityNotFoundException) {
                        UtilMethods.showShortToast(mContext, "Activity Not Found")
                    }
                    return true
                }
            } catch (e: Exception) {
                UtilMethods.browseUrlExternal(mContext, url)
                e.printStackTrace()
            }

        } else if (url.contains("whatsapp://")
            || url.contains("app.whatsapp")
            || url.contains("api.whatsapp")
        ) {
            try {
                val sendIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                sendIntent.setPackage("com.whatsapp")
                sendIntent.setPackage("com.gbwhatsapp")
                sendIntent.setPackage("com.yowhatsapp")
                sendIntent.setPackage("com.fmwhatsapp")
                startActivity(sendIntent)
            } catch (ex: Exception) {
                UtilMethods.browseUrlExternal(mContext, url)
                ex.printStackTrace()
            }
            return true;

        } else if (url.startsWith("viber:")) {
            try {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                ).apply {
                    setPackage("com.viber.voip")
                }
                startActivity(intent)
            } catch (ex: Exception) {
                UtilMethods.browseUrlExternal(mContext, url)
                ex.printStackTrace()
            }
            return true

        } else if (url.contains("facebook.com/sharer") ||
            url.contains("twitter.com/intent") ||
            url.contains("plus.google.com") ||
            url.contains("pinterest.com/pin")
        ) {
            UtilMethods.browseUrlExternal(mContext, url)
            return true
        } else if (url.contains("geo:")
            || url.contains("market://")
            || url.contains("play.google")
            || url.contains("vid:")
            || url.contains("youtube:")
            || url.contains("viber:")
            || url.contains("fb-messenger:")
            || url.contains("?target=external")
        ) {

            UtilMethods.browseUrlExternal(mContext, url)
            return true
        }

        if (isPopup) {
            return false
        }

        if (UtilMethods.isConnectedToInternet(mContext)) {
            if (url.startsWith("http:") || url.startsWith("https:")) {
                val host = Uri.parse(url).host ?: ""

                /*if (host.contains("m.facebook.com")
                        || host.contains("facebook.co")
                        || host.contains("www.facebook.com")){

                    val activityCode = 104
                    val intent = Intent()
                    intent.setClassName("com.facebook.katana", "com.facebook.katana.ProxyAuth")
                    intent.putExtra("client_id", application.packageName)
                    startActivityForResult(intent, activityCode)
                    return true
                }*/

                for (item in AppDataInstance.appConfig.externalBrowserUrl) {
                    when (item.urlCondition) {
                        Constants.WEBVIEW_OPEN_CONDITION.EQUAL.name -> {
                            if (item.url == url) {
                                if (item.urlType == Constants.WEBVIEW_OPEN_TYPE.CUSTOM_TAB.name) {
                                    UtilMethods.browseUrlCustomTab(mContext, url)
                                    return true
                                } else {
                                    UtilMethods.browseUrlExternal(mContext, url)
                                    return true
                                }
                            }
                        }
                        Constants.WEBVIEW_OPEN_CONDITION.NOT_EQUAL.name -> {
                            if (item.url != url) {
                                if (item.urlType == Constants.WEBVIEW_OPEN_TYPE.CUSTOM_TAB.name) {
                                    UtilMethods.browseUrlCustomTab(mContext, url)
                                    return true
                                } else {
                                    UtilMethods.browseUrlExternal(mContext, url)
                                    return true
                                }
                            }
                        }
                        Constants.WEBVIEW_OPEN_CONDITION.CONTAIN.name -> {
                            if (url.contains(item.url)) {
                                if (item.urlType == Constants.WEBVIEW_OPEN_TYPE.CUSTOM_TAB.name) {
                                    UtilMethods.browseUrlCustomTab(mContext, url)
                                    return true
                                } else {
                                    UtilMethods.browseUrlExternal(mContext, url)
                                    return true
                                }
                            }
                        }
                        Constants.WEBVIEW_OPEN_CONDITION.NOT_CONTAIN.name -> {
                            if (!url.contains(item.url)) {
                                if (item.urlType == Constants.WEBVIEW_OPEN_TYPE.CUSTOM_TAB.name) {
                                    UtilMethods.browseUrlCustomTab(mContext, url)
                                    return true
                                } else {
                                    UtilMethods.browseUrlExternal(mContext, url)
                                    return true
                                }
                            }
                        }
                        Constants.WEBVIEW_OPEN_CONDITION.START_WITH.name -> {
                            if (url.startsWith(item.url)) {
                                if (item.urlType == Constants.WEBVIEW_OPEN_TYPE.CUSTOM_TAB.name) {
                                    UtilMethods.browseUrlCustomTab(mContext, url)
                                    return true
                                } else {
                                    UtilMethods.browseUrlExternal(mContext, url)
                                    return true
                                }
                            }
                        }
                        Constants.WEBVIEW_OPEN_CONDITION.NOT_START_WITH.name -> {
                            if (!url.startsWith(item.url)) {
                                if (item.urlType == Constants.WEBVIEW_OPEN_TYPE.CUSTOM_TAB.name) {
                                    UtilMethods.browseUrlCustomTab(mContext, url)
                                    return true
                                } else {
                                    UtilMethods.browseUrlExternal(mContext, url)
                                    return true
                                }
                            }
                        }
                    }
                }

                if (url.contains("drive.google.com")) {
                    UtilMethods.browseUrlCustomTab(mContext, url)
                    return true
                }

                if (url.contains("google.com/maps") ||
                    url.contains("maps.app.goo.gl") ||
                    url.contains("maps.google.com") ||
                    url.contains("facebook.com") ||
                    url.contains("instagram.com") ||
                    url.contains("twitter.com")
                ) {
                    UtilMethods.browseUrlExternal(mContext, url)
                    return true
                }

                if (url.contains("play.google.com/store/apps/details?id=")) {
                    val appId = url.split("?id=")[1]
                    try {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=$appId")
                            )
                        )
                    } catch (ex: ActivityNotFoundException) {
                        UtilMethods.browseUrlExternal(mContext, url)
                    }
                    return true
                }

                if (host.contains("m.facebook.com")
                    || host.contains("facebook.co")
                    || host.contains(".google.com")
                    || host.contains(".google.co")
                    || host.contains("oauth.googleusercontent.com")
                    || host.contains("content.googleapis.com")
                    || host.contains("ssl.gstatic.com")
                ) {
                    /*val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    startActivity(intent)
                    return true*/
                    showLoader()
                    return false
                } else if (host.contains("t.me")) {
                    UtilMethods.browseUrlExternal(mContext, url)
                    return true
                } else if (host == Uri.parse(mDefaultURL).host) {
                    if (mWebViewPop != null) {
                        mWebViewPop!!.visibility = View.GONE
                        mWebViewPop = null
                    }
                }
                showLoader()
                return false
            } else {
                UtilMethods.browseUrlExternal(mContext, url)
                return true
            }
        } else {
            hideWebView()
            showNoInternet()
            hideLoader()
        }
        mLastUrl = url
        showLoader()
        return false
    }



    var mCustomView: View? = null
    var mCustomViewCallback: WebChromeClient.CustomViewCallback? = null
    var mOriginalOrientation: Int = 0
    var mOriginalSystemUiVisibility: Int = 0

    internal inner class CustomChromeClient : WebChromeClient() {

        override fun onShowFileChooser(
            webView: WebView, filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {

            UtilMethods.printLog(TAG, fileChooserParams.acceptTypes[0])

            return if (ContextCompat.checkSelfPermission(
                    this@HomeActivity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ) {
                if (mFileMessage != null) {
                    mFileMessage?.onReceiveValue(null)
                }
                mFileMessage = filePathCallback
                if (Arrays.asList<String>(*fileChooserParams.acceptTypes).contains("audio/*")) {
                    val chooserIntent = fileChooserParams.createIntent()
                    startActivityForResult(chooserIntent, CODE_AUDIO_CHOOSER)
                    return true
                }
                var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent!!.resolveActivity(this@HomeActivity.getPackageManager()) != null) {
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
                        takePictureIntent.putExtra("PhotoPath", mFileManagerImagePath)
                    } catch (ex: IOException) {
                        Log.e(TAG, "Image file creation failed", ex)
                    }
                    if (photoFile != null) {
                        mFileManagerImagePath = "file:" + photoFile.absolutePath
                        takePictureIntent!!.putExtra(
                            MediaStore.EXTRA_OUTPUT,
                            FileProvider.getUriForFile(
                                this@HomeActivity,
                                "$packageName.provider", photoFile
                            )
                        )
                    } else {
                        takePictureIntent = null
                    }
                }
                var takeVideoIntent: Intent? = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                if (takeVideoIntent!!.resolveActivity(this@HomeActivity.getPackageManager()) != null) {
                    var videoFile: File? = null
                    try {
                        videoFile = createVideoFile()
                        takeVideoIntent.putExtra("PhotoPath", mFileManagerVideoPath)
                    } catch (ex: IOException) {
                        Log.e(TAG, "Video file creation failed", ex)
                    }
                    if (videoFile != null) {
                        mFileManagerVideoPath = "file:" + videoFile.absolutePath
                        takeVideoIntent!!.putExtra(
                            MediaStore.EXTRA_OUTPUT,
                            FileProvider.getUriForFile(
                                this@HomeActivity,
                                "$packageName.provider", videoFile
                            )
                        )
                    } else {
                        takeVideoIntent = null
                    }
                }
                val contentSelectionIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                contentSelectionIntent.setDataAndType(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    "image/* video/*"
                )
                val mimeTypes = arrayOf(
                    "text/csv",
                    "text/comma-separated-values",
                    "application/pdf",
                    "image/*",
                    "video/*",
                    "*/*"
                )
                contentSelectionIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                val intentArray: Array<Intent?>
                intentArray = if (takePictureIntent != null && takeVideoIntent != null) {
                    arrayOf(takePictureIntent, takeVideoIntent)
                } else takePictureIntent?.let { arrayOf(it) }
                    ?: (takeVideoIntent?.let { arrayOf(it) } ?: arrayOfNulls(0))
                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Upload")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                startActivityForResult(chooserIntent, FCR)
                true
            } else {
                Toast.makeText(this@HomeActivity, "Storage Permission Denied", Toast.LENGTH_SHORT)
                    .show()
                ActivityCompat.requestPermissions(
                    (this@HomeActivity as Activity?)!!, arrayOf<String>(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ), REQUEST_EXTERNAL_STORAGE
                )
                false
            }

        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String,
            callback: GeolocationPermissions.Callback
        ) {
            if (Build.VERSION.SDK_INT < 23 || askForPermission(
                    PERMISSIONS_REQUEST_LOCATION,
                    true
                )
            ) {
                // location permissions were granted previously so auto-approve
                callback.invoke(origin, true, false)
                locationSettingsRequest()
            }
        }

        override fun onCreateWindow(
            view: WebView?, isDialog: Boolean,
            isUserGesture: Boolean, resultMsg: Message
        ): Boolean {
            var popupUrl: String? = null
            if (view?.hitTestResult?.type === WebView.HitTestResult.SRC_ANCHOR_TYPE
                || view?.hitTestResult?.type === WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
            ) {
                popupUrl = view.hitTestResult.extra
                // To open popup link inside of app
                /*if (popupUrl?.compareTo("about:blank") != 0){
                    mBinding.webView.loadUrl(popupUrl.toString())
                }*/
            }
            UtilMethods.printLog(TAG, "Popup URL: $popupUrl")

            mWebViewPop = WebView(mContext)
            mWebViewPop!!.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(webView: WebView, url: String): Boolean {
                    UtilMethods.printLog(TAG, "OverrideUrl (1/2): $url")
                    val result = shouldOverrideUrlHandler(webView, url, false)
                    if (result) {
                        onCloseWindow(WebView(mContext))
                    }
                    return result
                }
            }

            val cookieManager: CookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            if (Build.VERSION.SDK_INT >= 21) {
                cookieManager.setAcceptThirdPartyCookies(mWebViewPop, true)
                cookieManager.setAcceptThirdPartyCookies(mBinding.webView, true)
            }
            val popSettings = mWebViewPop!!.settings
            mWebViewPop!!.isVerticalScrollBarEnabled = false
            mWebViewPop!!.isHorizontalScrollBarEnabled = false
            popSettings.javaScriptEnabled = true
            popSettings.saveFormData = true
            popSettings.setEnableSmoothTransition(true)
            popSettings.userAgentString = getString(R.string.user_agent_string)
            popSettings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
            mWebViewPop!!.webChromeClient = CustomChromeClient()

            mWebViewPopBuilder = AlertDialog.Builder(
                this@HomeActivity,
                android.R.style.Theme_Black_NoTitleBar_Fullscreen
            )
                .create()
            mWebViewPopBuilder!!.setTitle("")
            mWebViewPopBuilder!!.setView(mWebViewPop!!)
            mWebViewPopBuilder!!.setButton("Close") { dialog, id ->
                if (mWebViewPop != null) {
                    mWebViewPop!!.destroy()
                }
                dialog.dismiss()
            }
            mWebViewPopBuilder!!.show()
            mWebViewPopBuilder!!.window!!.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            val transport = resultMsg.obj as WebView.WebViewTransport
            transport.webView = mWebViewPop!!
            resultMsg.sendToTarget()
            return true
        }

        override fun onCloseWindow(window: WebView) {
            hideLoader()
            try {
                if (mWebViewPop != null) {
                    mWebViewPop!!.destroy()
                }
            } catch (e: Exception) {
                UtilMethods.printLog("Webview Destroy Error: ", e.stackTrace.toString())
            }
            try {
                if (mWebViewPopBuilder != null) {
                    mWebViewPopBuilder!!.dismiss()
                }
            } catch (e: Exception) {
                UtilMethods.printLog("Builder Dismiss Error: ", e.stackTrace.toString())
            }
        }

        override fun onJsAlert(
            view: WebView?,
            url: String?,
            message: String?,
            result: JsResult?
        ): Boolean {
            return super.onJsAlert(view, url, message, result)
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            val url = view!!.url
            Log.d(TAG, "URL (JS): $url")
            super.onProgressChanged(view, newProgress)
            /**
             * Set progress view
             *
             * progress.setProgress(newProgress);
            if (newProgress == 100) {
            progress.setProgress(0);
            }**/
        }

        override fun onShowCustomView(
            paramView: View?,
            paramCustomViewCallback: CustomViewCallback
        ) {
            if (mCustomView != null) {
                onHideCustomView()
                return
            }
            mCustomView = paramView
            mOriginalSystemUiVisibility = window.decorView.systemUiVisibility
            mOriginalOrientation = requestedOrientation
            mCustomViewCallback = paramCustomViewCallback
            (window.decorView as FrameLayout).addView(mCustomView, FrameLayout.LayoutParams(-1, -1))
            window.decorView.systemUiVisibility = 3846

        }


        override fun onHideCustomView() {
            (window.decorView as FrameLayout).removeView(mCustomView)
            mCustomView = null
            window.decorView.systemUiVisibility = mOriginalSystemUiVisibility
            requestedOrientation = mOriginalOrientation
            mCustomViewCallback?.onCustomViewHidden()
            mCustomViewCallback = null
        }

        override fun onPermissionRequest(permissionRequest: PermissionRequest?) {
            mPermissionRequest = permissionRequest

            UtilMethods.printLog(TAG, "onJSPermissionRequest")
            mJsRequestCount = permissionRequest?.resources?.size ?: 0
            for (request in permissionRequest?.resources!!) {
                UtilMethods.printLog(
                    TAG,
                    "AskForPermission for" + permissionRequest.origin.toString() + "with" + request
                )
                when (request) {
                    "android.webkit.resource.AUDIO_CAPTURE" -> askForPermission(
                        PERMISSIONS_REQUEST_MICROPHONE,
                        true
                    )
                    "android.webkit.resource.VIDEO_CAPTURE" -> askForPermission(
                        PERMISSIONS_REQUEST_CAMERA,
                        true
                    )
                }
            }
        }

        override fun onPermissionRequestCanceled(request: PermissionRequest?) {
            super.onPermissionRequestCanceled(request)
            Toast.makeText(mContext, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }


    @Throws(IOException::class)
    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFileName = "JPEG_$timeStamp"
        val mediaStorageDir = cacheDir
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Oops! Failed create " + "WebView" + " directory")
                return null
            }
        }
        return File.createTempFile(
            imageFileName,
            ".jpg",
            mediaStorageDir
        )
    }

    @Throws(IOException::class)
    private fun createVideoFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFileName = "VID_$timeStamp"
        val mediaStorageDir = cacheDir
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Oops! Failed create " + "WebView" + " directory")
                return null
            }
        }
        return File.createTempFile(
            imageFileName,
            ".mp4",
            mediaStorageDir
        )
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    private fun generateKey(): SecretKey {
        val random = SecureRandom()
        val key = byteArrayOf(1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0)
        //random.nextBytes(key)
        return SecretKeySpec(key, "AES")
    }

    // download manager
    internal var mDownloadCompleteListener: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            clearDownloadingState()
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)
            val fileUri = mDownloadManger!!.getUriForDownloadedFile(id)
        }
    }

    private fun startDownload(
        url: String,
        disposition: String,
        userAgent: String,
        mimeType: String
    ) {
        val fileUri = Uri.parse(url)
        //val fileName = fileUri.lastPathSegment
        val fileName = URLUtil.guessFileName(url, disposition, mimeType)
        val cookies = CookieManager.getInstance().getCookie(url)

        try {
            val request = DownloadManager.Request(fileUri)
            request.setMimeType(mimeType)
                .addRequestHeader("cookie", cookies)
                .addRequestHeader("User-Agent", userAgent)
                .setDescription("Downloading file...")
                .setTitle(
                    URLUtil.guessFileName(
                        url, disposition,
                        mimeType
                    )
                )
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, fileName
                )
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)

            //registerReceiver(mDownloadCompleteListener, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            UtilMethods.showLongToast(mContext, "Downloading File")
        } catch (ex: java.lang.Exception) {
            UtilMethods.showLongToast(mContext, "Download failed!")
            UtilMethods.printLog(TAG, "${ex.message}")
        }
    }

    //cancel download, no active
    protected fun cancelDownload() {
        if (mOnGoingDownload != null) {
            mDownloadManger!!.remove(mOnGoingDownload!!)
            clearDownloadingState()
        }
    }

    protected fun clearDownloadingState() {
        unregisterReceiver(mDownloadCompleteListener)
        //mCancel.setVisibility(View.GONE);
        mOnGoingDownload = null
    }

    override fun onCreateContextMenu(
        contextMenu: ContextMenu, view: View?,
        contextMenuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(contextMenu, view, contextMenuInfo)
        val webViewHitTestResult: WebView.HitTestResult = mBinding.webView.hitTestResult
        if (webViewHitTestResult.type == WebView.HitTestResult.IMAGE_TYPE ||
            webViewHitTestResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
        ) {
            contextMenu.setHeaderTitle("Download Image From Below")
            contextMenu.add(0, 1, 0, "Save - Download Image")
                .setOnMenuItemClickListener {
                    val DownloadImageURL = webViewHitTestResult.extra
                    if (URLUtil.isValidUrl(DownloadImageURL)) {
                        val request = DownloadManager.Request(Uri.parse(DownloadImageURL))
                        request.allowScanningByMediaScanner()
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        val downloadManager =
                            getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                        downloadManager.enqueue(request)
                        Toast.makeText(
                            mContext,
                            "Image Downloaded Successfully.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            mContext,
                            "Sorry.. Something Went Wrong.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    false
                }
        }
    }

    var uploadMessage: ValueCallback<Array<Uri>?>? = null
    private var mUploadMessage: ValueCallback<Uri?>? = null
    val REQUEST_SELECT_FILE = 100
    private val FILECHOOSER_RESULTCODE = 1

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == REQUEST_SELECT_FILE) {
                if (uploadMessage == null) return
                uploadMessage!!.onReceiveValue(
                    WebChromeClient.FileChooserParams.parseResult(
                        resultCode,
                        intent
                    )
                )
                uploadMessage = null
            }
        } else if (requestCode == FILECHOOSER_RESULTCODE) {
            if (null == mUploadMessage) return
            // Use MainActivity.RESULT_OK if you're implementing WebView inside Fragment
            // Use RESULT_OK only if you're implementing WebView inside an Activity
            val result =
                if (intent == null || resultCode != RESULT_OK) null else intent.data
            mUploadMessage!!.onReceiveValue(result)
            mUploadMessage = null
        }
        var results: Array<Uri?>? = emptyArray()
        var uri: Uri? = null
        if (requestCode == FCR) {
            if (resultCode == RESULT_OK) {
                if (mFileMessage == null) {
                    return
                }
                if (intent == null || intent.data == null) {
                    if (intent != null && intent.clipData != null) {
                        val count =
                            intent.clipData!!.itemCount //evaluate the count before the for loop --- otherwise, the count is evaluated every loop.

                        results = arrayOfNulls(intent.clipData!!.itemCount)
                        for (i in 0 until count) {
                            uri = intent.clipData!!.getItemAt(i).uri
                            // results = new Uri[]{Uri.parse(mCM)};
                            results[i] = uri
                        }
                        //do something with the image (save it to some directory or whatever you need to do with it here)
                    }
                    if (mFileManagerImagePath != null) {
                        val file = File(Uri.parse(mFileManagerImagePath).path)
                        if (file.length() > 0)
                            results = arrayOf(Uri.parse(mFileManagerImagePath))
                    }
                    if (mFileManagerVideoPath != null) {
                        val file = File(Uri.parse(mFileManagerVideoPath).path)
                        if (file.length() > 0)
                            results = arrayOf(Uri.parse(mFileManagerVideoPath))
                    }
                } else {
                    val dataString = intent.dataString
                    if (dataString != null) {
                        results = arrayOf(Uri.parse(dataString))
                    } else {
                        if (intent.clipData != null) {
                            val numSelectedFiles = intent.clipData!!.itemCount
                            results = arrayOfNulls(numSelectedFiles)
                            for (i in 0 until numSelectedFiles) {
                                results[i] = intent.clipData!!.getItemAt(i).uri
                            }
                        }
                    }
                }
            } else {
                if (mFileManagerImagePath != null) {
                    val file = File(Uri.parse(mFileManagerImagePath).path)
                    if (file != null) file.delete()
                }
                if (mFileManagerVideoPath != null) {
                    val file = File(Uri.parse(mFileManagerVideoPath).path)
                    if (file != null) file.delete()
                }
            }
            mFileMessage?.onReceiveValue(results as Array<Uri>)
            mFileMessage = null
        } else if (requestCode == CODE_AUDIO_CHOOSER) {
            if (resultCode == RESULT_OK) {
                if (intent != null && intent.data != null) {
                    results = arrayOf(intent.data!!)
                }
            }
            mFileMessage?.onReceiveValue(results as Array<Uri>)
            mFileMessage = null
        } else if (requestCode == CODE_QR_SCAN) {
            if (resultCode == RESULT_OK) {
                if (intent != null) {
                    val result =
                        intent.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult")
                    if (result != null && URLUtil.isValidUrl(result)) {
                        mBinding.webView.loadUrl(result)
                    }
                }
            }
        }
        /* else {
            super.handleActivityResult(requestCode, resultCode, intent);
        }*/
    }

    private fun requestAllPermissions() {
        val requestList: ArrayList<String> = arrayListOf()

        // Access photos Permission
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestList.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
            }
        }else{
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        // Location Permission
        if (ActivityCompat.checkSelfPermission(
                mContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestList.add(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            getLocation()
        }

        if (ActivityCompat.checkSelfPermission(
                mContext,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestList.add(Manifest.permission.CAMERA)
        }

        if (ActivityCompat.checkSelfPermission(
                mContext,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestList.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    mContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestList.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (requestList.size > 0) {
            ActivityCompat.requestPermissions(
                this,
                requestList.toTypedArray(),
                PERMISSIONS_REQUEST_ALL
            )
        }
    }

    private fun jsPermissionAccepted() {
        mJsRequestCount--
        if (mPermissionRequest != null && mJsRequestCount == 0) {
            mPermissionRequest!!.grant(mPermissionRequest!!.resources)
        }
    }

    private fun askForPermission(permissionCode: Int, request: Boolean): Boolean {
        when (permissionCode) {
            PERMISSIONS_REQUEST_LOCATION ->
                if (Build.VERSION.SDK_INT > 23 && ContextCompat.checkSelfPermission(
                        mContext,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    if (request) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(
                                this@HomeActivity,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        ) {
                            UtilMethods.showSnackbar(
                                mBinding.rootContainer,
                                "Location permission is required, Please allow from permission manager!!"
                            )
                        } else {
                            ActivityCompat.requestPermissions(
                                this@HomeActivity,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                PERMISSIONS_REQUEST_LOCATION
                            )
                        }
                    }
                    return false
                } else {
                    jsPermissionAccepted()
                    return true
                }
            PERMISSIONS_REQUEST_CAMERA ->
                if (Build.VERSION.SDK_INT > 23 && ContextCompat.checkSelfPermission(
                        mContext,
                        Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    if (request) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(
                                this@HomeActivity,
                                Manifest.permission.CAMERA
                            )
                        ) {
                            UtilMethods.showSnackbar(
                                mBinding.rootContainer,
                                "Camera permission is required, Please allow from permission manager!!"
                            )
                        } else {
                            ActivityCompat.requestPermissions(
                                this@HomeActivity,
                                arrayOf(Manifest.permission.CAMERA),
                                PERMISSIONS_REQUEST_CAMERA
                            )
                        }
                    }
                    return false
                } else {
                    jsPermissionAccepted()
                    return true
                }
            PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE ->
                if (Build.VERSION.SDK_INT > 23 && ContextCompat.checkSelfPermission(
                        mContext,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    if (request) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(
                                this@HomeActivity,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                        ) {
                            UtilMethods.showSnackbar(
                                mBinding.rootContainer,
                                "Write permission is required, Please allow from permission manager!!"
                            )
                        } else {
                            ActivityCompat.requestPermissions(
                                this@HomeActivity,
                                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
                            )
                        }
                    }
                    return false
                } else {
                    jsPermissionAccepted()
                    return true
                }
            PERMISSIONS_REQUEST_MICROPHONE ->
                if (Build.VERSION.SDK_INT > 23 && ContextCompat.checkSelfPermission(
                        mContext,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    if (request) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(
                                this@HomeActivity,
                                Manifest.permission.RECORD_AUDIO
                            )
                        ) {
                            UtilMethods.showSnackbar(
                                mBinding.rootContainer,
                                "Audio permission is required, Please allow from permission manager!!"
                            )
                        } else
                            ActivityCompat.requestPermissions(
                                this@HomeActivity,
                                arrayOf(Manifest.permission.RECORD_AUDIO),
                                PERMISSIONS_REQUEST_MICROPHONE
                            )
                    }
                    return false
                } else {
                    jsPermissionAccepted()
                    return true
                }
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_ALL -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission accept location
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        UtilMethods.printLog(TAG, "External permission accept.")
                    }

                    // permission accept location
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        UtilMethods.printLog(TAG, "Location permission accept.")
                        getLocation()
                    }

                } else {
                    //UtilMethods.showSnackbar(mBinding.rootContainer, "Permission Failed!")
                }
                return
            }
            PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    UtilMethods.printLog(TAG, "Write permission accept.")
                    jsPermissionAccepted()
                } else {
                    UtilMethods.showSnackbar(mBinding.rootContainer, "Write Permission Failed!")
                }
            }
            PERMISSIONS_REQUEST_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    UtilMethods.printLog(TAG, "Camera permission accept.")
                    jsPermissionAccepted()
                } else {
                    UtilMethods.showSnackbar(mBinding.rootContainer, "Camera Permission Failed!")
                }
            }
            PERMISSIONS_REQUEST_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    UtilMethods.printLog(TAG, "Location permission accept.")
                    getLocation()
                    jsPermissionAccepted()
                } else {
                    UtilMethods.showSnackbar(mBinding.rootContainer, "Location Permission Failed!")
                }
            }
            PERMISSIONS_REQUEST_MICROPHONE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    UtilMethods.printLog(TAG, "Microphone Permission Accept.")
                    jsPermissionAccepted()
                } else {
                    UtilMethods.showSnackbar(
                        mBinding.rootContainer,
                        "Microphone Permission Failed!"
                    )
                }
            }
        }
    }

    // get user location for
    private fun getLocation(): String {
        var newloc = "0,0"
        //Checking for location permissions
        if (askForPermission(PERMISSIONS_REQUEST_LOCATION, false)) {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            val gps = WebviewGPSTrack(mContext)
            val latitude = gps.getLatitude()
            val longitude = gps.getLongitude()
            if (gps.canGetLocation()) {
                if (latitude != 0.0 || longitude != 0.0) {
                    cookieManager.setCookie(mDefaultURL, "lat=$latitude")
                    cookieManager.setCookie(mDefaultURL, "long=$longitude")
                    newloc = "$latitude,$longitude"
                } else {
                    UtilMethods.printLog(TAG, "Location null.")
                }
            } else {
                UtilMethods.printLog(TAG, "Location read failed.")
            }
        }
        return newloc
    }

    private fun locationSettingsRequest() {
        val locationManager = mContext
            .getSystemService(Service.LOCATION_SERVICE) as LocationManager
        val isGPSEnabled = locationManager
            .isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (askForPermission(PERMISSIONS_REQUEST_LOCATION, false) && isGPSEnabled == false) {
            val mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(2000)
                .setFastestInterval(1000)

            val settingsBuilder = LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest)
            settingsBuilder.setAlwaysShow(true)

            val result = LocationServices.getSettingsClient(mContext)
                .checkLocationSettings(settingsBuilder.build())
            result.addOnCompleteListener { task ->
                try {
                    task.getResult(ApiException::class.java)
                } catch (ex: ApiException) {

                    when (ex.statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                            Toast.makeText(mContext, "GPS IS OFF", Toast.LENGTH_SHORT).show()
                            val resolvableApiException = ex as ResolvableApiException
                            resolvableApiException.startResolutionForResult(
                                this,
                                REQUEST_CHECK_SETTINGS
                            )
                        } catch (e: IntentSender.SendIntentException) {
                            Toast.makeText(
                                mContext,
                                "PendingIntent unable to execute request.",
                                Toast.LENGTH_SHORT
                            ).show()

                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            Toast.makeText(
                                mContext,
                                "Something is wrong in your GPS",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun showAboutUs() {
        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popUpView = inflater.inflate(R.layout.layout_about_us, null)

        val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
        colorDrawable.alpha = 70

        mAboutUsPopup = PopupWindow(
            popUpView, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        mAboutUsPopup.setBackgroundDrawable(colorDrawable)
        mAboutUsPopup.isOutsideTouchable = true

        if (Build.VERSION.SDK_INT >= 21) {
            mAboutUsPopup.setElevation(5.0f)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mAboutUsPopup.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
        } else {
            mAboutUsPopup.showAsDropDown(popUpView, Gravity.CENTER, 0)
        }

        val txtTitle = popUpView.findViewById<View>(R.id.txt_about_us_title) as TextView
        val txtDetail = popUpView.findViewById<View>(R.id.txt_about_us_detail) as TextView
        val btnClose = popUpView.findViewById<View>(R.id.btn_done) as ImageView
        val btnRate = popUpView.findViewById<View>(R.id.img_rate) as ImageView
        val btnEmail = popUpView.findViewById<View>(R.id.img_email) as ImageView
        val btnShare = popUpView.findViewById<View>(R.id.img_share) as ImageView

        txtTitle.text = PreferenceUtils.getStringValue(Constants.KEY_ABOUT_WEBSITE, "")
        txtDetail.text = PreferenceUtils.getStringValue(Constants.KEY_ABOUT_TEXT, "")

        btnClose.setOnClickListener { mAboutUsPopup.dismiss() }

        btnRate.setOnClickListener {
            mAboutUsPopup.dismiss()
            UtilMethods.rateTheApp(mContext)
        }

        btnShare.setOnClickListener {
            mAboutUsPopup.dismiss()
            UtilMethods.shareTheApp(
                mContext,
                "Download " + getString(R.string.app_name) + "" +
                        " app from play store. Click here: " + "" +
                        "https://play.google.com/store/apps/details?id=" + packageName
            )
        }

        btnEmail.setOnClickListener {
            mAboutUsPopup.dismiss()
            UtilMethods.sandMailTo(
                mContext, "Contact with email!",
                PreferenceUtils.getStringValue(Constants.KEY_ABOUT_EMAIL, "")!!,
                "Contact with via " + R.string.app_name + " app", ""
            )
        }
    }

    private fun showMore() {
        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popUpView = inflater.inflate(R.layout.layout_more, null)

        val colorDrawable = ColorDrawable(ContextCompat.getColor(mContext, R.color.black))
        colorDrawable.alpha = 70

        mAboutUsPopup = PopupWindow(
            popUpView, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        mAboutUsPopup.setBackgroundDrawable(colorDrawable)
        mAboutUsPopup.isOutsideTouchable = true

        if (Build.VERSION.SDK_INT >= 21) {
            mAboutUsPopup.setElevation(5.0f)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mAboutUsPopup.showAsDropDown(popUpView, Gravity.CENTER, 0, Gravity.CENTER)
        } else {
            mAboutUsPopup.showAsDropDown(popUpView, Gravity.CENTER, 0)
        }

        val btnConfirm = popUpView.findViewById<View>(R.id.btn_done) as ImageView
        val btnShare = popUpView.findViewById<View>(R.id.btn_share) as AppCompatButton
        val btnAbout = popUpView.findViewById<View>(R.id.btn_about) as AppCompatButton
        val btnRate = popUpView.findViewById<View>(R.id.btn_rate) as AppCompatButton
        val btnExit = popUpView.findViewById<View>(R.id.btn_exit) as AppCompatButton


        btnShare.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())
            )
        )
        btnAbout.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())
            )
        )
        btnRate.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())
            )
        )
        btnExit.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())
            )
        )

        btnConfirm.setOnClickListener { mAboutUsPopup.dismiss() }

        btnShare.setOnClickListener {
            mAboutUsPopup.dismiss()
            UtilMethods.shareTheApp(
                mContext,
                "Download " + getString(R.string.app_name) + "" +
                        " app from play store. Click here: " + "" +
                        "https://play.google.com/store/apps/details?id=" + packageName
            )
        }

        btnAbout.setOnClickListener {
            mAboutUsPopup.dismiss()
            showAboutUs()
        }

        btnRate.setOnClickListener {
            mAboutUsPopup.dismiss()
            UtilMethods.rateTheApp(mContext)
        }

        btnExit.setOnClickListener {
            exitHomeScreen()
        }

    }

    private fun initSliderMenu() {
        val isSelectedRtl = TextUtils.getLayoutDirectionFromLocale(
            Locale
                .getDefault()
        ) == ViewCompat.LAYOUT_DIRECTION_RTL
        if (isSelectedRtl || PreferenceUtils.getInstance()
                .getBooleanValue(Constants.KEY_RTL_ACTIVE, false)
        ) {
            mBinding.navigationView.layoutDirection = View.LAYOUT_DIRECTION_RTL
            mBinding.navigationView.textDirection = View.TEXT_DIRECTION_RTL
        }
        mBinding.navigationView.itemIconTintList = null
        val navigationMenu = mBinding.navigationView.menu
        navigationMenu.clear()

        var i = 1
        for (menu in AppDataInstance.appConfig.navigationMenus) {
            try {
                val iconId =
                    mContext.resources.getIdentifier(menu.icon, "drawable", mContext.packageName)
                navigationMenu.add(i++, i, Menu.NONE, menu.name).setIcon(iconId)
            } catch (ex: Exception) {
                UtilMethods.printLog(TAG, ex.message.toString())
                //navigationMenu.add(i++, i , Menu.NONE, menu.name).setIcon(R.drawable.ic_label)
            }
        }

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val menuUrl = AppDataInstance.appConfig.navigationMenus.find { it.name == item.title }?.url
            ?: "".toUpperCase()
        menuItemAction(menuUrl)
        if (mBinding.drawerLayout.isDrawerOpen(GravityCompat.START)) run {
            mBinding.drawerLayout.closeDrawer(GravityCompat.START)
        } else if (mBinding.drawerLayout.isDrawerOpen(GravityCompat.END)) run {
            mBinding.drawerLayout.closeDrawer(GravityCompat.END)
        }
        return true
    }

    private fun menuItemAction(url: String) {
        when (url) {
            getString(R.string.menu_home) -> loadBaseWebView()

            getString(R.string.menu_about) -> showAboutUs()

            getString(R.string.menu_rate) -> UtilMethods.rateTheApp(mContext)

            getString(R.string.menu_more) -> showMore()

            getString(R.string.menu_share) -> UtilMethods.shareTheApp(
                mContext,
                "Download " + getString(R.string.app_name) + "" +
                        " app from play store. Click here: " + "" +
                        "https://play.google.com/store/apps/details?id=" + packageName
            )

            getString(R.string.menu_exit) -> exitHomeScreen()

            else ->
                try {
                    mBinding.webView.loadUrl(url)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
        }
    }

    private fun exitHomeScreen() {
        this.finish()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            //setActiveFullScreen()
        }
    }

    private fun setActiveFullScreen() {
        if (PreferenceUtils.getInstance()
                .getBooleanValue(Constants.KEY_FULL_SCREEN_ACTIVE, false)
        ) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LOW_PROFILE
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }


    override fun onBackPressed() {
        if (mBinding.drawerLayout.isDrawerOpen(GravityCompat.START)) run {
            mBinding.drawerLayout.closeDrawer(GravityCompat.START)
        } else if (mBinding.drawerLayout.isDrawerOpen(GravityCompat.END)) run {
            mBinding.drawerLayout.closeDrawer(GravityCompat.END)
        } else if (mBinding.webView.canGoBack()) {
            showLoader()
            mBinding.webView.goBack()
        } else {
            if (isDoubleBackToExit) {
                //super.onBackPressed()
                finishAffinity()
                exitProcess(0)
            }

            isDoubleBackToExit = true
            UtilMethods.showSnackbar(mBinding.rootContainer, getString(R.string.massage_exit))

            Handler(Looper.getMainLooper()).postDelayed({
                run {
                    isDoubleBackToExit = false
                }
            }, 2000)

        }
    }

    fun notificationClickSync() {
        if (AppDataInstance.notificationUrl != "" && isApplicationAlive) {
            loadBaseWebView()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mBinding.webView.saveState(outState)
        //outState.clear()
    }

    /*override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        mBinding.webView.restoreState(savedInstanceState)
    }*/

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            readBundle(intent.extras)
        } else {
            UtilMethods.printLog(TAG, "New intent Extras is empty!")
        }
    }

    private fun readBundle(extras: Bundle?) {
        if (extras != null) {
            AppDataInstance.notificationUrl =
                extras.getString(Constants.KEY_NOTIFICATION_URL).orEmpty()
            AppDataInstance.notificationUrlOpenType =
                extras.getString(Constants.KEY_NOTIFICATION_OPEN_TYPE).orEmpty()

            notificationClickSync()

            UtilMethods.printLog(TAG, "URL: " + AppDataInstance.notificationUrl)
            UtilMethods.printLog(TAG, "Type: " + AppDataInstance.notificationUrlOpenType)

        } else {
            UtilMethods.printLog(TAG, "New intent Bundle is empty!!")
        }
    }

    override fun onStart() {
        super.onStart()
        isApplicationAlive = true
        setActiveFullScreen()
        if (resources.getBoolean(R.bool.screen_rotation)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    @SuppressLint("SwitchIntDef")
    override fun onResume() {
        super.onResume()
        isApplicationAlive = true
        initView()
        ThemeConfig(mContext, this).initThemeColor()
        ThemeConfig(mContext, this).initThemeStyle()
        mBinding.webView.onResume()
        setActiveFullScreen()
    }

    override fun onPause() {
        super.onPause()
        isApplicationAlive = false
        mBinding.webView.onPause()
        mBinding.webView.loadUrl("javascript:document.location=document.location")
    }


    override fun onRestart() {
        super.onRestart()
        isApplicationAlive = true
        notificationClickSync()

        if (mLastUrl != "") {
            isViewLoaded = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isApplicationAlive = false
        mBinding.webView.destroy()
    }
}
