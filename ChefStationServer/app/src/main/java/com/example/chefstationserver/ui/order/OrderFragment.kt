package com.example.chefstationserver.ui.order

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chefstationserver.Adapter.MyOrderAdapter
import com.example.chefstationserver.Adapter.MyShipperSelectedAdapter
import com.example.chefstationserver.CallBack.IMyButtonCallBack
import com.example.chefstationserver.CallBack.IShipperLoadCallbackListener
import com.example.chefstationserver.Common.BottomSheetOrderFragment
import com.example.chefstationserver.Common.Common
import com.example.chefstationserver.Common.MySwipeHelper
import com.example.chefstationserver.Eventbus.AddonSizeEditEvent
import com.example.chefstationserver.Eventbus.ChangeMenuClick
import com.example.chefstationserver.Eventbus.LoadOrderEvent
import com.example.chefstationserver.Model.*
import com.example.chefstationserver.R
import com.example.chefstationserver.Remote.IFCMService
import com.example.chefstationserver.Remote.RetrofitFCMClient
import com.example.chefstationserver.SizeAddonEditActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import dmax.dialog.SpotsDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_order.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.Exception
import java.lang.StringBuilder

class OrderFragment: Fragment(){

    private val compositeDisposable = CompositeDisposable()
    lateinit var  ifcmService: IFCMService
    lateinit var recycler_order:RecyclerView
    lateinit var layoutAnimationController:LayoutAnimationController
    lateinit var orderViewModel:OrderViewModel

    private var adapter : MyOrderAdapter?=null

