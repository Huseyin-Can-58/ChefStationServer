package com.example.chefstationserver.ui.best_deals

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chefstationserver.Adapter.MyBestDealsAdapter
import com.example.chefstationserver.CallBack.IMyButtonCallBack
import com.example.chefstationserver.Common.Common
import com.example.chefstationserver.Common.MySwipeHelper
import com.example.chefstationserver.Eventbus.ToastEvent
import com.example.chefstationserver.R
import com.example.chefstationserver.model.BestDealsModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class BestDealsFragment : Fragment() {

    private val PICK_IMAGE_REQUEST: Int = 1234
    private lateinit var dialog: AlertDialog
    private lateinit var layoutAnimationController: LayoutAnimationController
    private var adapter: MyBestDealsAdapter?=null
    private var recycler_best_deals: RecyclerView?=null
    internal var bestDealsModels : List<BestDealsModel> = ArrayList<BestDealsModel>()

    private lateinit var viewModel: BestDealsViewModel

    internal lateinit var storage: FirebaseStorage
    internal lateinit var storageReference: StorageReference
    private var imageUri: Uri?=null
    internal lateinit var img_best_deals: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        viewModel =
            ViewModelProvider(this).get(BestDealsViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_best_deals, container, false)

        initViews(root)

        viewModel.getMessageError().observe(this, Observer {
            Toast.makeText(context,it, Toast.LENGTH_SHORT).show()
        })

        viewModel.getBestDealsList().observe(this, Observer {
            dialog.dismiss()
            bestDealsModels = it
            adapter = MyBestDealsAdapter(context!!,bestDealsModels)
            recycler_best_deals!!.adapter = adapter
            recycler_best_deals!!.layoutAnimation = layoutAnimationController
        })

        return root
    }

    private fun initViews(root: View?) {

        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference

        dialog = SpotsDialog.Builder().setContext(context).setCancelable(false).build()
        dialog.show()
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)
        recycler_best_deals = root!!.findViewById(R.id.recycler_best_deal) as RecyclerView
        recycler_best_deals!!.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)

        recycler_best_deals!!.layoutManager = layoutManager
        recycler_best_deals!!.addItemDecoration(DividerItemDecoration(context,layoutManager.orientation))

        val swipe = object: MySwipeHelper(context!!, recycler_best_deals!!, 200){
            override fun instantiateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {
                buffer.add(
                    MyButton(context!!,
                        "Güncelle",
                        30,
                        0,
                        Color.parseColor("#560027"),
                        object : IMyButtonCallBack {
                            override fun onClick(pos: Int) {

                                Common.bestDealsSelected = bestDealsModels[pos]

                                showUpdateDialog()
                            }

                        })
                )

                buffer.add(
                    MyButton(context!!,
                        "Sil",
                        30,
                        0,
                        Color.parseColor("#333639"),
                        object : IMyButtonCallBack {
                            override fun onClick(pos: Int) {

                                Common.bestDealsSelected = bestDealsModels[pos]

                                showDeleteDialog()
                            }

                        })
                )
            }
        }
    }

    private fun showDeleteDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(context!!)
        builder.setTitle("En İyi Fiyatı Sil")
        builder.setMessage("Gerçekten bu yemeği silmek istiyor musunuz?")
        builder.setNegativeButton("İPTAL",{dialogInterface, i -> dialogInterface.dismiss()})
        builder.setPositiveButton("SİL",{dialogInterface, i -> deleteBestDeals()})
        val updateDialog = builder.create()
        updateDialog.show()
    }

    private fun deleteBestDeals() {
        FirebaseDatabase.getInstance()
            .getReference(Common.BEST_DEALS)
            .child(Common.bestDealsSelected!!.key!!)
            .removeValue()
            .addOnFailureListener { e -> Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show() }
            .addOnCompleteListener { task ->
                viewModel!!.loadBestDeals()
                EventBus.getDefault().postSticky(ToastEvent(Common.ACTION.DELETE,true))
            }
    }

    private fun showUpdateDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(context!!)
        builder.setTitle("En İyi Fiyatı Güncelle")
        builder.setMessage("Lütfen bilgileri doldurun")

        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_update_category,null)
        val edt_category_name = itemView.findViewById<View>(R.id.edt_category_name) as EditText
        img_best_deals = itemView.findViewById<View>(R.id.img_category) as ImageView

        //Veriyi oluşturmak

        edt_category_name.setText(Common.bestDealsSelected!!.name)
        Glide.with(context!!).load(Common.bestDealsSelected!!.image).into(img_best_deals)

        //Event'ları oluşturmak
        img_best_deals.setOnClickListener{ view ->
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent,"Fotoğraf Seçiniz"),PICK_IMAGE_REQUEST)
        }

        builder.setNegativeButton("İPTAL") {dialogInterface , _ -> dialogInterface.dismiss()}
        builder.setPositiveButton("GÜNCELLE") {dialogInterface, _ ->

            val updateData = HashMap<String,Any>()
            updateData["name"] = edt_category_name.text.toString()
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
                        imageFolder.downloadUrl.addOnSuccessListener { uri ->
                            dialogInterface.dismiss()
                            dialog.dismiss()
                            updateData["image"] = uri.toString()
                            updateBestDeal(updateData)
                        }
                    }
            }
            else{
                updateBestDeal(updateData)
            }
        }

        builder.setView(itemView)
        val updateDialog = builder.create()
        updateDialog.show()
    }

    private fun updateBestDeal(updateData: java.util.HashMap<String, Any>) {
        FirebaseDatabase.getInstance()
            .getReference(Common.BEST_DEALS)
            .child(Common.bestDealsSelected!!.key!!)
            .updateChildren(updateData)
            .addOnFailureListener { e -> Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show() }
            .addOnCompleteListener { task ->
                viewModel!!.loadBestDeals()
                EventBus.getDefault().postSticky(ToastEvent(Common.ACTION.UPDATE,true))
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK){

            if(data != null && data.data != null){
                imageUri = data.data
                img_best_deals.setImageURI(imageUri)
            }
        }
    }
}