package jp.techacademy.taiki.hamaguchi.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*
import java.util.ArrayList

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>
            Log.d("tst3", map.toString())

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras!!.get("question") as Question

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                // --- ここから ---
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
                // --- ここまで ---
            }
        }

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            fab_fav.hide()
        } else {
            //お気に入りが何もないとき
            fab_fav.setImageResource(R.drawable.ic_star_border)
            fab_fav.setOnClickListener(object: View.OnClickListener{
                override fun onClick(p0: View?) {
                    val favRef = dataBaseReference.child(FavoritePATH).child(user.uid)
                    val data = HashMap<String, String>()
                    data["QID"] = mQuestion.questionUid
                    favRef.push().setValue(data).addOnCompleteListener{
                        fab_fav.setImageResource(R.drawable.ic_star)
                    }
                }

            })

            var flag = false

            dataBaseReference.child(FavoritePATH).child(user.uid).addChildEventListener(object :ChildEventListener{
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val map = snapshot.value as Map<*, *>
                    Log.d("tst", map.toString())
                    Log.d("tst2", map["QID"] as? String ?: "1")
                    val qid = map["QID"] as? String
                    if (qid == mQuestion.questionUid) {
                        //お気に入り済みの時
                        fab_fav.setImageResource(R.drawable.ic_star)
                        fab_fav.setOnClickListener(object: View.OnClickListener{
                            override fun onClick(p0: View?) {
                                val favRef = dataBaseReference.child(FavoritePATH).child(user.uid).child(
                                    snapshot.key.toString()
                                )
                                val data = null
                                favRef.setValue(data).addOnCompleteListener{
                                    fab_fav.setImageResource(R.drawable.ic_star_border)
                                }
                            }

                        })
                        flag = true
                    } else {
                        if (!flag){
                            fab_fav.setImageResource(R.drawable.ic_star_border)
                            fab_fav.setOnClickListener(object: View.OnClickListener{
                                override fun onClick(p0: View?) {
                                    val favRef = dataBaseReference.child(FavoritePATH).child(user.uid)
                                    val data = HashMap<String, String>()
                                    data["QID"] = mQuestion.questionUid
                                    favRef.push().setValue(data).addOnCompleteListener{
                                        fab_fav.setImageResource(R.drawable.ic_star)
                                    }
                                }

                            })
                        }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    fab_fav.setImageResource(R.drawable.ic_star_border)
                    fab_fav.setOnClickListener(object: View.OnClickListener{
                        override fun onClick(p0: View?) {
                            val favRef = dataBaseReference.child(FavoritePATH).child(user.uid)
                            val data = HashMap<String, String>()
                            data["QID"] = mQuestion.questionUid
                            favRef.push().setValue(data).addOnCompleteListener{
                                fab_fav.setImageResource(R.drawable.ic_star)
                            }
                        }

                    })
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
        }
    }
}