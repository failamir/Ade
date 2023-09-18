package infix.imrankst1221.codecanyon.ui.splash

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import android.view.View
import infix.imrankst1221.codecanyon.ApplicationClass
import infix.imrankst1221.codecanyon.R
import infix.imrankst1221.codecanyon.databinding.ActivitySplashBinding
import infix.imrankst1221.codecanyon.ui.home.HomeActivity
import infix.imrankst1221.rocket.library.setting.ThemeBaseActivity
import infix.imrankst1221.rocket.library.utility.PreferenceUtils
import infix.imrankst1221.rocket.library.utility.Constants
import infix.imrankst1221.rocket.library.utility.UtilMethods

class SplashActivity : ThemeBaseActivity(), ApplicationClass.OnOpenAdListener {
    lateinit var mBinding: ActivitySplashBinding
    private var splashScreenDelay = 5L
    private var isLoaderFinish = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mContext = this
        initView()

        Handler(Looper.getMainLooper()).postDelayed({
            isLoaderFinish = true
            openAdsLogic()
        },   1000 * splashScreenDelay)
    }

    private fun initView(){
        when(PreferenceUtils.getStringValue(Constants.KEY_SPLASH_SCREEN_TYPE, "")){
            Constants.SPLASH_SCREEN_TYPE.STANDER.name -> {
                mBinding.layoutWelcome.visibility = View.VISIBLE
                mBinding.imageFull.visibility = View.GONE
                mBinding.txtSplashQuote.text = PreferenceUtils.getStringValue(Constants.KEY_SPLASH_QUOTE, "")
                mBinding.txtSplashFooter.text = PreferenceUtils.getStringValue(Constants.KEY_SPLASH_FOOTER, "")
            }
            Constants.SPLASH_SCREEN_TYPE.FULL_SCREEN.name -> {
                mBinding.layoutWelcome.visibility = View.GONE
                mBinding.imageFull.visibility = View.VISIBLE
            }
        }
        val gradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                        ContextCompat.getColor(mContext,UtilMethods.getThemePrimaryDarkColor())))
        gradientDrawable.cornerRadius = 0f
        mBinding.viewBackground.background = gradientDrawable
    }

    override fun onShowAdComplete() {
        if(isLoaderFinish){
            startMainScreen()
        }
    }

    override fun onAdsFailToLoad() {
        startMainScreen()
    }

    private fun openAdsLogic(){
        val adMobId = PreferenceUtils.getInstance().getStringValue(Constants.ADMOB_ID,"")!!
        if (adMobId.isNotEmpty() && resources.getString(R.string.admob_open_ads_unit_id).isNotEmpty()) {
            (application as ApplicationClass).showOpenAds(this)
        }else{
            startMainScreen()
        }
    }

    private fun startMainScreen() {
        mBinding.layoutSplash.visibility = View.GONE
        val intent = Intent(this, HomeActivity::class.java)
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        if(resources.getBoolean(R.bool.screen_rotation)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }else{
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

}
