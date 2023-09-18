package infix.imrankst1221.codecanyon.setting

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import infix.imrankst1221.codecanyon.R
import infix.imrankst1221.codecanyon.ui.home.HomeActivity
import infix.imrankst1221.rocket.library.utility.UtilMethods

class ThemeConfig(){
    lateinit var mContext: Context
    lateinit var mActivity: HomeActivity

    constructor(context: Context, activity: HomeActivity) : this() {
        this.mContext = context
        this.mActivity = activity
    }

    fun initThemeColor(){
        mActivity.mBinding.layoutToolbar.background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                        ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

        mActivity.mBinding.navigationView.getHeaderView(0).background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                        ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

        // radius for button
        val buttonGradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                        ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor())))

        buttonGradientDrawable.cornerRadius = 20f
        mActivity.mBinding.btnTryAgain.background = buttonGradientDrawable
        mActivity.mBinding.btnErrorHome.background = buttonGradientDrawable
        mActivity.mBinding.btnErrorTryAgain.background = buttonGradientDrawable

        // set other view color
        mActivity.mBinding.loaderLibrary.setColor(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()))
        mActivity.mBinding.imgNoInternet.setColorFilter(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()))
        mActivity.mBinding.txtNoInternetTitle.setTextColor(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor()))
        mActivity.mBinding.txtNoInternetDetail.setTextColor(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()))
        mActivity.mBinding.btnAdShow.setColorFilter(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()))

        mActivity.mBinding.swapView.setColorSchemeColors(ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryColor()),
                ContextCompat.getColor(mContext, UtilMethods.getThemePrimaryDarkColor()))
    }

    fun initThemeStyle(){
        UtilMethods.setLoaderStyle( mActivity.mBinding.loaderBackground,
                mActivity.mBinding.loaderDefault,
                mActivity.mBinding.loaderLibrary)
        UtilMethods.setNavigationBarStyle(mActivity.mBinding.layoutToolbar,
                mActivity.mBinding.txtToolbarTitle,
                mActivity.mBinding.txtToolbarImage)
        val isNavigationSlider = UtilMethods.setNavigationBarIcon(
                mActivity.mBinding.imgLeftMenu,
                mActivity.mBinding.imgRightMenu,
                mActivity.mBinding.imgLeftMenu1,
                mActivity.mBinding.imgRightMenu1,
                mActivity.mBinding.layoutToolbar,
                R.drawable.ic_menu,
                R.drawable.ic_reload,
                R.drawable.ic_share_toolbar,
                R.drawable.ic_home_toolbar ,
                R.drawable.ic_exit_toolbar,
                R.drawable.ic_arrow_left,
                R.drawable.ic_arrow_right
        )

        if (isNavigationSlider){
            mActivity.mBinding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            mActivity.mBinding.navigationView.setNavigationItemSelectedListener(mActivity)
            val toggle = ActionBarDrawerToggle(mActivity, mActivity.mBinding.drawerLayout, null, R.string.open_drawer, R.string.close_drawer)
            mActivity.mBinding.drawerLayout.addDrawerListener(toggle)
            toggle.syncState()
        }else{
            mActivity.mBinding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }
    }
}
