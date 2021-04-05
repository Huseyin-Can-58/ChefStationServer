package com.example.chefstationserver.ui.foodlist

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chefstationserver.Adapter.MyFoodListAdapter
import com.example.chefstationserver.CallBack.IMyButtonCallBack
import com.example.chefstationserver.Eventbus.AddonSizeEditEvent
import com.example.chefstationserver.R
import com.example.chefstationserver.Common.Common
import com.example.chefstationserver.Common.MySwipeHelper
import com.example.chefstationserver.Eventbus.ChangeMenuClick
import com.example.chefstationserver.Eventbus.ToastEvent
import com.example.chefstationserver.SizeAddonEditActivity
import com.example.chefstationserver.Model.FoodModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class FoodListFragment : Fragment() {

    private var imageUri: Uri?=null
    private val PICK_IMAGE_REQUEST: Int = 1234
    private lateinit var foodListViewModel: FoodListViewModel
    var recycler_food_list : RecyclerView?=null
    var layoutAnimationController: LayoutAnimationController?=null

    var adapter : MyFoodListAdapter?=null
    var foodModelList : List<FoodModel> = ArrayList<FoodModel>()

    private var img_food:ImageView?=null
    private lateinit var storage:FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var dialog:android.app.AlertDialog

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.food_list_menu,menu)

        // Arama görünümünü oluşturma
        val menuItem = menu.findItem(R.id.action_search)

        val searchManager = activity!!.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menuItem.actionView as androidx.appcompat.widget.SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity!!.componentName!!))
        //Event
        searchView.setOnQueryTextListener(object:androidx.appcompat.widget.SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(search: String?): Boolean {
                startSearchFood(search!!)
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return false
            }


        })
        // Arama metnini temizleme
        val closeButton = searchView.findViewById<View>(R.id.search_close_btn) as ImageView
        closeButton.setOnClickListener{
            val tx = searchView.findViewById<View>(R.id.search_src_text) as EditText
            // Metni temizleme
            tx.setText("")
            // Kuyruğu temizleme
            searchView.setQuery("",false)
            searchView.onActionViewCollapsed()
            menuItem.collapseActionView()
            foodListViewModel.getMutableFoodModelListData().value = Common.categorySelected!!.foods
        }
    }

    private fun startSearchFood(s: String) {

        val resultFood : MutableList<FoodModel> = ArrayList()
        for(i in Common.categorySelected!!.foods!!.indices){

            val foodModel = Common.categorySelected!!.foods!![i]
            if(foodModel.name!!.toLowerCase().contains(s.toLowerCase())){

                // Sonuçların index değerleri dönecek
                foodModel.positionInList = i
                resultFood.add(foodModel)
            }
        }
        // Arama sonuçlarını güncelleme

        foodListViewModel!!.getMutableFoodModelListData().value = resultFood
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        foodListViewModel =
            ViewModelProviders.of(this).get(FoodListViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_food_list, container, false)
        initViews(root)
        foodListViewModel.getMutableFoodModelListData().observe(this, Observer{
            if(it!=null) {
            foodModelList = it
            adapter = MyFoodListAdapter(context!!,foodModelList)
            recycler_food_list!!.adapter = adapter
            recycler_food_list!!.layoutAnimation = layoutAnimationController
            }
        })
        return root
    }

    private fun initViews(root: View?) {

        setHasOptionsMenu(true) //Pencere üzerinde ayarlar menüsünü aktifleştirme

        dialog = SpotsDialog.Builder().setContext(context!!).setCancelable(false).build()
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference

        recycler_food_list = root!!.findViewById(R.id.recycler_food_list) as RecyclerView
        recycler_food_list!!.setHasFixedSize(true)
        recycler_food_list!!.layoutManager = LinearLayoutManager(context)

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)

        (activity as AppCompatActivity).supportActionBar!!.title = Common.categorySelected!!.name

        val displayMetrics = DisplayMetrics()
        activity!!.windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        val width = displayMetrics.widthPixels

        val swipe = object: MySwipeHelper(context!!, recycler_food_list!!, width/6){
            override fun instantiateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {
                buffer.add(
                    MyButton(context!!,
                        "Sil",
                        30,
                        0,
                        Color.parseColor("#9b0000"),
                        object : IMyButtonCallBack {
                            override fun onClick(pos: Int) {


                                Common.foodSelected = foodModelList[pos]
                                val builder = AlertDialog.Builder(context!!)
                                builder.setTitle("Sil")
                                    .setMessage("Bu yemeği gerçekten silmek istiyor musunuz?")
                                    .setNegativeButton("İPTAL",{dialogInterface , _ -> dialogInterface.dismiss()})
                                    .setPositiveButton("SİL",{dialogInterface, i ->
                                        val foodModel = adapter!!.getItemAtPosition(pos)
                                        if(foodModel.positionInList == -1)
                                            Common.categorySelected!!.foods!!.removeAt(pos)
                                        else
                                            Common.categorySelected!!.foods!!.removeAt(foodModel.positionInList)
                                         updateFood(Common.categorySelected!!.foods, Common.ACTION.DELETE)
                                    })

                                val deleteDialog = builder.create()
                                deleteDialog.show()
                            }

                        })
                )

                buffer.add(
                    MyButton(context!!,
                        "Güncelle",
                        30,
                        0,
                        Color.parseColor("#560027"),
                        object : IMyButtonCallBack {
                            override fun onClick(pos: Int) {

                                val foodModel = adapter!!.getItemAtPosition(pos)
                                if(foodModel.positionInList == -1)
                                    showUpdateDialog(pos,foodModel)
                                else
                                    showUpdateDialog(foodModel.positionInList,foodModel)
                            }

                        })
                )

                buffer.add(
                        MyButton(context!!,
                                "Eklenti",
                                30,
                                0,
                                Color.parseColor("#035600"),
                                object : IMyButtonCallBack {
                                    override fun onClick(pos: Int) {

                                        val foodModel = adapter!!.getItemAtPosition(pos)
                                        if(foodModel.positionInList == -1)
                                            Common.foodSelected = foodModelList!![pos]
                                        else
                                            Common.foodSelected = foodModel
                                        startActivity(Intent(context,SizeAddonEditActivity::class.java))
                                        if(foodModel.positionInList == -1)
                                            EventBus.getDefault().postSticky(AddonSizeEditEvent(true,pos))
                                        else
                                            EventBus.getDefault().postSticky(AddonSizeEditEvent(true,foodModel.positionInList))

                                    }

                                })
                )

                buffer.add(
                        MyButton(context!!,
                                "Porsiyon",
                                30,
                                0,
                                Color.parseColor("#12005e"),
                                object : IMyButtonCallBack {
                                    override fun onClick(pos: Int) {

                                        val foodModel = adapter!!.getItemAtPosition(pos)
                                        if(foodModel.positionInList == -1)
                                            Common.foodSelected = foodModelList!![pos]
                                        else
                                            Common.foodSelected = foodModel
                                        startActivity(Intent(context,SizeAddonEditActivity::class.java))
                                        if(foodModel.positionInList == -1)
                                            EventBus.getDefault().postSticky(AddonSizeEditEvent(false,pos))
                                        else
                                            EventBus.getDefault().postSticky(AddonSizeEditEvent(false,foodModel.positionInList))
                                    }

                                })
                )
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.action_create)
            showAddFoodDialog()
        return super.onOptionsItemSelected(item)
    }

    private fun showAddFoodDialog() {

        val builder = androidx.appcompat.app.AlertDialog.Builder(context!!)
        builder.setTitle("Yemeği Oluştur")
        builder.setMessage("Lütfen bilgileri doldurun")

        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_update_food,null)

        val edt_food_name = itemView.findViewById<View>(R.id.edt_food_name) as EditText
        val edt_food_price = itemView.findViewById<View>(R.id.edt_food_price) as EditText
        val edt_food_description = itemView.findViewById<View>(R.id.edt_food_description) as EditText
        img_food = itemView.findViewById<View>(R.id.img_food_image) as ImageView

        // Veriyi Oluşturmak

        Glide.with(context!!).load(R.drawable.ic_baseline_image_24).into(img_food!!)

        //Event'i Oluşturmak

        img_food!!.setOnClickListener{
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent,"Fotoğraf Seçiniz"),PICK_IMAGE_REQUEST)
        }

        builder.setNegativeButton("İPTAL",{dialogInterface, _ -> dialogInterface.dismiss()})
        builder.setPositiveButton("OLUŞTUR"){dialogInterface, i ->

            val updateFood = FoodModel()
            updateFood.name = edt_food_name.text.toString()
            updateFood.id = UUID.randomUUID().toString()
            updateFood.price = if(TextUtils.isEmpty(edt_food_price.text))
                0
            else
                edt_food_price.text.toString().toLong()
            updateFood.description = edt_food_description.text.toString()

            if(imageUri != null){

                dialog.setMessage("Yükleniyor..")
                dialog.show()

                val imageName = UUID.randomUUID().toString()
                val imageFolder = storageReference.child("images/$imageName")
                imageFolder.putFile(imageUri!!)
                        .addOnFailureListener{e ->
                            dialog.dismiss()
                            Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()
                        }
                        .addOnProgressListener { taskSnapshot ->
                            val progress = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                            dialog.setMessage("Yükleniyor $progress%")
                        }
                        .addOnSuccessListener { taskSnapshot ->
                            dialogInterface.dismiss()
                            imageFolder.downloadUrl.addOnSuccessListener { uri ->
                                dialog.dismiss()
                                updateFood.image = uri.toString()
                                if(Common.categorySelected!!.foods == null)
                                    Common.categorySelected!!.foods = ArrayList()
                                Common.categorySelected!!.foods!!.add(updateFood)
                                updateFood(Common.categorySelected!!.foods!!, Common.ACTION.CREATE)
                            }
                        }
            }
            else{

                Common.categorySelected!!.foods!!.add(updateFood)
                updateFood(Common.categorySelected!!.foods!!, Common.ACTION.CREATE)
            }
        }

        builder.setView(itemView)
        val updateDialog = builder.create()
        updateDialog.show()

    }

    private fun showUpdateDialog(pos: Int,foodModel: FoodModel) {

        val builder = AlertDialog.Builder(context!!)
        builder.setTitle("Güncelle")
        builder.setMessage("Lütfen bilgileri doldurun")

        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_update_food,null)

        val edt_food_name = itemView.findViewById<View>(R.id.edt_food_name) as EditText
        val edt_food_price = itemView.findViewById<View>(R.id.edt_food_price) as EditText
        val edt_food_description = itemView.findViewById<View>(R.id.edt_food_description) as EditText
        img_food = itemView.findViewById<View>(R.id.img_food_image) as ImageView

        // Veriyi Oluşturmak

        edt_food_name.setText(StringBuilder("").append(foodModel.name))
        edt_food_price.setText(StringBuilder("").append(foodModel.price))
        edt_food_description.setText(StringBuilder("").append(foodModel.description))
        Glide.with(context!!).load(foodModel.image).into(img_food!!)

        //Event'i Oluşturmak

        img_food!!.setOnClickListener{
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent,"Fotoğraf Seçiniz"),PICK_IMAGE_REQUEST)
        }

        builder.setNegativeButton("İPTAL",{dialogInterface, _ -> dialogInterface.dismiss()})
        builder.setPositiveButton("GÜNCELLE"){dialogInterface, i ->

            val updateFood = foodModel
            updateFood.name = edt_food_name.text.toString()
            updateFood.price = if(TextUtils.isEmpty(edt_food_price.text))
                0
            else
                edt_food_price.text.toString().toLong()
            updateFood.description = edt_food_description.text.toString()

            if(imageUri != null){

                dialog.setMessage("Yükleniyor..")
                dialog.show()

                val imageName = UUID.randomUUID().toString()
                val imageFolder = storageReference.child("images/$imageName")
                imageFolder.putFile(imageUri!!)
                        .addOnFailureListener{e ->
                            dialog.dismiss()
                            Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()
                        }
                        .addOnProgressListener { taskSnapshot ->
                            val progress = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                            dialog.setMessage("Yükleniyor $progress%")
                        }
                        .addOnSuccessListener { taskSnapshot ->
                            dialogInterface.dismiss()
                            imageFolder.downloadUrl.addOnSuccessListener { uri ->
                                dialog.dismiss()
                                updateFood.image = uri.toString()
                                Common.categorySelected!!.foods!![pos] = updateFood
                                updateFood(Common.categorySelected!!.foods!!, Common.ACTION.UPDATE)
                            }
                        }
            }
            else{

                Common.categorySelected!!.foods!![pos] = updateFood
                updateFood(Common.categorySelected!!.foods!!, Common.ACTION.UPDATE)
            }
        }

        builder.setView(itemView)
        val updateDialog = builder.create()
        updateDialog.show()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK){

            if(data != null && data.data != null){
                imageUri = data.data
                img_food!!.setImageURI(imageUri)
            }
        }
    }

    private fun updateFood(foods: MutableList<FoodModel>?,action: Common.ACTION) {

        val updateData = HashMap<String,Any>()
        updateData["foods"] = foods!!

        FirebaseDatabase.getInstance()
            .getReference(Common.CATEGORY_REF)
            .child(Common.categorySelected!!.menu_id!!)
            .updateChildren(updateData)
            .addOnFailureListener { e -> Toast.makeText(context!!,""+e.message,Toast.LENGTH_SHORT).show() }
            .addOnCompleteListener { task ->
                if(task.isSuccessful){

                    foodListViewModel.getMutableFoodModelListData()
                    EventBus.getDefault().postSticky(ToastEvent(action,true))
                }
            }
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(ChangeMenuClick(true))
        super.onDestroy()
    }
}