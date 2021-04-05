package com.example.chefstationserver.ui.best_deals

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chefstationserver.CallBack.IBestDealCallbackListener
import com.example.chefstationserver.Common.Common
import com.example.chefstationserver.Model.CategoryModel
import com.example.chefstationserver.model.BestDealsModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BestDealsViewModel : ViewModel(), IBestDealCallbackListener {

    private  var bestDealsListMutable : MutableLiveData<List<BestDealsModel>>?=null
    private  var messageError : MutableLiveData<String> = MutableLiveData()
    private  var bestDealsCallBackListener : IBestDealCallbackListener

    init{
        bestDealsCallBackListener = this
    }

    fun getBestDealsList() :MutableLiveData<List<BestDealsModel>>{

        if(bestDealsListMutable == null){

            bestDealsListMutable = MutableLiveData()
            loadBestDeals()
        }

        return bestDealsListMutable!!
    }

    fun loadBestDeals() {
        val tempList = ArrayList<BestDealsModel>()
        val bestDealsRef = FirebaseDatabase.getInstance().getReference(Common.BEST_DEALS)
        bestDealsRef.addListenerForSingleValueEvent(object: ValueEventListener {

            override fun onCancelled(p0: DatabaseError) {
                bestDealsCallBackListener.onListBestDealsLoadFailed((p0.message))
            }

            override fun onDataChange(p0: DataSnapshot) {

                for(itemSnapShot in p0.children)
                {

                    val model = itemSnapShot.getValue<BestDealsModel>(BestDealsModel::class.java)
                    model!!.key = itemSnapShot.key!!
                    tempList.add(model)
                }

                bestDealsCallBackListener.onListBestDealsLoadSuccess(tempList)
            }



        })
    }

    fun getMessageError():MutableLiveData<String>{

        return messageError
    }

    override fun onListBestDealsLoadSuccess(bestDealsModels: List<BestDealsModel>) {
        bestDealsListMutable!!.value = bestDealsModels
    }

    override fun onListBestDealsLoadFailed(message: String) {
        messageError.value = message
    }
}