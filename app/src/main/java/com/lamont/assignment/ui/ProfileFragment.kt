package com.lamont.assignment.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.lamont.assignment.MainActivity
import com.lamont.assignment.R
import com.lamont.assignment.databinding.FragmentProfileBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*



class ProfileFragment : Fragment() {

    var _binding : FragmentProfileBinding? = null
    val binding get() = _binding!!
    lateinit var sharedPreferences: SharedPreferences
    lateinit var db : FirebaseFirestore
    lateinit var dbAuth : FirebaseAuth
    lateinit var dbStorage : FirebaseStorage

    //image holder
    var imgBit:Bitmap? = null
    var imgUri:Uri? = null
    var imgChange = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        dbAuth = FirebaseAuth.getInstance()
        dbStorage = FirebaseStorage.getInstance()
        val storageRef = dbStorage.reference
        sharedPreferences = requireActivity().getSharedPreferences("SHARE_PREF", Context.MODE_PRIVATE)

        db.collection("users").document(dbAuth.currentUser?.uid!!)
            .addSnapshotListener { doc, error ->
                doc?.let {
                    binding.tvUid.setText(getString(R.string.uid_pro,dbAuth.currentUser?.uid.toString()))
                    binding.tvUsername.setText(doc.data?.get("username").toString())
                    binding.etEmail.setText(doc.data?.get("email").toString())
                    binding.etPhone.setText(doc.data?.get("phone").toString())
                    binding.etDOB.setText(doc.data?.get("dob").toString())
                    binding.etAddress.setText(doc.data?.get("address").toString())
                    val localFile = File.createTempFile("tempImg", "jpg")
                    storageRef.child("profile/${doc.data?.get("imgName")}").getFile(localFile)
                        .addOnProgressListener {
                            binding.pbImg.visibility = View.VISIBLE
                        }
                        .addOnSuccessListener {
                            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                            binding.pbImg.visibility = View.GONE
                            binding.ivProfile.setImageBitmap(bitmap)
                    }
                }
            }

