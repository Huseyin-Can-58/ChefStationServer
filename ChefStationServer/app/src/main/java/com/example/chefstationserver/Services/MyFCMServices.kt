package com.example.chefstationserver.Services

import android.content.Intent
import android.util.Log
import com.example.chefstationserver.Common.Common
import com.example.chefstationserver.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.*

class MyFCMServices : FirebaseMessagingService() {

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        Common.updateToken(this,p0,true,false) // Server uygulamasında çalışıldığı için Shipper = false
    }

    override fun onMessageReceived(remotemessage: RemoteMessage) {
        super.onMessageReceived(remotemessage)
        val dataRecv = remotemessage.data
        Log.d("HCY",dataRecv.toString())
        if(dataRecv != null){

            if(dataRecv[Common.NOTI_TITLE]!!.equals("Yeni Sipariş")){

                //MainActivity çağırılmalıdır
                //currentuser bilgisine ihtiyaç duyuluyor

                val intent = Intent(this,MainActivity::class.java)
                intent.putExtra(Common.IS_OPEN_ACTIVITY_NEW_ORDER,true)
                Common.showNotification(this, Random().nextInt(),
                        dataRecv[Common.NOTI_TITLE],
                        dataRecv[Common.NOTI_CONTENT],
                        intent)
            }
            else
                Common.showNotification(this, Random().nextInt(),
                dataRecv[Common.NOTI_TITLE],
                dataRecv[Common.NOTI_CONTENT],
                null)
        }
    }
}