package jp.techacademy.taiki.hamaguchi.qa_app

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
// findViewById()を呼び出さずに該当Viewを取得するために必要となるインポート宣言
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*

class FavoriteActivity : AppCompatActivity() {

    private var mGenre = 0

    // --- ここから ---
    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter

    private var mGenreRef: DatabaseReference? = null

    val db = Firebase.firestore

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>
            val qid = map["QID"] ?: ""
            val contentsRef = db.collection(ContentsPATH).document(qid.toString())
            contentsRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        Log.d("TAG", "DocumentSnapshot data: ${document.data}")
                        val results = document.toObject(FireStoreQuestion::class.java)
                        var question = Question(
                            results!!.title, results!!.body, results!!.name, results!!.uid,
                            results!!.id, results!!.genre, byteArrayOf(), results!!.answers
                        )
                        results?.apply {
                            val bytes =
                                if (this.image.isNotEmpty()) {
                                    Base64.decode(this.image, Base64.DEFAULT)
                                } else {
                                    byteArrayOf()
                                }
                            question = Question(
                                results!!.title, results!!.body, results!!.name, results!!.uid,
                                results!!.id, results!!.genre, bytes, results!!.answers
                            )
                        }
                        //mQuestionArrayList.clear()
                        mQuestionArrayList.add(question)
                        mAdapter.notifyDataSetChanged()
                    } else {
                        Log.d("TAG", "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("TAG", "get failed with ", exception)
                }
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            // 変更があったQuestionを探す
            for (question in mQuestionArrayList) {
                if (dataSnapshot.key.equals(question.questionUid)) {
                    // このアプリで変更がある可能性があるのは回答（Answer)のみ
                    question.answers.clear()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            question.answers.add(answer)
                        }
                    }

                    mAdapter.notifyDataSetChanged()
                }
            }
        }

        override fun onChildRemoved(p0: DataSnapshot) {
            val map = p0.value as Map<String, String>
            Log.d("rem", map.toString())
            val qid = map["QID"] ?: ""
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                mQuestionArrayList.removeIf {
                    it.questionUid.contains(qid)
                }
                mAdapter.notifyDataSetChanged()
            }
        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {

        }

        override fun onCancelled(p0: DatabaseError) {

        }
    }
    // --- ここまで追加する ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        val actionBar: ActionBar? = supportActionBar
        if (actionBar != null) {
            actionBar.setTitle("お気に入り")
        }

        // --- ここから ---
        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        // ListViewの準備
        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        listView.adapter = mAdapter
        //mAdapter.notifyDataSetChanged()

        listView.setOnItemClickListener{ parent, view, position, id ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            mGenreRef = mDatabaseReference.child(FavoritePATH).child(user.uid)
            mGenreRef!!.addChildEventListener(mEventListener)
        }
    }

}