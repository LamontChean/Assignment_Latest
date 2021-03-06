package com.lamont.assignment.repository

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lamont.assignment.model.Request

class RequestRepository() {
    var db : FirebaseFirestore = FirebaseFirestore.getInstance()
    var _requestList : MutableLiveData<MutableList<Request>> = MutableLiveData(mutableListOf())

    fun loadRequestList(): MutableLiveData<MutableList<Request>> {
        readRequestList()
        return _requestList
    }

    fun updateStatus(requestId: String, status: Int) {
        db.collection("request")
            .document(requestId)
            .update("status", status)
    }

    fun updateDonor(requestId: String, donorId: String) {
        db.collection("request")
            .document(requestId)
            .update("donorId", donorId)
    }

    fun removeRequest(requestId: String) {
        db.collection("request")
            .document(requestId)
            .delete()
    }

    fun updateId(requestId: String) {
        db.collection("request")
            .document(requestId)
            .update("requestId", requestId)
    }

    fun updateImgUri(requestId: String, imgUri: Uri) {
        db.collection("request")
            .document(requestId)
            .update("imgUri", imgUri.toString())
    }

    private fun readRequestList() {
        db.collection("request")
            .orderBy("status", Query.Direction.DESCENDING)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                firebaseFirestoreException?.let {
                    return@addSnapshotListener
                }
                querySnapshot?.let {
                    var requestData : MutableList<Request> = mutableListOf()
                    for (document in it) {
                        val requestId = document.get("requestId").toString()
                        val ownerId = document.get("ownerId").toString()
                        val name = document.get("owner").toString()
                        val desc= document.get("desc").toString()
                        val category = document.get("category").toString()
                        val imgUri = document.get("imgUri").toString().toUri()
                        val status = document.get("status").toString().toIntOrNull()
                        val donor = document.get("donorId").toString()
                        val createdDate = document.get("createdDate").toString()
                        val request = Request(requestId, ownerId, name, desc, category, imgUri, donor, createdDate, status!!)
                        requestData.add(request)
                    }
                    _requestList.value = requestData
                }
            }
    }
}

internal var _requestList : MutableLiveData<MutableList<Request>>
    get() {return _requestList}
    set(value) { _requestList = value}