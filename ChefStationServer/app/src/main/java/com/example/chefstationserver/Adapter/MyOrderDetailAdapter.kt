package com.example.chefstationserver.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chefstationserver.Eventbus.AddonSizeEditEvent
import com.example.chefstationserver.Model.AddonModel
import com.example.chefstationserver.Model.CartItem
import com.example.chefstationserver.Model.SizeModel
import com.example.chefstationserver.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MyOrderDetailAdapter(internal var context: Context,
                           internal var cartItemList:MutableList<CartItem>):RecyclerView.Adapter<MyOrderDetailAdapter.MyViewHolder>() {

    val gson:Gson = Gson()

    class MyViewHolder(itemView:View):RecyclerView.ViewHolder(itemView){

        var txt_food_name:TextView?=null
        var txt_food_size:TextView?=null
        var txt_food_addon:TextView?=null
        var txt_food_quantity:TextView?=null
        var img_food_image:ImageView?=null

        init {

            img_food_image = itemView.findViewById(R.id.img_food_image) as ImageView
            txt_food_name = itemView.findViewById(R.id.txt_food_name) as TextView
            txt_food_size = itemView.findViewById(R.id.txt_size) as TextView
            txt_food_quantity = itemView.findViewById(R.id.txt_food_quantity) as TextView
            txt_food_addon = itemView.findViewById(R.id.txt_food_add_on) as TextView
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.layout_order_detail_item,parent,false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        Glide.with(context).load(cartItemList[position].foodImage)
                .into(holder.img_food_image!!)
        holder.txt_food_name!!.setText(StringBuilder().append(cartItemList[position].foodName))
        holder.txt_food_quantity!!.setText(StringBuilder("Adet: ").append(cartItemList[position].foodQuantity))

        val sizeModel:SizeModel = gson.fromJson(cartItemList[position].foodSize, object:TypeToken<SizeModel?>(){}.type)
        if(sizeModel != null) holder.txt_food_size!!.setText(StringBuilder("Porsiyon: ").append(sizeModel.name))
        if(!cartItemList[position].foodAddon.equals("Default")){

            val addonModels:List<AddonModel> = gson.fromJson(cartItemList[position].foodAddon,object:TypeToken<List<AddonModel?>?>(){}.type)
            val addonString = StringBuilder()
            if(addonModels != null){

                for(addonModel in addonModels) addonString.append(addonModel.name).append(",")
                addonString.delete(addonString.length-1,addonString.length) // Son virg??l?? kald??rmak i??in
                holder.txt_food_addon!!.setText(StringBuilder("Eklenti: ").append(addonString))
            }
        }
        else{

            holder.txt_food_addon!!.setText(StringBuilder("Eklenti: Varsay??lan"))
        }

    }

    override fun getItemCount(): Int {
        return cartItemList.size
    }
}