package com.example.chefstationserver.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chefstationserver.CallBack.IRecyclerItemClickListener
import com.example.chefstationserver.R
import com.example.chefstationserver.Common.Common
import com.example.chefstationserver.Model.FoodModel
import com.example.chefstationserver.model.BestDealsModel
import com.example.chefstationserver.model.MostPopularModel
import com.google.firebase.database.FirebaseDatabase
import net.cachapa.expandablelayout.ExpandableLayout

class MyFoodListAdapter (internal var context: Context,
                         internal var foodList:List<FoodModel>) :
    RecyclerView.Adapter<MyFoodListAdapter.MyViewHolder>(){

    var lastExpandable:ExpandableLayout?=null

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(foodList.get(position).image).into(holder.img_food_image!!)
        holder.txt_food_name!!.setText(foodList.get(position).name)
        holder.txt_food_price!!.setText(StringBuilder("$").append(foodList.get(position).price.toString()))

        //Event

        holder.setListener(object: IRecyclerItemClickListener {
            override fun onItemClick(view: View, pos: Int) {
                Common.foodSelected = foodList.get(pos)
                Common.foodSelected!!.key = pos.toString()

                if(lastExpandable != null && lastExpandable!!.isExpanded)
                    lastExpandable!!.collapse()

                if(!holder.expandable_layout!!.isExpanded){

                    holder.expandable_layout!!.isSelected = true
                    holder.expandable_layout!!.expand()
                }
                else{

                    holder.expandable_layout!!.isSelected = false
                    holder.expandable_layout!!.collapse()
                }
                lastExpandable = holder.expandable_layout
            }

        })

        holder.btn_best_deal!!.setOnClickListener{
            makeFoodBestDeal(foodList[position])
        }
        holder.btn_most_popular!!.setOnClickListener{
            makeFoodMostPopular(foodList[position])
        }

    }

    private fun makeFoodBestDeal(foodModel: FoodModel) {

        val bestDeal = BestDealsModel()
        bestDeal.food_id = foodModel!!.id!!
        bestDeal.image = foodModel!!.image!!
        bestDeal.menu_id = Common.categorySelected!!.menu_id!!
        bestDeal.name = foodModel!!.name!!

        FirebaseDatabase.getInstance()
            .getReference(Common.BEST_DEALS)
            .child(java.lang.StringBuilder(bestDeal.menu_id).append("_").append(bestDeal.food_id).toString())
            .setValue(bestDeal)
            .addOnFailureListener{ e-> Toast.makeText(context,e.message!!,Toast.LENGTH_SHORT).show()}
            .addOnSuccessListener {
                Toast.makeText(context,"Başarıyla En İyi Fiyatlara Eklendi",Toast.LENGTH_SHORT).show()
            }
    }

    private fun makeFoodMostPopular(foodModel: FoodModel) {

        val mostPopular = MostPopularModel()
        mostPopular.food_id = foodModel!!.id!!
        mostPopular.image = foodModel!!.image!!
        mostPopular.menu_id = Common.categorySelected!!.menu_id!!
        mostPopular.name = foodModel!!.name!!

        FirebaseDatabase.getInstance()
            .getReference(Common.MOST_POPULARS)
            .child(java.lang.StringBuilder(mostPopular.menu_id).append("_").append(mostPopular.food_id).toString())
            .setValue(mostPopular)
            .addOnFailureListener{ e-> Toast.makeText(context,e.message!!,Toast.LENGTH_SHORT).show()}
            .addOnSuccessListener {
                Toast.makeText(context,"Başarıyla En Popüler Yemeklere Eklendi",Toast.LENGTH_SHORT).show()
            }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_food_item,parent,false))
    }

    override fun getItemCount(): Int {
        return foodList.size
    }

    fun getItemAtPosition(pos: Int): FoodModel {

        return foodList.get(pos)
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        var txt_food_name: TextView? = null
        var txt_food_price: TextView? = null
        var img_food_image: ImageView? = null

        var expandable_layout:ExpandableLayout?=null
        var btn_best_deal:Button?=null
        var btn_most_popular:Button?=null

        internal var listener:IRecyclerItemClickListener?=null

        fun setListener(listener: IRecyclerItemClickListener){

            this.listener = listener
        }

        init {

            txt_food_name = itemView.findViewById(R.id.txt_food_name) as TextView
            txt_food_price = itemView.findViewById(R.id.txt_food_price) as TextView
            img_food_image = itemView.findViewById(R.id.img_food_image) as ImageView
            expandable_layout = itemView.findViewById(R.id.expandable_layout) as ExpandableLayout
            btn_best_deal = itemView.findViewById(R.id.btn_best_deal) as Button
            btn_most_popular = itemView.findViewById(R.id.btn_most_popular) as Button

            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {

            listener!!.onItemClick(view!!,adapterPosition)
        }

    }

}