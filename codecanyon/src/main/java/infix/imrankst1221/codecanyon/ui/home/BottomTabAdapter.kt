package infix.imrankst1221.codecanyon.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import infix.imrankst1221.codecanyon.R
import infix.imrankst1221.rocket.library.data.NavigationTab
import infix.imrankst1221.rocket.library.setting.AppDataInstance

/**
 * @author imran.choudhury
 * 5/7/21
 */

interface OnBottomTabItemClick{
    fun itemClicked(position: Int)
}
class BottomTabAdapter(private val dataSet: ArrayList<NavigationTab>, private val listener: OnBottomTabItemClick) :
    RecyclerView.Adapter<BottomTabAdapter.ViewHolder>() {
    lateinit var mContext: Context
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtTab: TextView = view.findViewById(R.id.txtTab)
        val imgTab: AppCompatImageView = view.findViewById(R.id.imgTab)
        init {
            // Define click listener for the ViewHolder's View.
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.layout_bottom_tab, viewGroup, false)
        mContext = view.context
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val iconId = mContext.resources.getIdentifier(AppDataInstance.appConfig.navigationTab[position].icon, "drawable", mContext.packageName)
        if (dataSet[position].name.isEmpty()){
            viewHolder.txtTab.visibility = View.GONE
        }else{
            viewHolder.txtTab.visibility = View.VISIBLE
        }
        viewHolder.txtTab.text = dataSet[position].name
        viewHolder.imgTab.setImageDrawable(ContextCompat.getDrawable(mContext, iconId))
        viewHolder.itemView.setOnClickListener {
            listener.itemClicked(position)
        }
    }

    override fun getItemCount() = dataSet.size

}