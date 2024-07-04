package com.example.collegetests

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.collegetests.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivitySignUpBinding
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        mBinding = ActivitySignUpBinding.inflate(LayoutInflater.from(this))
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        val signUpStudentButton:Button = findViewById(R.id.signup_asStudentButton)
        val signUpTeacherButton:Button = findViewById(R.id.signup_asTeacherButton)
        val signUpButton: Button = findViewById(R.id.signup_button)
        val switchButton: Button = findViewById(R.id.switch_button)

        val textView2:EditText = findViewById(R.id.textView2)
        val textView3:EditText = findViewById(R.id.textView3)
        val textView4:EditText = findViewById(R.id.textView4)
        val textView5:EditText = findViewById(R.id.textView5)


        var studentButtonClicked:Boolean = true

        switchButton.setOnClickListener{
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
        signUpStudentButton.setOnClickListener {
            studentButtonClicked = true
            signUpTeacherButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple_500)));
            signUpTeacherButton.setTextColor((ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))))
            signUpStudentButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.light_grey)));
            signUpStudentButton.setTextColor((ColorStateList.valueOf(ContextCompat.getColor(this, R.color.grey))))

            textView3.inputType = InputType.TYPE_CLASS_NUMBER
            println(textView4.inputType.toString())
            textView2.setText(null)
            textView3.setText(null)
            textView4.setText(null)
            textView5.setText(null)

            textView2.setHint("Введите код группы,\n к примеру, ПР113п") //"Введите код группы, к примеру, ПР113п"
            textView3.setHint("Введите Ваш поряд. номер\n на момент регистрации")
            textView4.setHint("Создайте пароль (мин. 8 симв.)")
            textView5.isVisible = true
            textView5.isClickable = true
            inputMethodManager.hideSoftInputFromWindow(textView2.windowToken, 0)//хз что я сделал
        }
        signUpTeacherButton.setOnClickListener {

            studentButtonClicked = false
            signUpStudentButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple_500)));
            signUpStudentButton.setTextColor((ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))))
            signUpTeacherButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.light_grey)));
            signUpTeacherButton.setTextColor((ColorStateList.valueOf(ContextCompat.getColor(this, R.color.grey))))

            textView3.inputType = 129
            textView2.setText(null)
            textView3.setText(null)
            textView4.setText(null)
            textView5.setText(null)

            textView2.setHint("Введите ФИО преподавателя,\n к примеру, Фадеева Е.Н.")
            textView3.setHint("Создайте пароль")
            textView4.setHint("Подтвердите пароль")
            textView5.isVisible = false
            textView5.isClickable = false
            inputMethodManager.hideSoftInputFromWindow(textView2.windowToken, 0) //оно работает
        }

        signUpButton.setOnClickListener {
            val groupName : String?
            val teacherName : String?
            val password : String?

            if (studentButtonClicked){
                var orderInGroup : Int = 0
                groupName = textView2.text.toString()
                if (textView3.text.toString() != "") orderInGroup = (textView3.text.toString()).toInt()
                password = textView4.text.toString()

                //Start ProgressBar first (Set visibility VISIBLE)
                if (!(groupName == null || orderInGroup == 0 || orderInGroup > 30 || password.length < 8 || password != textView5.text.toString())) {
                    registerStudent(groupName, orderInGroup, password,
                        {
                            if (it) {
                                switchButton.isEnabled = false
                                signUpStudentButton.isEnabled = false
                                signUpButton.isEnabled = false
                                signUpTeacherButton.isEnabled = false
                                startActivity(Intent(this, SignInActivity::class.java))
                                finish()
                            }
                        }
                    )
                    val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putInt("isTeacherLogged", 0)
                    editor.apply()
                } else { Toast.makeText(this, "Проверьте правильность ввода данных", Toast.LENGTH_SHORT).show() }
            } else {
                teacherName = textView2.text.toString()
                password = textView3.text.toString()
                if (!(teacherName == null || password.length < 8 || password != textView4.text.toString())) {
                    registerTeacher(teacherName, password,{
                        if (it) {
                            switchButton.isEnabled = false
                            signUpStudentButton.isEnabled = false
                            signUpButton.isEnabled = false
                            signUpTeacherButton.isEnabled = false
                            startActivity(Intent(this, SignInActivity::class.java))
                            finish()
                        }
                    })
                    val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putInt("isTeacherLogged", 1)
                    editor.apply()
                } else { Toast.makeText(this, "Проверьте правильность ввода данных", Toast.LENGTH_SHORT).show() }
            }
        }

    }
    fun registerStudent(groupName : String, orderInGroup : Int, password : String, callback : (Boolean) -> Unit){
        if (groupName.isNotEmpty() && orderInGroup.toString().isNotEmpty() && password.isNotEmpty()){
            val student_id = "$groupName-$orderInGroup"
            val email = "$student_id@student.app"
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = task.result?.user?.uid
                        if (userId != null) {
                            val studentMap = hashMapOf(
                                "student_id" to student_id,
                                "group_name" to groupName,
                                "order_in_group" to orderInGroup,
                                "unsolved_tests" to mutableListOf<String>()
                            )
                            FirebaseFirestore.getInstance().collection("students").document(student_id)
                                .set(studentMap)
                                .addOnSuccessListener {
                                    println("Student uploaded successfully")
                                }
                                .addOnFailureListener {
                                    println("Student uploading issue")
                                }
                        }
                        Toast.makeText(this, "Успешная регистрация", Toast.LENGTH_SHORT).show()
                        callback(true)
                    } else {
                        Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show()
                    }
                    }.addOnFailureListener { exception ->
                        Toast.makeText(this, "Что-то пошло не так", Toast.LENGTH_SHORT).show()
                        println(exception.toString())
                }

        } else { Toast.makeText(this, "Проверьте правильность введённых данных", Toast.LENGTH_SHORT).show() }
    }

    fun registerTeacher(teacherName : String, password : String, callback : (Boolean) -> Unit) {
        if (teacherName.isNotEmpty() && password.isNotEmpty()) {
            val email = "$teacherName@teacher.app"
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = task.result?.user?.uid
                        if (userId != null) {
                            val teacherMap = hashMapOf(
                                "teacher_name" to teacherName,
                                "given_tests" to mutableListOf<String>()
                            )
                            FirebaseFirestore.getInstance().collection("teachers").document(teacherName)
                                .set(teacherMap)
                                .addOnSuccessListener {
                                    println("Teacher uploaded successfully")
                                }
                                .addOnFailureListener {
                                    println("Teacher uploading issue")
                                }
                        }
                        Toast.makeText(this, "Успешная регистрация", Toast.LENGTH_SHORT).show()
                        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putInt("isTeacherLogged", 1)
                        editor.apply()
                        callback(true)

                    } else {
                        Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { exception ->
                    Toast.makeText(this, "Что-то пошло не так", Toast.LENGTH_SHORT).show()
                    println(exception.toString())
                }
        } else { Toast.makeText(this, "Проверьте правильность введённых данных", Toast.LENGTH_SHORT).show() }
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