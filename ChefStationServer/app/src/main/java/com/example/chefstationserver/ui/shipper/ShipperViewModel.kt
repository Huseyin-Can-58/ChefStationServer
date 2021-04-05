package com.example.chefstationserver.ui.shipper

import android.widget.Button
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chefstationserver.CallBack.ICategoryCallbackListener
import com.example.chefstationserver.CallBack.IShipperLoadCallbackListener
import com.example.chefstationserver.Common.Common
import com.example.chefstationserver.Model.CategoryModel
import com.example.chefstationserver.Model.OrderModel
import com.example.chefstationserver.Model.ShipperModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ShipperViewModel : ViewModel(), IShipperLoadCallbackListener {

    private  var shipperListMutable : MutableLiveData<List<ShipperModel>>?=null
    private  var messageError : MutableLiveData<String> = MutableLiveData()
    private  var shipperCallBackListener : IShipperLoadCallbackListener

    init{
        shipperCallBackListener = this
    }

    fun getShipperList() :MutableLiveData<List<ShipperModel>>{

        if(shipperListMutable == null){

            shipperListMutable = MutableLiveData()
            loadShipper()
        }

        return shipperListMutable!!
    }

    fun loadShipper() {
        val tempList = ArrayList<ShipperModel>()
        val shipperRef = FirebaseDatabase.getInstance().getReference(Common.SHIPPER_REF)
        shipperRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                for(itemSnapShot in p0.children)
                {

                    val model = itemSnapShot.getValue<ShipperModel>(ShipperModel::class.java)
                    model!!.key = itemSnapShot.key
                    tempList.add(model)
                }

                shipperCallBackListener.onShipperLoadSuccess(tempList)
            }

            override fun onCancelled(p0: DatabaseError) {
                shipperCallBackListener.onShipperLoadFailed((p0.message))
            }


        })
    }

    fun getMessageError():MutableLiveData<String>{

        return messageError
    }

    override fun onShipperLoadSuccess(shipperList: List<ShipperModel>) {
        shipperListMutable!!.value = shipperList
    }

    override fun onShipperLoadSuccess(
        pos: Int,
        orderModel: OrderModel?,
        shipperList: List<ShipperModel>?,
        dialog: AlertDialog?,
        ok: Button?,
        cancel: Button?,
        rdi_shipping: RadioButton?,
        rdi_shipped: RadioButton?,
        rdi_cancelled: RadioButton?,
        rdi_delete: RadioButton?,
        rdi_restore_placed: RadioButton?
    ) {
        // Hiçbir şey yapılmayacak
    }

    override fun onShipperLoadFailed(message: String) {
        messageError.value = message
    }

}