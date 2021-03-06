package id.co.personal.pasarikan.models

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    var userId: String = "",
    var username: String? = "",
    var ownerName: String = "",
    var noKTP: String = "",
    var address: String = "",
    var phoneNumber: String = "",
    var email: String = "",
    var imageUrl: String? = ""
)