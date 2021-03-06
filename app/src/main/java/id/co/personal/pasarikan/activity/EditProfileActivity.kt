package id.co.personal.pasarikan.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import id.co.personal.pasarikan.MyFunction
import id.co.personal.pasarikan.R
import id.co.personal.pasarikan.models.User
import kotlinx.android.synthetic.main.activity_edit_profile.*

class EditProfileActivity : AppCompatActivity() {
    private var storage: FirebaseStorage = Firebase.storage
    private var storageRef: StorageReference
    private var database: FirebaseDatabase = Firebase.database
    private var dbRef: DatabaseReference
    private var imageUri: Uri? = null
    private var downloadUrl: String? = null
    private lateinit var auth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private var uid: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        auth = FirebaseAuth.getInstance()
    }

    public override fun onStart() {
        super.onStart()
        currentUser = auth.currentUser
        currentUser?.let {
            uid = currentUser!!.uid
            readUserProfile(uid)
        }
        buttonFunction()
    }

    init {
        dbRef = database.getReference("users")
        storageRef = storage.reference
    }

    private fun readUserProfile(uid: String) {
        val userListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue<User>()
                et_owner_name.setText(user!!.ownerName)
                et_email.setText(user.email)
                et_ktp.setText(user.noKTP)
                et_phone_number.setText(user.phoneNumber)
                et_shop_address.setText(user.address)
                downloadUrl = user.imageUrl
                Glide.with(this@EditProfileActivity)
                    .load(downloadUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .into(iv_profile_picture)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("DataChange", "loadPost:onCancelled", databaseError.toException())
            }
        }
        dbRef.child(uid).addListenerForSingleValueEvent(userListener)
    }

    private fun buttonFunction() {
        btn_writeData.setOnClickListener {
            val email = et_email.text.toString().trim()
            if (et_ktp.text!!.isNotBlank() && et_phone_number.text!!.isNotBlank() && et_shop_address.text!!.isNotBlank()) {
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    et_email.error = "Email tidak valid"
                } else {
                    btn_writeData.visibility = View.GONE
                    progress_write_data.visibility = View.VISIBLE
                    writeUserData(
                        uid,
                        et_owner_name.text.toString(),
                        et_ktp.text.toString(),
                        et_shop_address.text.toString(),
                        et_phone_number.text.toString(),
                        et_email.text.toString(),
                        downloadUrl
                    )
                }
            } else {
                Toast.makeText(this, "Mohon Lengkapi Form", Toast.LENGTH_SHORT).show()
            }
        }
        toolbar_edit_profil.setNavigationOnClickListener {
            finish()
        }
        btn_uploadImage.setOnClickListener {
            openFileChooser()
        }
    }

    private fun openFileChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun uploadImage(uri: Uri?) {
        val loadingDialog = MyFunction.createLoadingDialog(this)
        loadingDialog.show()
        val ref: StorageReference = storageRef.child(
            "images/profile/$uid"
        )
        val uploadTask = ref.putFile(uri!!)
        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                loadingDialog.dismissWithAnimation()
                task.exception?.let {
                    throw it
                }
            }
            ref.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                loadingDialog.dismissWithAnimation()
                downloadUrl = task.result.toString()
                dbRef.child(uid).child("imageUrl").setValue(downloadUrl)
                val successDialog = MyFunction.createSuccessDialog(
                    context = this,
                    titleText = "Success",
                    contentText = "Image has been uploaded"
                )
                successDialog.show()
                readUserProfile(uid)
            } else {
                val errorDialog =
                    MyFunction.createErrorDialog(this, contentText = "Failed to upload an image")
                errorDialog.show()
            }
        }
    }

    private fun writeUserData(
        uid: String,
        ownerName: String,
        noKTP: String,
        address: String,
        phoneNumber: String,
        email: String,
        imageUrl: String?
    ) {
        val userData = User(
            userId = uid,
            ownerName = ownerName,
            noKTP = noKTP,
            address = address,
            phoneNumber = phoneNumber,
            email = email,
            imageUrl = imageUrl
        )
        dbRef.child(uid).setValue(userData).addOnSuccessListener {
            if (progress_write_data.visibility == View.VISIBLE) {
                currentUser?.updateEmail(email)?.addOnSuccessListener {
                    progress_write_data.visibility = View.GONE
                    finish()
                }
            }
        }
            .addOnFailureListener {
                if (progress_write_data.visibility == View.VISIBLE) {
                    progress_write_data.visibility = View.GONE
                    btn_writeData.visibility = View.VISIBLE
                }
            }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null
        ) {
            imageUri = data.data
            Glide.with(this)
                .load(imageUri)
                .placeholder(R.drawable.ic_default_user_picture)
                .apply(RequestOptions.circleCropTransform())
                .into(iv_profile_picture)
            uploadImage(imageUri)
        }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 2
    }
}