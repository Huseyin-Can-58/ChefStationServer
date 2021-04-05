package com.example.chefstationserver.ui.most_populars

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
import com.example.chefstationserver.Adapter.MyMostPopularsAdapter
import com.example.chefstationserver.CallBack.IMyButtonCallBack
import com.example.chefstationserver.Common.Common
import com.example.chefstationserver.Common.MySwipeHelper
import com.example.chefstationserver.Eventbus.ToastEvent
import com.example.chefstationserver.R
import com.example.chefstationserver.model.BestDealsModel
import com.example.chefstationserver.model.MostPopularModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MostPopularFragment : Fragment() {

    private val PICK_IMAGE_REQUEST: Int = 1234
    private lateinit var dialog: AlertDialog
    private lateinit var layoutAnimationController: LayoutAnimationController
    private var adapter: MyMostPopularsAdapter?=null
    private var recycler_most_populars: RecyclerView?=null
    internal var mostPopularsModels : List<MostPopularModel> = ArrayList<MostPopularModel>()

    private lateinit var viewModel: MostPopularViewModel

    internal lateinit var storage: FirebaseStorage
    internal lateinit var storageReference: StorageReference
    private var imageUri: Uri?=null
    internal lateinit var img_most_populars: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        viewModel =
            ViewModelProvider(this).get(MostPopularViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_most_populars, container, false)

        initViews(root)

        viewModel.getMessageError().observe(this, Observer {
            Toast.makeText(context,it, Toast.LENGTH_SHORT).show()
        })

        viewModel.getMostPopularsList().observe(this, Observer {
            dialog.dismiss()
            mostPopularsModels = it
            adapter = MyMostPopularsAdapter(context!!,mostPopularsModels)
            recycler_most_populars!!.adapter = adapter
            recycler_most_populars!!.layoutAnimation = layoutAnimationController
        })

        return root
    }

    private fun initViews(root: View?) {

        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference

        dialog = SpotsDialog.Builder().setContext(context).setCancelable(false).build()
        dialog.show()
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)
        recycler_most_populars = root!!.findViewById(R.id.recycler_most_popular) as RecyclerView
        recycler_most_populars!!.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)

        recycler_most_populars!!.layoutManager = layoutManager
        recycler_most_populars!!.addItemDecoration(DividerItemDecoration(context,layoutManager.orientation))

        val swipe = object: MySwipeHelper(context!!, recycler_most_populars!!, 200){
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

                                Common.mostPopularsSelected = mostPopularsModels[pos]

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

                                Common.mostPopularsSelected = mostPopularsModels[pos]

                                showDeleteDialog()
                            }

                        })
                )
            }
        }
    }

    private fun showDeleteDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(context!!)
        builder.setTitle("En Popüleri Sil")
        builder.setMessage("Gerçekten bu yemeği silmek istiyor musunuz?")
        builder.setNegativeButton("İPTAL",{dialogInterface, i -> dialogInterface.dismiss()})
        builder.setPositiveButton("SİL",{dialogInterface, i -> deleteMostPopulars()})
        val updateDialog = builder.create()
        updateDialog.show()
    }

    private fun deleteMostPopulars() {
        FirebaseDatabase.getInstance()
            .getReference(Common.MOST_POPULARS)
            .child(Common.mostPopularsSelected!!.key!!)
            .removeValue()
            .addOnFailureListener { e -> Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show() }
            .addOnCompleteListener { task ->
                viewModel!!.loadMostPopulars()
                EventBus.getDefault().postSticky(ToastEvent(Common.ACTION.DELETE,true))
            }
    }

    private fun showUpdateDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(context!!)
        builder.setTitle("En Popüleri Güncelle")
        builder.setMessage("Lütfen bilgileri doldurun")

        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_update_category,null)
        val edt_category_name = itemView.findViewById<View>(R.id.edt_category_name) as EditText
        img_most_populars = itemView.findViewById<View>(R.id.img_category) as ImageView

        //Veriyi oluşturmak

        edt_category_name.setText(Common.mostPopularsSelected!!.name)
        Glide.with(context!!).load(Common.mostPopularsSelected!!.image).into(img_most_populars)

        //Event'ları oluşturmak
        img_most_populars.setOnClickListener{ view ->
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
                            updateMostPopular(updateData)
                        }
                    }
            }
            else{
                updateMostPopular(updateData)
            }
        }

        builder.setView(itemView)
        val updateDialog = builder.create()
        updateDialog.show()
    }

    private fun updateMostPopular(updateData: java.util.HashMap<String, Any>) {
        FirebaseDatabase.getInstance()
            .getReference(Common.MOST_POPULARS)
            .child(Common.mostPopularsSelected!!.key!!)
            .updateChildren(updateData)
            .addOnFailureListener { e -> Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show() }
            .addOnCompleteListener { task ->
                viewModel!!.loadMostPopulars()
                EventBus.getDefault().postSticky(ToastEvent(Common.ACTION.UPDATE,true))
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK){

            if(data != null && data.data != null){
                imageUri = data.data
                img_most_populars.setImageURI(imageUri)
            }
        }
    }
}