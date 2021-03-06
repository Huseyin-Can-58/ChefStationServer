package com.example.chefstationserver

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import com.example.chefstationserver.Common.Common
import com.example.chefstationserver.Eventbus.CategoryClick
import com.example.chefstationserver.Eventbus.ChangeMenuClick
import com.example.chefstationserver.Eventbus.ToastEvent
import com.example.chefstationserver.Model.FCMResponse
import com.example.chefstationserver.Model.FCMSendData
import com.example.chefstationserver.Remote.IFCMService
import com.example.chefstationserver.Remote.RetrofitFCMClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.layout_news_system.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.HashMap

class HomeActivity : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 7171
    private var menuClick: Int = -1
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout:DrawerLayout
    private lateinit var navController: NavController
    private lateinit var navView:NavigationView

    private var img_upload:ImageView?=null
    private val compositeDisposable = CompositeDisposable()
    private lateinit var ifcmService: IFCMService
    private var imgUri: Uri?=null
    private lateinit var storage:FirebaseStorage
    private var storageReference:StorageReference?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService::class.java)
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference
        subscribeToTopic(Common.getNewOrderTopic())
        updateToken()

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_category, R.id.nav_food_list, R.id.nav_order,R.id.nav_best_deals,R.id.nav_most_populars
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navView.setNavigationItemSelectedListener(object:NavigationView.OnNavigationItemSelectedListener{
            override fun onNavigationItemSelected(p0: MenuItem): Boolean {

                p0.isChecked = true
                drawerLayout!!.closeDrawers()

                if(p0.itemId == R.id.nav_sign_out){
                    signOut()
                }
                else if(p0.itemId == R.id.nav_category){
                    if(menuClick != p0.itemId){

                        navController.popBackStack()            // Stack'i temizleme
                        navController.navigate(R.id.nav_category)
                    }

                }
                else if(p0.itemId == R.id.nav_order){
                    if(menuClick != p0.itemId){

                        navController.popBackStack()
                        navController.navigate(R.id.nav_order)
                    }

                }
                else if(p0.itemId == R.id.nav_best_deals){
                    if(menuClick != p0.itemId){

                        navController.popBackStack()
                        navController.navigate(R.id.nav_best_deals)
                    }

                }
                else if(p0.itemId == R.id.nav_most_populars){
                    if(menuClick != p0.itemId){

                        navController.popBackStack()
                        navController.navigate(R.id.nav_most_populars)
                    }

                }
                else if(p0.itemId == R.id.nav_discount){
                    if(menuClick != p0.itemId){

                        navController.popBackStack()
                        navController.navigate(R.id.nav_discount)
                    }

                }
                else if(p0.itemId == R.id.nav_news){

                    showSendNewsDialog()
                }
                /*else if(p0.itemId == R.id.nav_shipper){
                    if(menuClick != p0.itemId){

                        navController.popBackStack()
                        navController.navigate(R.id.nav_shipper)
                    }

                }*/

                menuClick = p0!!.itemId

                return true
            }

        })

        // View

        val headerView = navView.getHeaderView(0)
        val txt_user = headerView.findViewById<View>(R.id.txt_user) as TextView
        Common.setSpanString("Merhaba ",Common.currentServerUser!!.name,txt_user)

        menuClick = R.id.nav_category   // Varsay??lan

        checkOpenOrderFragment()
    }

    private fun showSendNewsDialog() {

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Haber Sistemi")
                .setMessage("B??t??n m????terilere haber g??nder")

        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_news_system,null)

        //Views

        val edt_title = itemView.findViewById<View>(R.id.edt_title) as EditText
        val edt_content = itemView.findViewById<View>(R.id.edt_content) as EditText
        val edt_link = itemView.findViewById<View>(R.id.edt_link) as EditText

        val rdi_none = itemView.findViewById<View>(R.id.rdi_none) as RadioButton
        val rdi_link = itemView.findViewById<View>(R.id.rdi_link) as RadioButton
        val rdi_upload = itemView.findViewById<View>(R.id.rdi_image) as RadioButton

        img_upload = itemView.findViewById(R.id.img_upload) as ImageView

        //Event

        rdi_none.setOnClickListener{
            edt_link.visibility = View.GONE
            img_upload!!.visibility = View.GONE
        }
        rdi_link.setOnClickListener{
            edt_link.visibility = View.VISIBLE
            img_upload!!.visibility = View.GONE
        }
        rdi_upload.setOnClickListener{
            edt_link.visibility = View.GONE
            img_upload!!.visibility = View.VISIBLE
        }
        img_upload!!.setOnClickListener{

            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent,"Foto??raf Se??iniz"),PICK_IMAGE_REQUEST)
        }

        builder.setView(itemView)
        builder.setNegativeButton("??PTAL",{dialogInterface, i -> dialogInterface.dismiss()})
        builder.setPositiveButton("G??NDER",{dialogInterface, i ->

            if(rdi_none.isChecked)
                sendNews(edt_title.text.toString(),edt_content.text.toString())

            else if(rdi_link.isChecked)
                sendNews(edt_title.text.toString(),edt_content.text.toString(),edt_link.text.toString())

            else if(rdi_upload.isChecked){

                if(imgUri != null){

                    val dialog = AlertDialog.Builder(this).setMessage("Y??kleniyor...").create()
                    dialog.show()
                    val file_name = UUID.randomUUID().toString()
                    val newsImage = storageReference!!.child("news/$file_name")
                    newsImage.putFile(imgUri!!)
                            .addOnFailureListener{ e: Exception -> dialog.dismiss()
                            Toast.makeText(this,e.message,Toast.LENGTH_SHORT).show()}
                            .addOnSuccessListener { taskSnapshot ->
                                dialog.dismiss()
                                newsImage.downloadUrl.addOnSuccessListener { uri->
                                    sendNews(edt_title.text.toString(),edt_content.text.toString(),uri.toString())
                                }
                            }
                            .addOnProgressListener { taskSnapshot ->
                                val progress = Math.round(100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toDouble()
                                dialog.setMessage(StringBuilder("Y??kleniyor: $progress %"))
                            }
                }
            }
        })
        val dialog = builder.create()
        dialog.show()
    }

    private fun sendNews(title: String, content: String) {

        val notificationData: MutableMap<String,String> = HashMap()
        notificationData[Common.NOTI_TITLE] = title
        notificationData[Common.NOTI_CONTENT] = content
        notificationData[Common.IS_SEND_IMAGE] = "false"

        val fcmSendData = FCMSendData(Common.getNewsTopic(),notificationData)

        val dialog = AlertDialog.Builder(this).setMessage("Y??kleniyor...").create()
        dialog.show()
        compositeDisposable.addAll(ifcmService.sendNotification(fcmSendData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ t: FCMResponse? ->
                    dialog.dismiss()
                    if(t!!.message_id != 0L)
                        Toast.makeText(this@HomeActivity,"Haber g??nderildi",Toast.LENGTH_SHORT).show()
                    else
                        Toast.makeText(this@HomeActivity,"Haber g??nderimi ba??ar??s??z oldu!",Toast.LENGTH_SHORT).show()

                    },{t: Throwable? ->
                        dialog.dismiss()
                    Toast.makeText(this@HomeActivity,t!!.message,Toast.LENGTH_SHORT).show()
                }))
    }

    private fun sendNews(title: String, content: String, url: String) {
        val notificationData: MutableMap<String,String> = HashMap()
        notificationData[Common.NOTI_TITLE] = title
        notificationData[Common.NOTI_CONTENT] = content
        notificationData[Common.IS_SEND_IMAGE] = "true"
        notificationData[Common.IMAGE_URL] = url

        val fcmSendData = FCMSendData(Common.getNewsTopic(),notificationData)

        val dialog = AlertDialog.Builder(this).setMessage("Y??kleniyor...").create()
        dialog.show()
        compositeDisposable.addAll(ifcmService.sendNotification(fcmSendData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ t: FCMResponse? ->
                    dialog.dismiss()
                    if(t!!.message_id != 0L)
                        Toast.makeText(this@HomeActivity,"Haber g??nderildi",Toast.LENGTH_SHORT).show()
                    else
                        Toast.makeText(this@HomeActivity,"Haber g??nderimi ba??ar??s??z oldu!",Toast.LENGTH_SHORT).show()

                },{t: Throwable? ->
                    dialog.dismiss()
                    Toast.makeText(this@HomeActivity,t!!.message,Toast.LENGTH_SHORT).show()
                }))
    }

    private fun checkOpenOrderFragment() {

        val isOpenNewOrder = intent.extras!!.getBoolean(Common.IS_OPEN_ACTIVITY_NEW_ORDER,false)
        if(isOpenNewOrder){

            navController.popBackStack()
            navController.navigate(R.id.nav_order)
            menuClick = R.id.nav_order
        }
    }

    private fun updateToken() {
        FirebaseInstanceId.getInstance()
                .instanceId
                .addOnFailureListener { e -> Toast.makeText(this@HomeActivity,""+e.message,Toast.LENGTH_SHORT).show() }
                .addOnSuccessListener { instanceIdresult ->
                    Log.d("MYTOKEN",instanceIdresult.token)
                    Common.updateToken(this@HomeActivity,instanceIdresult.token,true,false)
                }
    }

    private fun subscribeToTopic(newOrderTopic: String) {
        FirebaseMessaging.getInstance()
            .subscribeToTopic(newOrderTopic)
            .addOnFailureListener { message -> Toast.makeText(this@HomeActivity,""+message.message,Toast.LENGTH_SHORT).show() }
            .addOnCompleteListener { task ->
                if(!task.isSuccessful)
                    Toast.makeText(this@HomeActivity,"Konu kayd?? ba??ar??s??z oldu!",Toast.LENGTH_SHORT).show()
            }
    }

    private fun signOut() {

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("????k????")
                .setMessage("Ger??ekten ????kmak istiyor musunuz?")
                .setNegativeButton("Hay??r",{dialogInterface, _ -> dialogInterface.dismiss()})
                .setPositiveButton("Evet"){dialogInterface, _ ->
                    Common.foodSelected = null
                    Common.categorySelected = null
                    Common.currentServerUser = null
                    FirebaseAuth.getInstance().signOut()

                    val intent = Intent(this@HomeActivity,MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }

        val dialog = builder.create()
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().removeAllStickyEvents()
        EventBus.getDefault().unregister(this)
        compositeDisposable.clear()
        super.onStop()
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onCategoryClick(event:CategoryClick){
        if (event.isSuccess)
            if(menuClick != R.id.nav_food_list){
                navController!!.navigate(R.id.nav_food_list)
                menuClick = R.id.nav_food_list
            }
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onChangeMenuEvent(event:ChangeMenuClick){

        if(!event.isFromFoodList){

            //Temizleme
            navController!!.popBackStack(R.id.nav_category,true)
            navController!!.navigate(R.id.nav_category)
        }
        menuClick = -1
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onToastEvent(event:ToastEvent){

        if(event.action == Common.ACTION.CREATE){

            Toast.makeText(this,"Olu??turma Ba??ar??l??",Toast.LENGTH_SHORT).show()
        }
        else if(event.action == Common.ACTION.UPDATE){

            Toast.makeText(this,"G??ncelleme Ba??ar??l??",Toast.LENGTH_SHORT).show()
        }
        else{
            Toast.makeText(this,"Silme Ba??ar??l??",Toast.LENGTH_SHORT).show()
        }



        EventBus.getDefault().postSticky(ChangeMenuClick(event.isBackFromFoodList))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK){

            if(data != null && data.data != null){
                imgUri = data.data
                img_upload!!.setImageURI(imgUri)
            }
        }
    }
}