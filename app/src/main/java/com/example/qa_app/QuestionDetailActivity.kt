package com.example.qa_app

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.CompoundButton
import android.widget.ListView

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_question_detail.*
import kotlinx.android.synthetic.main.activity_question_send.*
import kotlinx.android.synthetic.main.list_question_detail.*

import java.util.HashMap
//import com.sun.tools.internal.xjc.reader.xmlschema.bindinfo.BIConversion.User
import com.google.firebase.database.ValueEventListener
import com.google.firebase.internal.FirebaseAppHelper.getUid



class QuestionDetailActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private var mGenre: Int = 0

    var dataBaseReference = FirebaseDatabase.getInstance().reference

    var flag = false

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""

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
        mQuestion = extras.get("question") as Question

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            var user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                favorite_button.setEnabled(false)
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                favorite_button.setEnabled(true)
                // Questionを渡して回答作成画面を起動する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }

        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)

        favorite_button.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()

        // ログイン済みのユーザーを取得する
        var user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            favorite_button.setVisibility(View.INVISIBLE)
        } else {
            favorite_button.setVisibility(View.VISIBLE)
            dataBaseReference.child("favorite").child(user!!.uid).child(mQuestion.questionUid).addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if(dataSnapshot.value != null) {
                            flag = true
                            favorite_button.text = "お気に入り解除"
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                    }
                }
            )

        }
    }

    override fun onClick(v: View) {
        favorite_button.notPressTwice()

        // ログイン済みのユーザーを取得する
        var user = FirebaseAuth.getInstance().currentUser

        // favorite_button.text = "お気に入り解除"
        val extras = intent.extras
        mGenre = extras.getInt("genre")


        if (flag == false) {
            flag = true

            favorite_button.text = "お気に入り解除"
            val data = HashMap<String, String>()

            var genreRef =
                dataBaseReference.child("favorite").child(user!!.uid).child(mQuestion.questionUid)
            data["genre"] = mQuestion.genre.toString()
            genreRef.setValue(data)
        } else {
            flag = false

            favorite_button.text = "お気に入り"

            var genreRef = dataBaseReference.child("favorite").child(user!!.uid).child(mQuestion.questionUid)
            genreRef.removeValue()
        }
    }
    /**
     * 二度押し防止施策として 1秒間タップを禁止する
     */
    fun View.notPressTwice() {
        this.isEnabled = false
        this.postDelayed({
            this.isEnabled = true
        }, 1000L)
    }
}