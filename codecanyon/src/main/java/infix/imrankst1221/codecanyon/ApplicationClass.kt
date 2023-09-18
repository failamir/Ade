package infix.imrankst1221.codecanyon
/**
 * Created by Md Imran Choudhury on 10/Aug/2018.
 * All rights received InfixSoft
 * Contact email: imrankst1221@gmail.com
 */

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.onesignal.OneSignal
import infix.imrankst1221.codecanyon.ui.home.HomeActivity
import infix.imrankst1221.codecanyon.ui.splash.SplashActivity
import infix.imrankst1221.rocket.library.setting.AppDataInstance
import infix.imrankst1221.rocket.library.utility.Constants
import infix.imrankst1221.rocket.library.utility.PreferenceUtils
import infix.imrankst1221.rocket.library.utility.UtilMethods
import org.json.JSONException


class ApplicationClass: Application() {
    val TAG = "---ApplicationClass"
    lateinit var mContext: Context
    private var currentActivity: Activity? = null
    private var appOpenAdManager: AppOpenAdManager? = null
    private var adsNotReadyRetry = 0
    private var isAdsAlreadyFailed = false

    override fun onCreate() {
        super.onCreate()
        mContext = this

        initConfig()
        applyTheme()
    }

    private fun initConfig(){
        AppDataInstance.getINSTANCE(mContext)
        PreferenceUtils.getInstance().initPreferences(mContext)

        if (resources.getString(R.string.admob_app_id).isNotEmpty()) {
            MobileAds.initialize(this) {}
            if (resources.getString(R.string.admob_open_ads_unit_id).isNotEmpty()) {
                appOpenAdManager = AppOpenAdManager()
                appOpenAdManager?.loadAd(this)
            }
        }

        if(resources.getBoolean(R.bool.enable_firebase_notification)) {
            initFirebase()
        }
        if (resources.getBoolean(R.bool.enable_onesignal)) {
            initOnesignal()
        }
    }

    private fun initFirebase(){
        FirebaseApp.initializeApp(mContext)
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }

    private fun initOnesignal(){
        //OneSignal.setLogLevel(OneSignal.LOG_LEVEL.DEBUG, OneSignal.LOG_LEVEL.DEBUG)
        OneSignal.initWithContext(this)
        OneSignal.setAppId(getString(R.string.onesignal_app_id))
        PreferenceUtils.editStringValue(Constants.KEY_ONE_SIGNAL_USER_ID,
            OneSignal.getDeviceState()?.userId ?: "")
        OneSignal.setNotificationOpenedHandler { result ->
            try {
                val customKeyURL: String?
                val customKeyType: String?
                val actionType = result.action.type
                val notification = result.notification
                val launchURL = notification.launchURL
                val additionalData = notification.additionalData

                var notificationUrl: String = ""
                var notificationUrlOpenType: String = ""

                if (launchURL != null) {
                    notificationUrl = launchURL
                    notificationUrlOpenType = "INSIDE"
                    UtilMethods.printLog(TAG, "launchURL = $launchURL")
                }

                if (additionalData != null) {
                    customKeyURL = additionalData.optString(Constants.KEY_NOTIFICATION_URL, "")
                    if (customKeyURL.isNotEmpty()) {
                        customKeyType =
                            additionalData.optString(Constants.KEY_NOTIFICATION_OPEN_TYPE, "INSIDE")
                                .toUpperCase()
                        notificationUrl = customKeyURL
                        notificationUrlOpenType = customKeyType

                        UtilMethods.printLog(TAG, "customType = $customKeyType")
                        UtilMethods.printLog(TAG, "customURL = $customKeyURL")
                    }
                }

                if (HomeActivity::class.isInstance(mContext)) {
                    UtilMethods.printLog(TAG, "HomeActivity Instance.")
                    try {
                        AppDataInstance.notificationUrl = notificationUrl
                        AppDataInstance.notificationUrlOpenType = notificationUrlOpenType
                        (mContext as HomeActivity).notificationClickSync()
                    } catch (ex: Exception) {
                        UtilMethods.printLog(TAG, "" + ex.message)
                    }
                } else {
                    val intent = Intent(mContext.applicationContext, MainActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK

                    if (notificationUrl.isNotEmpty()) {
                        intent.putExtra(Constants.KEY_NOTIFICATION_URL, notificationUrl)
                        intent.putExtra(
                            Constants.KEY_NOTIFICATION_OPEN_TYPE,
                            notificationUrlOpenType
                        )
                    }
                    mContext.startActivity(intent)
                }
            }catch (ex: JSONException) {
                UtilMethods.printLog(TAG, ex.message.toString())
            }

        }
    }
    private fun applyTheme(){
        /**
         * Turn on/off dark mode
         * Flag for off: AppCompatDelegate.MODE_NIGHT_NO
         * Flag for on: AppCompatDelegate.MODE_NIGHT_YES
         */
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        /**
         * Disable taking screenshot
         */
        //window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }
    interface OnOpenAdListener {
        fun onShowAdComplete()
        fun onAdsFailToLoad()
    }

    fun showOpenAds(activity: SplashActivity){
        adsNotReadyRetry = 0
        currentActivity = activity
        appOpenAdManager?.showAdIfAvailable(activity, activity)
    }

    private inner class AppOpenAdManager {
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd = false

        fun loadAd(context: Context) {
            if (isLoadingAd || isAdAvailable()) {
                return
            }

            isLoadingAd = true
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                context,
                context.getString(R.string.admob_open_ads_unit_id),
                request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAd.AppOpenAdLoadCallback() {

                    override fun onAdLoaded(ad: AppOpenAd) {
                        UtilMethods.printLog(TAG, "Ad was loaded.")
                        isLoadingAd = false
                        appOpenAd = ad
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        UtilMethods.printLog(TAG, loadAdError.message)
                        isLoadingAd = false
                    }
                })
        }

        fun showAdIfAvailable(
            activity: SplashActivity,
            onOpenAdListener: OnOpenAdListener) {
            if (isShowingAd) {
                UtilMethods.printLog(TAG, "The app open ad is already showing.")
                return
            }

            if (isAdsAlreadyFailed) return

            if (!isAdAvailable()) {
                UtilMethods.printLog(TAG, "The app open ad is not ready yet.")
                if (adsNotReadyRetry > 2) {
                    isAdsAlreadyFailed = true
                    onOpenAdListener.onAdsFailToLoad()
                }else {
                    Handler(Looper.myLooper()!!).postDelayed({
                        adsNotReadyRetry++
                        appOpenAdManager?.showAdIfAvailable(activity, activity)
                    }, 1000)
                }
                return
            }


            if(appOpenAd != null) {
                currentActivity?.let {
                    isShowingAd = true
                    appOpenAd?.show(it)
                }
            }

            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {
                    UtilMethods.printLog(TAG, "Ad dismissed fullscreen content.")
                    appOpenAd = null
                    isShowingAd = false

                    onOpenAdListener.onShowAdComplete()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    UtilMethods.printLog(TAG, adError.message)
                    appOpenAd = null
                    isShowingAd = false

                    onOpenAdListener.onShowAdComplete()
                }

                override fun onAdShowedFullScreenContent() {
                    UtilMethods.printLog(TAG, "Ad showed fullscreen content.")
                }
            }
        }

        private fun isAdAvailable(): Boolean {
            return appOpenAd != null
        }
    }
}