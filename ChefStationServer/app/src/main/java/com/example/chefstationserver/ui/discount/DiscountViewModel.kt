package com.example.chefstationserver.ui.discount

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chefstationserver.CallBack.IDiscountCallbackListener
import com.example.chefstationserver.Common.Common
import com.example.chefstationserver.model.DiscountModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DiscountViewModel : ViewModel(), IDiscountCallbackListener {

    val messageError = MutableLiveData<String>()
    private var discountMutableLiveData:MutableLiveData<List<DiscountModel>?>? = null
    private var discountCallbackListener:IDiscountCallbackListener

    init{

        discountCallbackListener = this
    }

    fun getDiscountMutableLiveData():MutableLiveData<List<DiscountModel>?>{

        if(discountMutableLiveData == null)

            discountMutableLiveData = MutableLiveData<List<DiscountModel>?>()

        loadDiscount()

        return discountMutableLiveData!!
    }

    fun loadDiscount() {

        val temp:MutableList<DiscountModel> = ArrayList<DiscountModel>()

        val discountRef = FirebaseDatabase.getInstance()
                .getReference(Common.DISCOUNT)
        discountRef.addListenerForSingleValueEvent(object:ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {

                if(snapshot.children.iterator().hasNext()){

                    for(discountSnapshot in snapshot.children){

                        val discountModel = discountSnapshot.getValue(DiscountModel::class.java)
                        discountModel!!.key = discountSnapshot.key
                        temp.add(discountModel!!)
                    }
                    discountCallbackListener.onDiscountLoadSuccess(temp)
                }
                else
                    discountCallbackListener.onDiscountLoadFailed("Boş veri")
            }

            override fun onCancelled(error: DatabaseError) {
                discountCallbackListener.onDiscountLoadFailed(error.message)
            }


        })
    }

    override fun onDiscountLoadSuccess(discountList: List<DiscountModel>) {
        discountMutableLiveData!!.value = discountList
    }

    override fun onDiscountLoadFailed(message: String) {

        if(message.equals("Boş veri"))
            discountMutableLiveData!!.value = null
        //messageError.value = message
    }

}