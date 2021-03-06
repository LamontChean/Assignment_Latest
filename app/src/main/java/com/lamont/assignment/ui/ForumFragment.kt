package com.lamont.assignment.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lamont.assignment.R
import com.lamont.assignment.adapter.PostAdapter
import com.lamont.assignment.databinding.FragmentForumBinding
import com.lamont.assignment.viewModel.PostViewModel

class ForumFragment : Fragment() {

    private var _binding: FragmentForumBinding? = null
    private val binding get() = _binding!!
    private lateinit var postAdapter : PostAdapter
    lateinit var dbAuth : FirebaseAuth
    lateinit var db : FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("Tag", "ForumFragment.onCreateView() has been called.")
        _binding = FragmentForumBinding.inflate(inflater, container, false)

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("Tag", "ForumFragment.onViewCreated() has been called.")

        //Declaring necessary variable for data access
        dbAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        //Declaring view model for post & recycle view
        val postModel = PostViewModel()
        postAdapter = PostAdapter(requireContext())
        binding.forumRecycler.adapter = postAdapter

        //Load posts, if empty, an empty image will be show to notify the users
        postModel.loadPostList().observe(requireActivity(), Observer {
            if(it.isEmpty()){
                binding.emptyContainer.visibility = View.VISIBLE
            } else {
                binding.emptyContainer.visibility = View.GONE
            }

            postAdapter.setData(it)
        })

        //Enable the clicking of post in recycler view
        postAdapter.onItemClickListener(object: PostAdapter.OnItemClickListener{
            @SuppressLint("ResourceAsColor")
            override fun onItemClick(position: Int, view: View) {
                //Check the action when user click, whether it is like, comment or delete
                when (view.id) {
                    R.id.like -> {
                        val postId = postModel.loadPostList().value?.get(position)!!.postId
                        db.collection("like").whereEqualTo("postId", postId)
                            .get()
                            .addOnSuccessListener {
                                var exist = false
                                var likeID = ""
                                val likeButton = view.findViewById<Button>(R.id.like)
                                for ( doc in it) {
                                    if (doc["ownerId"] == dbAuth.currentUser!!.uid) {
                                        exist = true
                                        likeID = doc.id
                                        break
                                    }
                                }
                                if (exist) {
                                    PostViewModel.removeLike(likeID)
                                    likeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.love_black, 0, 0, 0)
                                    likeButton.setTextColor(R.color.black)
                                }
                                else {
                                    val like = mapOf<String, Any>(
                                        "ownerId" to dbAuth.currentUser!!.uid,
                                        "postId" to postId
                                    )
                                    PostViewModel.addLike(like as MutableMap<String, Any>)
                                    likeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.love_red, 0, 0, 0)
                                    likeButton.setTextColor(R.color.red)
                                }
                            }
                    }
                    R.id.comment -> {
                        val bundle = Bundle()
                        bundle.putString("postId", postModel.loadPostList().value?.get(position)!!.postId)
                        bundle.putString("imgUri", postModel.loadPostList().value?.get(position)!!.imgUri.toString())
                        bundle.putString("videoUri", postModel.loadPostList().value?.get(position)!!.videoUri.toString())
                        bundle.putString("forumDesc", postModel.loadPostList().value?.get(position)!!.forumDesc)
                        bundle.putString("ownerName", postModel.loadPostList().value?.get(position)!!.postOwner)
                        bundle.putString("ivProfile", postModel.loadPostList().value?.get(position)!!.ivProfile.toString())
                        bundle.putString("dateTime", postModel.loadPostList().value?.get(position)!!.createdDate.toString())

                        val navController = findNavController()
                        navController.navigate(R.id.commentFragment, bundle)
                    }
                    R.id.btnDelete -> {
                        val dialog = AlertDialog.Builder(requireContext())
                        dialog.setTitle(getString(R.string.remove))
                            .setMessage(getString(R.string.rmConfirmation))
                            .setNeutralButton(getString(R.string.cancel), null)
                            .setPositiveButton(getString(R.string.confirm)) { dialog, which ->
                                PostViewModel.deletePost(postModel.loadPostList().value?.get(position)!!.postId)
                            }.show()
                    }
                }
            }
        })

        //Page refresh function
        binding.swipeToRefresh.setOnRefreshListener {
            binding.swipeToRefresh.isRefreshing = false
            binding.forumRecycler.adapter = postAdapter
        }

    }

}