package com.example.coding_study

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coding_study.databinding.MypageFragmentBinding
import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MyPageFragment: Fragment(R.layout.mypage_fragment) {
    private lateinit var binding: MypageFragmentBinding
    private lateinit var myPageProfileView: View
    private lateinit var myPageListView: View
    private lateinit var myPageAdapter: MyPageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = MypageFragmentBinding.inflate(inflater, container, false)
        val view = binding.root

        myPageProfileView = binding.myPageProfileView
        myPageListView = binding.myPageListView

        val myPageRecyclerView = binding.myPageRecyclerView

        var itemClickListener: MyPageAdapter.ItemClickListener = object : MyPageAdapter.ItemClickListener {
            override fun onItemClick(position: Int) {
                Log.e("MypageFragment", "onclick")
                if (position == 0) {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.study_fragment_layout, MyPageEditFragment())
                        .addToBackStack(null)
                        .commit()
                }
            }
        }

        val textList = listOf("내 프로필 수정","신청한 스터디", "내가 쓴 글")
        myPageAdapter = MyPageAdapter(textList, itemClickListener)
        myPageRecyclerView.adapter = myPageAdapter
        binding.myPageRecyclerView.layoutManager = LinearLayoutManager(context)

        val sharedPreferences = requireActivity().getSharedPreferences("MyToken", Context.MODE_PRIVATE)
        val token = sharedPreferences?.getString("token", "") // 저장해둔 토큰값 가져오기

        val retrofitBearer = Retrofit.Builder()
            .baseUrl("http://112.154.249.74:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .addHeader("Authorization", "Bearer " + token.orEmpty())
                            .build()
                        Log.d("TokenInterceptor_StudyFragment", "Token: " + token.orEmpty())
                        chain.proceed(request)
                    }
                    .build()
            )
            .build()

        val myPageService = retrofitBearer.create(MyPageGetService::class.java)
        myPageService.myPageGetProfile().enqueue(object : Callback<MyPageProfileResponse>{
            override fun onResponse(call: Call<MyPageProfileResponse>, response: Response<MyPageProfileResponse>
            ) {
                if (response.isSuccessful) {
                    val myPageResponse = response.body()
                    Log.e("MyPageFragment response body", "$myPageResponse")
                    Log.e("MyPageFragment response code", "${response.code()}")

                    val gson = Gson()
                    val myPageData = myPageResponse?.data
                    val myPageDto = gson.fromJson(gson.toJson(myPageData), Array<MyProfile>::class.java)
                    val myProfile: MyProfile = myPageDto[0]

                    if (myPageResponse != null) {
                        binding.myPageNickname.text = myProfile.nickname
                        binding.myPageAddress.text = myProfile.address
                        binding.myPageField1.text = myProfile.fieldList[0]
                        binding.myPageField2.text = myProfile.fieldList[1]
                    }
                }
            }

            override fun onFailure(call: Call<MyPageProfileResponse>, t: Throwable) {
                Log.e("MyPageFragment", "Failed to get profile", t)
                Toast.makeText(context, "서버 연결 실패", Toast.LENGTH_LONG).show()
            }

        })

        return view
    }
}