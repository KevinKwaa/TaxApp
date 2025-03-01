package com.example.taxapp.user

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.taxapp.firebase.FirebaseManager
//import com.example.taxapp.model.UserModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

class AuthViewModel : ViewModel() {
    private val TAG = "AuthViewModel"

    private val auth = FirebaseManager.getAuthInstance()
    private val firestore = FirebaseManager.getAuthFirestore()

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, it.exception?.localizedMessage)
                }
            }
    }

    fun register(email : String, password : String, onResult : (Boolean,String?) -> Unit){
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener{
                if(it.isSuccessful){
                    var userId = it.result?.user?.uid

                    val userModel = UserModel(email, userId!!)
                    firestore.collection("users").document(userId)
                        .set(userModel)
                        .addOnCompleteListener { dbTask->
                            if (dbTask.isSuccessful){
                                onResult(true, null)
                            }else{
                                onResult(false, "Something Went Wrong...")
                            }
                        }

                }else {
                    onResult(false, it.exception?.localizedMessage)
                }
            }
    }

    fun userProfile(name: String, phone: String, dob: String, income: String, onResult: (Boolean, String?) -> Unit){
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userDetails = hashMapOf(
                "name" to name,
                "phone" to phone,
                "dob" to dob,
                "income" to income
            )

            firestore.collection("users").document(userId)
                .update(userDetails as Map<String, Any>)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, null)
                    } else {
                        onResult(false, task.exception?.localizedMessage)
                        Log.e("AuthViewModel", "Error updating user profile", task.exception)
                    }
                }
        } else {
            onResult(false, "User not logged in")
        }
    }
}