package com.example.chefstationserver.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chefstationserver.R
import com.example.chefstationserver.model.DiscountModel
import java.lang.StringBuilder
import java.text.SimpleDateFormat

class MyDiscountAdapter(var context:Context,var discountModelList:MutableList<DiscountModel>) :
        RecyclerView.Adapter<MyDiscountAdapter.MyViewHolder>(){

    var simpleDateFormat:SimpleDateFormat

    init {

        simpleDateFormat = SimpleDateFormat("dd/MM/yyyy")
    }

    inner class MyViewHolder(itemView:View):RecyclerView.ViewHolder(itemView){

                var txt_code:TextView?=null
                var txt_percent:TextView?=null
                var txt_valid:TextView?=null

                init {

                    txt_code = itemView.findViewById(R.id.txt_code) as TextView
                    txt_percent = itemView.findViewById(R.id.txt_percent) as TextView
                    txt_valid = itemView.findViewById(R.id.txt_valid) as TextView
                }
            }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_discount_item,parent,false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        holder.txt_code!!.setText(StringBuilder("Kod: ").append(discountModelList[position].key))
        holder.txt_percent!!.setText(StringBuilder("Yüzde: ").append(discountModelList[position].percent).append("%"))
        holder.txt_valid!!.setText(StringBuilder("Son Geçerlilik: ").append(simpleDateFormat.format(discountModelList[position].untilDate)))
    }

    override fun getItemCount(): Int {
        return discountModelList.size
    }
}