    var myShipperSelectedAdapter:MyShipperSelectedAdapter ?= null
    lateinit var shipperLoadCallbackListener:IShipperLoadCallbackListener
    var recycler_shipper:RecyclerView?=null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_order,container,false)
        initViews(root)

        orderViewModel = ViewModelProviders.of(this).get(OrderViewModel::class.java)

        orderViewModel!!.messageError.observe(this, Observer { s->
            Toast.makeText(context,s,Toast.LENGTH_SHORT).show()
        })

        orderViewModel!!.getOrderModelList().observe(this, Observer { orderList->
            if(orderList != null){

                adapter = MyOrderAdapter(context!!,orderList.toMutableList())
                recycler_order.adapter = adapter
                recycler_order.layoutAnimation = layoutAnimationController

                updateTextCounter()
            }
        })

        return root
    }

    private fun initViews(root:View) {

        //shipperLoadCallbackListener = this

        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService::class.java)

        setHasOptionsMenu(true)

        recycler_order = root.findViewById(R.id.recycler_order) as RecyclerView
        recycler_order.setHasFixedSize(true)
        recycler_order.layoutManager = LinearLayoutManager(context)

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)

        val displayMetrics = DisplayMetrics()
        activity!!.windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        val width = displayMetrics.widthPixels

        val swipe = object: MySwipeHelper(context!!, recycler_order!!, width/6){
            override fun instantiateMyButton(
                    viewHolder: RecyclerView.ViewHolder,
                    buffer: MutableList<MyButton>
            ) {

                buffer.add(
                        MyButton(context!!,
                                "Ara",
                                30,
                                0,
                                Color.parseColor("#560027"),
                                object : IMyButtonCallBack {
                                    override fun onClick(pos: Int) {

                                        Dexter.withActivity(activity)
                                                .withPermission(android.Manifest.permission.CALL_PHONE)
                                                .withListener(object:PermissionListener{
                                                    override fun onPermissionGranted(response: PermissionGrantedResponse?) {

                                                        val orderModel = adapter!!.getItemAtPosition(pos)
                                                        val intent = Intent()
                                                        intent.setAction(Intent.ACTION_DIAL)
                                                        intent.setData(
                                                                Uri.parse(StringBuilder("tel: ")
                                                                        .append(orderModel.userPhone).toString())
                                                        )
                                                        startActivity(intent)

                                                    }

                                                    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                                                        Toast.makeText(context,"Bu izni kabul etmelisiniz "+response!!.permissionName,
                                                                Toast.LENGTH_SHORT).show()
                                                    }

                                                    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {

                                                    }


                                                }).check()

                                    }

                                })
                )

                buffer.add(
                        MyButton(context!!,
                                "Sil",
                                30,
                                0,
                                Color.parseColor("#035600"),
                                object : IMyButtonCallBack {
                                    override fun onClick(pos: Int) {

                                        val orderModel = adapter!!.getItemAtPosition(pos)

                                        val builder = AlertDialog.Builder(context!!)
                                                .setTitle("Sil")
                                                .setMessage("Bu siparişi gerçekten silmek istiyor musunuz?")
                                                .setNegativeButton("İPTAL"){dialogInterface, i -> dialogInterface.dismiss()}
                                                .setPositiveButton("SİL"){dialogInterface, i ->
                                                    FirebaseDatabase.getInstance()
                                                            .getReference(Common.ORDER_REF)
                                                            .child(orderModel!!.key!!)
                                                            .removeValue()
                                                            .addOnFailureListener {
                                                                Toast.makeText(context!!,""+it.message,Toast.LENGTH_SHORT).show()
                                                            }
                                                            .addOnSuccessListener {

                                                                adapter!!.removeItem(pos)
                                                                adapter!!.notifyItemRemoved(pos)
                                                                updateTextCounter()
                                                                dialogInterface.dismiss()
                                                                Toast.makeText(context!!,"Sipariş silindi!",Toast.LENGTH_SHORT).show()
                                                            }
                                                }

                                        val dialog = builder.create()
                                        dialog.show()

                                        val btn_negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
                                        btn_negative.setTextColor(Color.LTGRAY)
                                        val btn_positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                                        btn_negative.setTextColor(Color.RED)

                                    }

                                })
                )

                buffer.add(
                        MyButton(context!!,
                                "Düzenle",
                                30,
                                0,
                                Color.parseColor("#12005e"),
                                object : IMyButtonCallBack {
                                    override fun onClick(pos: Int) {

                                        showEditDialog(adapter!!.getItemAtPosition(pos),pos)
                                    }

                                })
                )
            }
        }

    }

    private fun showEditDialog(orderModel: OrderModel, pos: Int) {

        var rdi_shipping :RadioButton?=null
        var rdi_shipped :RadioButton?=null
        var rdi_cancelled:RadioButton?=null
        var rdi_delete:RadioButton?=null
        var rdi_restore_placed:RadioButton?=null

        var layout_dialog:View?=null
        var builder:AlertDialog.Builder?=null

        if(orderModel.orderStatus == -1){

            layout_dialog = LayoutInflater.from(context!!)
                            .inflate(R.layout.layout_dialog_cancelled,null)

            builder = AlertDialog.Builder(context!!)
                        .setView(layout_dialog)

            rdi_delete = layout_dialog.findViewById<View>(R.id.rdi_delete) as RadioButton
            rdi_restore_placed = layout_dialog.findViewById<View>(R.id.rdi_restore_placed) as RadioButton


        }
        else if(orderModel.orderStatus == 0){

            layout_dialog = LayoutInflater.from(context!!)
                .inflate(R.layout.layout_dialog_shipping,null)
            //recycler_shipper = layout_dialog.findViewById(R.id.recycler_shipper) as RecyclerView //// Sadece hazırlanıyor durumunda eklenecek ( status == 0)
            builder = AlertDialog.Builder(context!!,android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
                .setView(layout_dialog)

            rdi_shipping = layout_dialog.findViewById<View>(R.id.rdi_shipping) as RadioButton
            rdi_cancelled = layout_dialog.findViewById<View>(R.id.rdi_cancelled) as RadioButton
        }

        else{

            layout_dialog = LayoutInflater.from(context!!)
                .inflate(R.layout.layout_dialog_shipped,null)
            builder = AlertDialog.Builder(context!!)
                .setView(layout_dialog)

            rdi_shipped   = layout_dialog.findViewById<View>(R.id.rdi_shipped) as RadioButton
            rdi_cancelled = layout_dialog.findViewById<View>(R.id.rdi_cancelled) as RadioButton
        }
        // View

        val btn_ok = layout_dialog.findViewById<View>(R.id.btn_ok) as Button
        val btn_cancel = layout_dialog.findViewById<View>(R.id.btn_cancel) as Button

        val txt_status = layout_dialog.findViewById<View>(R.id.txt_status) as TextView

        // Set Data

        txt_status.setText(StringBuilder("Sipariş Durumu (")
            .append(Common.convertStatusToString(orderModel.orderStatus))
            .append(")"))

        // Diyalog Penceresi

        val dialog = builder.create()

       // if(orderModel.orderStatus == 0) // Hazırlanıyor
            //loadShipperList(pos,orderModel,dialog,btn_ok,btn_cancel,rdi_shipping,rdi_shipped,rdi_cancelled,rdi_delete,rdi_restore_placed)
      //  else
            showDialog(pos,orderModel,dialog,btn_ok,btn_cancel,rdi_shipping,rdi_shipped,rdi_cancelled,rdi_delete,rdi_restore_placed)

    }

    private fun loadShipperList(
        pos: Int,
        orderModel: OrderModel,
        dialog: AlertDialog,
        btnOk: Button,
        btnCancel: Button,
        rdiShipping: RadioButton?,
        rdiShipped: RadioButton?,
        rdiCancelled: RadioButton?,
        rdiDelete: RadioButton?,
        rdiRestorePlaced: RadioButton?
    ) {
            val tempList:MutableList<ShipperModel> = ArrayList()
        val shipperRef = FirebaseDatabase.getInstance().getReference(Common.SHIPPER_REF)
        val shipperActive = shipperRef.orderByChild("active").equalTo(true)
        shipperActive.addListenerForSingleValueEvent(object:ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {
                for(shipperSnapShot in p0.children){

                    val shipperModel = shipperSnapShot.getValue(ShipperModel::class.java)!!
                    shipperModel.key = shipperSnapShot.key
                    tempList.add(shipperModel)
                }
                shipperLoadCallbackListener.onShipperLoadSuccess(pos,orderModel,tempList,dialog,btnOk,btnCancel,
                rdiShipping,rdiShipped,rdiCancelled,rdiDelete,rdiRestorePlaced)
            }

            override fun onCancelled(p0: DatabaseError) {
                shipperLoadCallbackListener.onShipperLoadFailed(p0!!.message)
            }

        })
    }

    private fun showDialog(
        pos: Int,
        orderModel: OrderModel,
        dialog: AlertDialog,
        btnOk: Button,
        btnCancel: Button,
        rdiShipping: RadioButton?,
        rdiShipped: RadioButton?,
        rdiCancelled: RadioButton?,
        rdiDelete: RadioButton?,
        rdiRestorePlaced: RadioButton?
    ) {

        dialog.show()
        // Pencereyi biçimlendirmek

        btnCancel.setOnClickListener{ dialog.dismiss() }
        btnOk.setOnClickListener{

            if(rdiCancelled != null && rdiCancelled.isChecked){

                updateOrder(pos,orderModel,-1)
                dialog.dismiss()
            }
            else if(rdiShipping != null && rdiShipping.isChecked){

                updateOrder(pos,orderModel,1)
                dialog.dismiss()

               /* var shipperModel:ShipperModel?=null
                if(myShipperSelectedAdapter != null){

                    shipperModel = myShipperSelectedAdapter!!.selectedShipper
                    if(shipperModel != null){

                       createShippingOrder(pos,shipperModel,orderModel,dialog)
                    }
                    else{
                        Toast.makeText(context,"Lütfen bir kurye seçiniz",Toast.LENGTH_SHORT).show()
                    }
                }*/
            }

            else if(rdiShipped != null && rdiShipped.isChecked){

                updateOrder(pos,orderModel,2)
                dialog.dismiss()
            }

            else if(rdiRestorePlaced != null && rdiRestorePlaced.isChecked){

                updateOrder(pos,orderModel,0)
                dialog.dismiss()
            }

            else if(rdiDelete != null && rdiDelete.isChecked){

                deleteOrder(pos,orderModel)
                dialog.dismiss()
            }

        }

    }

    private fun createShippingOrder(
        pos:Int,
        shipperModel: ShipperModel,
        orderModel: OrderModel,
        dialog: AlertDialog
    ) {
        val shippingOrder = ShippingOrderModel()
        shippingOrder.shipperName = shipperModel.name
        shippingOrder.shipperPhone = shipperModel.phone
        shippingOrder.orderModel = orderModel
        shippingOrder.isStartTrip = false
        shippingOrder.currentLat = -1.0
        shippingOrder.currentLng = -1.0
        FirebaseDatabase.getInstance()
            .getReference(Common.SHIPPING_ORDER_REF)
            .push()
            .setValue(shippingOrder)
            .addOnFailureListener { e:Exception ->
                dialog.dismiss()
                Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener { task: Task<Void?> ->
                if(task.isSuccessful){

                    dialog.dismiss()

                    // Token yükleme

                    FirebaseDatabase.getInstance()
                            .getReference(Common.TOKEN_REF)
                            .child(shipperModel.key!!)
                            .addListenerForSingleValueEvent(object:ValueEventListener{
                                override fun onDataChange(p0: DataSnapshot) {
                                    if(p0.exists()){

                                        val tokenModel = p0.getValue(TokenModel::class.java)
                                        val notiData = HashMap<String,String>()
                                        notiData.put(Common.NOTI_TITLE,"Gönderilmeyi bekleyen bir siparişiniz var")
                                        notiData.put(Common.NOTI_CONTENT,StringBuilder(orderModel.userPhone)
                                                .append(" numaralı müşteriye gönderilmeyi bekleyen bir siparişiniz var ").toString())

                                        val sendData = FCMSendData(tokenModel!!.token!!,notiData)

                                        compositeDisposable.add(
                                                ifcmService.sendNotification(sendData)
                                                        .subscribeOn(Schedulers.io())
                                                        .observeOn(AndroidSchedulers.mainThread())
                                                        .subscribe({ fcmResponse ->
                                                            dialog.dismiss()
                                                            if(fcmResponse.success == 1){

                                                                updateOrder(pos,orderModel,1)
                                                            }
                                                            else{

                                                                Toast.makeText(context!!,"Bildirim gönderimi başarısız oldu ! Sipariş güncellenemedi",Toast.LENGTH_SHORT).show()
                                                            }

                                                        },{ t->
                                                            dialog.dismiss()
                                                            Toast.makeText(context!!,""+t.message,Toast.LENGTH_SHORT).show()
                                                        })
                                        )
                                    }
                                    else{
                                        dialog.dismiss()
                                        Toast.makeText(context,"Token bulunamadı",Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onCancelled(p0: DatabaseError) {
                                    dialog.dismiss()
                                    Toast.makeText(context,""+p0.message,Toast.LENGTH_SHORT).show()
                                }

                            })

                }
            }

    }

    private fun deleteOrder(pos: Int, orderModel: OrderModel) {

        if(!TextUtils.isEmpty(orderModel.key)){

            FirebaseDatabase.getInstance()
                .getReference(Common.ORDER_REF)
                .child(orderModel.key!!)
                .removeValue()
                .addOnFailureListener { throwable -> Toast.makeText(context!!,""+throwable.message,Toast.LENGTH_SHORT).show() }
                .addOnSuccessListener {

                    adapter!!.removeItem(pos)
                    adapter!!.notifyItemRemoved(pos)
                    updateTextCounter()
                    Toast.makeText(context!!,"Sipariş Güncelleme Başarılı!",Toast.LENGTH_SHORT).show()
                }
        }

        else{

            Toast.makeText(context!!,"Sipariş numarası boş olamaz",Toast.LENGTH_SHORT).show()
        }

    }

    private fun updateOrder(pos: Int, orderModel: OrderModel, status: Int) {
        if(!TextUtils.isEmpty(orderModel.key)){

            val update_data = HashMap<String,Any>()
            update_data.put("orderStatus",status)

            FirebaseDatabase.getInstance()
                .getReference(Common.ORDER_REF)
                .child(orderModel.key!!)
                .updateChildren(update_data)
                .addOnFailureListener { throwable -> Toast.makeText(context!!,""+throwable.message,Toast.LENGTH_SHORT).show() }
                .addOnSuccessListener {

                    val dialog = SpotsDialog.Builder().setContext(context!!).setCancelable(false).build()
                    dialog.show()

                    // Token yükleme

                    FirebaseDatabase.getInstance()
                        .getReference(Common.TOKEN_REF)
                        .child(orderModel.userId!!)
                        .addListenerForSingleValueEvent(object:ValueEventListener{
                            override fun onDataChange(p0: DataSnapshot) {
                                if(p0.exists()){

                                    val tokenModel = p0.getValue(TokenModel::class.java)
                                    val notiData = HashMap<String,String>()
                                    notiData.put(Common.NOTI_TITLE,"Siparişiniz güncellendi")
                                    notiData.put(Common.NOTI_CONTENT,StringBuilder(orderModel.key)
                                        .append(" numaralı siparişiniz ")
                                        .append(Common.convertStatusToString(status))
                                        .append(" durumuna güncellendi").toString())

                                    val sendData = FCMSendData(tokenModel!!.token!!,notiData)

                                    compositeDisposable.add(
                                        ifcmService.sendNotification(sendData)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe({ fcmResponse ->
                                                dialog.dismiss()
                                                if(fcmResponse.success == 1){

                                                    Toast.makeText(context!!,"Sipariş Güncelleme Başarılı",Toast.LENGTH_SHORT).show()
                                                }
                                                else{

                                                    Toast.makeText(context!!,"Bildirim gönderimi başarısız oldu",Toast.LENGTH_SHORT).show()
                                                }

                                            },{ t->
                                                dialog.dismiss()
                                                Toast.makeText(context!!,""+t.message,Toast.LENGTH_SHORT).show()
                                            })
                                    )
                                }
                                else{
                                    dialog.dismiss()
                                    Toast.makeText(context,"Token bulunamadı",Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onCancelled(p0: DatabaseError) {
                                dialog.dismiss()
                                Toast.makeText(context,""+p0.message,Toast.LENGTH_SHORT).show()
                            }

                        })

                    adapter!!.removeItem(pos)
                    adapter!!.notifyItemRemoved(pos)
                    updateTextCounter()
                }
        }
        else{

            Toast.makeText(context!!,"Sipariş numarası boş olamaz",Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTextCounter() {

        txt_order_filter.setText(StringBuilder("Siparişler (")
            .append(adapter!!.itemCount)
            .append(")"))

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.order_list_menu,menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if(item.itemId == R.id.action_filter){

            val bottomSheet = BottomSheetOrderFragment.instance
            bottomSheet!!.show(activity!!.supportFragmentManager,"OrderList")
            return true
        }
        else{
            return super.onOptionsItemSelected(item)
        }

    }

    override fun onStart() {
        super.onStart()
        if(!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    override fun onStop() {

        if(EventBus.getDefault().hasSubscriberForEvent(LoadOrderEvent::class.java))
            EventBus.getDefault().removeStickyEvent(LoadOrderEvent::class.java)
        if(EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this)

        compositeDisposable.clear()
        super.onStop()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(ChangeMenuClick(true))
        super.onDestroy()
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onLoadOrder(event:LoadOrderEvent){

        orderViewModel.loadOrder(event.status)
    }

   /* override fun onShipperLoadSuccess(shipperList: List<ShipperModel>) {
        //Hiçbir şey yapılmayacak
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
        if(recycler_shipper != null){

            recycler_shipper!!.setHasFixedSize(true)
            val layoutManager = LinearLayoutManager(context)
            recycler_shipper!!.layoutManager = layoutManager
            recycler_shipper!!.addItemDecoration(DividerItemDecoration(context,layoutManager.orientation))
            myShipperSelectedAdapter = MyShipperSelectedAdapter(context!!,shipperList!!)
            recycler_shipper!!.adapter = myShipperSelectedAdapter
        }

        showDialog(pos,orderModel!!,dialog!!,ok!!,cancel!!,rdi_shipping,rdi_shipped,rdi_cancelled,rdi_delete,rdi_restore_placed)
    }

    override fun onShipperLoadFailed(message: String) {
        Toast.makeText(context!!,message,Toast.LENGTH_SHORT).show()
    }*/
}