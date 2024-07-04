package com.example.collegetests

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth


class SignInActivity : AppCompatActivity() {



    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null){
            if (sharedPreferences.getInt("isTeacherLogged", -1) != -1){
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
        setContentView(R.layout.activity_sign_in)

            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            val signInStudentButton: Button = findViewById(R.id.signin_asStudentButton)
            val signInTeacherButton: Button = findViewById(R.id.signin_asTeacherButton)
            val signInButton: Button = findViewById(R.id.signin_button)
            val switchButton: Button = findViewById(R.id.switch_button)

            val textView2: EditText = findViewById(R.id.textView2)
            val textView3: EditText = findViewById(R.id.textView3)
            val textView4: EditText = findViewById(R.id.textView4)

            var studentBtnClicked:Boolean = true



        switchButton.setOnClickListener{
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
        
        signInStudentButton.setOnClickListener {
            studentBtnClicked = true
            signInTeacherButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple_500)))
            signInTeacherButton.setTextColor((ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))))
            signInStudentButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.light_grey)))
            signInStudentButton.setTextColor((ColorStateList.valueOf(ContextCompat.getColor(this, R.color.grey))))

            textView3.inputType = InputType.TYPE_CLASS_NUMBER
            textView2.setText(null)
            textView3.setText(null)
            textView4.setText(null)

            textView2.setHint("Введите код группы")
            textView3.setHint("Введите Ваш поряд. номер\n на момент регистрации")
            textView4.setHint("Введите пароль")
            textView4.isVisible = true
            textView4.isClickable = true
            inputMethodManager.hideSoftInputFromWindow(textView2.windowToken, 0)
        }
        signInTeacherButton.setOnClickListener {

            studentBtnClicked = false
            signInStudentButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple_500)))
            signInStudentButton.setTextColor((ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))))
            signInTeacherButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.light_grey)))
            signInTeacherButton.setTextColor((ColorStateList.valueOf(ContextCompat.getColor(this, R.color.grey))))

            textView3.inputType = 129
            textView2.setText(null)
            textView3.setText(null)
            textView4.setText(null)

            textView2.setHint("Введите ФИО преподавателя")
            textView3.setHint("Введите пароль")
            textView4.isVisible = false
            textView4.isClickable = false
            inputMethodManager.hideSoftInputFromWindow(textView2.windowToken, 0)
        }

        signInButton.setOnClickListener {
            val groupName : String?
            val teacherName : String?
            val password : String?

            if (studentBtnClicked){
                var orderInGroup : Int = 0
                groupName = textView2.text.toString()
                if (textView3.text.toString() != "") orderInGroup = (textView3.text.toString()).toInt()
                password = textView4.text.toString()
                if (!(groupName == null || orderInGroup == 0 || orderInGroup > 30)) {
                    loginStudent(groupName, orderInGroup, password)
                } else { Toast.makeText(this, "Проверьте правильность ввода данных", Toast.LENGTH_SHORT).show() }
            } else {
                teacherName = textView2.text.toString()
                password = textView3.text.toString()
                if (teacherName != "" && password != "") {
                    loginTeacher(teacherName, password)
                } else { Toast.makeText(this, "Проверьте правильность ввода данных", Toast.LENGTH_SHORT).show() }
            }
            }
        }

    fun loginStudent(groupName : String, orderInGroup : Int, password : String){
        if(groupName.isNotEmpty() && orderInGroup.toString().isNotEmpty() && password.isNotEmpty()){
            val email = "$groupName-$orderInGroup@student.app"
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        Toast.makeText(this, "Успешный вход", Toast.LENGTH_SHORT).show()
                        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putInt("isTeacherLogged", 0)
                        editor.apply()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { exception ->
                    Toast.makeText(this, "Что-то пошло не так", Toast.LENGTH_SHORT).show()
                    println(exception.toString())
                }
        }
    }
    fun loginTeacher(teacherName : String, password: String){
        if(teacherName.isNotEmpty() && password.isNotEmpty()){
            val email = "$teacherName@teacher.app"
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        Toast.makeText(this, "Успешный вход", Toast.LENGTH_SHORT).show()
                        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putInt("isTeacherLogged", 1)
                        editor.apply()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { exception ->
                    // Обработка ошибки сохранения данных
                    Toast.makeText(this, "Что-то пошло не так", Toast.LENGTH_SHORT).show()
                    println(exception.toString())
                }
        }
    }
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (currentFocus != null) {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
            currentFocus!!.clearFocus()
        }
        return super.dispatchTouchEvent(event)
    }
}