        binding.editProfile.setOnClickListener {

            if (binding.editProfile.text == getString(R.string.edit)) {
                binding.etEmail.isFocusableInTouchMode = true
                binding.etPhone.isFocusableInTouchMode = true
                binding.etAddress.isFocusableInTouchMode = true
                binding.ivProfile.isClickable = true
                binding.tvUid.setOnClickListener {
                    Toast.makeText(requireContext(), "UID is not editable!", Toast.LENGTH_SHORT)
                        .show()
                }
                binding.tvUsername.setOnClickListener {
                    Toast.makeText(requireContext(), "Username is not editable!", Toast.LENGTH_SHORT).show()
                }
                binding.etDOB.setOnClickListener {
                    Toast.makeText(requireContext(), "DOB is not editable!", Toast.LENGTH_SHORT).show()
                }
                binding.editProfile.text = "Save"

                binding.ivProfile.setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Select Photo")
                        .setMessage("Please choose a method to upload photo.")
                        .setNeutralButton("Cancel") { dialog, which ->
                        }
                        .setNegativeButton("Gallery") { dialog, which ->
                            val intent = Intent(Intent.ACTION_PICK)
                            intent.type = "image/*"
                            startActivityForResult(intent, RequestFragment.IMAGE_REQUEST_CODE)
                        }
                        .setPositiveButton("Camera") { dialog, which ->
                            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            startActivityForResult(intent, RequestFragment.CAMERA_REQUEST_CODE)

                        }
                        .show()
                }

            } else {

                val email = binding.etEmail.text.toString()
                val phone = binding.etPhone.text.toString()
                val address = binding.etAddress.text.toString()


                db.collection("users")
                    .get()
                    .addOnSuccessListener {
                        var error = false
                        for (doc in it) {
                            if (doc.id.toString() == dbAuth.currentUser?.uid) {
                                continue
                            }
                            when {
                                email == doc.data.get("email").toString() -> {
                                    Toast.makeText(requireContext(), "Email existed", Toast.LENGTH_SHORT).show()
                                    error = true
                                    break
                                }
                                !email.matches(RegisterFragment.emailPattern.toRegex()) -> {
                                    Toast.makeText(requireContext(), "Invalid email address", Toast.LENGTH_SHORT).show()
                                    error = true
                                    break
                                }
                                phone == doc.data.get("phone").toString() -> {
                                    Toast.makeText(requireContext(), "Phone existed", Toast.LENGTH_SHORT).show()
                                    error = true
                                    break
                                }
                                phone.length > 11 || phone.length < 10 -> {
                                    Toast.makeText(requireContext(), "Phone Invalid", Toast.LENGTH_SHORT).show()
                                    error = true
                                    break
                                }
                                else -> {
                                    error = false
                                }
                            }
                        }
                        if (!error) {
                            val user = mutableMapOf<String, Any>(
                                "email" to binding.etEmail.text.toString(),
                                "phone" to binding.etPhone.text.toString(),
                                "address" to binding.etAddress.text.toString()
                            )
                            if(imgChange) {
                                val username = sharedPreferences.getString("username", null)!!
                                val formatter = SimpleDateFormat("yy_MM_dd_HH_mm_ss", Locale.getDefault())
                                val imgName = "${username}_${formatter.format(Date())}" //Kae Lun_22_03_28_11_11_11
                                user["imgName"] = imgName
                            }
                            dbAuth.currentUser?.updateEmail(user["email"]!!.toString())
                            val editSharedPref = sharedPreferences.edit()
                            editSharedPref.putString("email", user["email"]!!.toString())
                            editSharedPref.commit()

                            db.collection("users").document(dbAuth.currentUser?.uid!!).update(user)
                                .addOnSuccessListener {
                                    if(imgChange) {
                                        if (imgBit != null) {
                                            val baos = ByteArrayOutputStream()
                                            imgBit!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                                            val data = baos.toByteArray()
                                            storageRef.child("profile/${user["imgName"]}").putBytes(data)
                                                .addOnFailureListener {
                                                    Toast.makeText(requireContext(), "FirebaseStorage API Error", Toast.LENGTH_SHORT).show()
                                                }

                                        } else if (imgUri != null) {
                                            storageRef.child("profile/${user["imgName"]}").putFile(imgUri!!)
                                                .addOnFailureListener {
                                                    Toast.makeText(requireContext(), "FirebaseStorage API Error", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                        //update the field status to uneditable
                                        binding.etEmail.isFocusableInTouchMode = false
                                        binding.etPhone.isFocusableInTouchMode = false
                                        binding.etAddress.isFocusableInTouchMode = false
                                        binding.etEmail.isFocusable = false
                                        binding.etPhone.isFocusable = false
                                        binding.etAddress.isFocusable = false
                                        binding.ivProfile.isClickable = false
                                        binding.pbImg.visibility = View.GONE
                                        imgChange = false
                                    }
                                    binding.editProfile.text = getString(R.string.edit)
                                    Toast.makeText(requireContext(), "Update Successful", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                                }
                            binding.etEmail.setText(binding.etEmail.text.toString().lowercase())
                            findNavController().navigate(R.id.profileFragment)
                        }
                    }
            }
        }

        binding.logout.setOnClickListener {
            if (binding.logout.text == "Logout") {
                dbAuth.signOut()
                val editPref = sharedPreferences.edit()
                editPref.remove("email")
                editPref.remove("password")
                editPref.remove("username")
                editPref.commit()
                val intent = Intent(requireActivity(), MainActivity::class.java)
                startActivity(intent)
            }
        }

        binding.changePassword.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.reset_password, null, false)
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("Reset Password")
                .setView(dialogView)
                .setNeutralButton("Cancel") { dialog, which -> }
                .setPositiveButton("Confirm", null)
                .show()
            dialog.getButton(AlertDialog. BUTTON_POSITIVE)
                .setOnClickListener {
                    val password = dialogView.findViewById<EditText>(R.id.etOldPswd).text.toString()
                    val newPswd = dialogView.findViewById<EditText>(R.id.etNewPswd).text.toString()
                    val conNewPswd = dialogView.findViewById<EditText>(R.id.etConNewPswd).text.toString()

                    if (password != sharedPreferences.getString("password", null).toString()) {
                        Toast.makeText(requireContext(), "Wrong Old Password", Toast.LENGTH_SHORT).show()
                    } else if (newPswd != conNewPswd) {
                        Toast.makeText(requireContext(), "New Password not Match", Toast.LENGTH_SHORT).show()
                    } else if (!newPswd.matches(RegisterFragment.passwordPattern.toRegex())) {
                        Toast.makeText(requireContext(), "Password must contains 8 characters with at least one special character, one capital letter and one small letter", Toast.LENGTH_SHORT).show()
                    } else if (newPswd == password) {
                        Toast.makeText(requireContext(), "Old Password Same With New Password", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Password update successfully", Toast.LENGTH_SHORT).show()
                        dbAuth.currentUser?.updatePassword(newPswd)
                        val editSharedPref = sharedPreferences.edit()
                        editSharedPref.putString("password", newPswd)
                        editSharedPref.commit()
                        db.collection("users").document(dbAuth.currentUser!!.uid)
                            .update("password", newPswd)
                        dialog.dismiss()

                    }
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            imgChange = true
            if (requestCode == RequestFragment.IMAGE_REQUEST_CODE) {
                imgUri = data?.data!!
                binding.ivProfile.setImageURI(imgUri)
            } else if (requestCode == RequestFragment.CAMERA_REQUEST_CODE) {
                imgBit = data?.extras?.get("data")!! as Bitmap
                binding.ivProfile.setImageBitmap(imgBit)
            }
        }
    }
}