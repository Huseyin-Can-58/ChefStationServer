package com.example.chefstationserver.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chefstationserver.CallBack.IRecyclerItemClickListener
import com.example.chefstationserver.Common.Common
import com.example.chefstationserver.Eventbus.CategoryClick
import com.example.chefstationserver.R
import com.example.chefstationserver.model.BestDealsModel
import com.example.chefstationserver.model.MostPopularModel
import org.greenrobot.eventbus.EventBus

class MyMostPopularsAdapter (internal var context: Context,
                             internal var mostPopularList:List<MostPopularModel>) :
    RecyclerView.Adapter<MyMostPopularsAdapter.MyViewHolder>(){

    override fun onBindViewHolder(holder: MyMostPopularsAdapter.MyViewHolder, position: Int) {
        Glide.with(context).load(mostPopularList.get(position).image).into(holder.category_image!!)
        holder.category_name!!.setText(mostPopularList.get(position).name)

        //Event

        holder.setListener(object: IRecyclerItemClickListener {
            override fun onItemClick(view: View, pos: Int) {

            }
        })
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyMostPopularsAdapter.MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_category_item,parent,false))
    }

    override fun getItemCount(): Int {
        return mostPopularList.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        var category_name: TextView? = null
        var category_image: ImageView? = null

        internal var listener:IRecyclerItemClickListener?=null

        fun setListener(listener: IRecyclerItemClickListener){

            this.listener = listener
        }

        init {

            category_name = itemView.findViewById(R.id.category_name) as TextView
            category_image = itemView.findViewById(R.id.category_image) as ImageView
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {

            listener!!.onItemClick(view!!,adapterPosition)

        }

    }
}