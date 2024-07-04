package com.example.collegetests

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.ui.AppBarConfiguration
import com.example.collegetests.databinding.ActivityMainBinding
import com.example.collegetests.ui.home.TestsFragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        userId = currentUserEmail?.substringBefore('@').toString()
        val sharedPreferences: SharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val isTeacherLogged = sharedPreferences.getInt("isTeacherLogged", -1) == 1

        if (userId != null && !isTeacherLogged) {
            val userIdTemp = userId!!.split("").toMutableList()
            for (i in userIdTemp.indices) {
                if (!(userIdTemp[i] in arrayListOf<String>("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"))){
                    userIdTemp[i] = userIdTemp[i].uppercase()
                } else { break }
            }
            userId = userIdTemp.joinToString("")
        } else { if (userId != null) userId = capitalizeFirstLetter(userId!!) }


        println(userId + "Хахахахахха")

        val userGroup = userId?.substringBefore('-')
        val userOrder = userId?.substringAfter('-')
        setContentView(R.layout.activity_main)
        val bundle = Bundle()
        bundle.putString("userId", userId)
        bundle.putString("testsType", "given")


        setSupportActionBar(findViewById(R.id.toolbar))
        val actionBar = supportActionBar
        if (!isTeacherLogged) {
            actionBar?.setDisplayHomeAsUpEnabled(true)
            actionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)
            drawerLayout = findViewById(R.id.drawer_layout)
        }
        navView = findViewById(R.id.nav_view)

        val testsFragment = TestsFragment.newInstance(userId!!, "given")
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, testsFragment)
            .commit()

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.given_tests -> {
                    val testsFragment = TestsFragment.newInstance(userId!!, "given")
                    val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                    if (currentFragment != null) {
                        supportFragmentManager.beginTransaction()
                            .remove(currentFragment)
                            .replace(R.id.fragment_container, testsFragment)
                            .commit()
                    }
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    }
                    true
                }
                R.id.solved_tests -> {
                    val testsFragment = TestsFragment.newInstance(userId!!, "solved")
                    val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                    if (currentFragment != null) {
                        supportFragmentManager.beginTransaction()
                            .remove(currentFragment)
                            .replace(R.id.fragment_container, testsFragment)
                            .commit()
                    }
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    }
                    true
                }
                else -> {
                    println("Нажатие не обработано"); false
                }
            }
        }

        val headerView = navView.getHeaderView(0)
        val textView = headerView.findViewById<TextView>(R.id.textView)
        if (isTeacherLogged)
            textView.text = "Преподаватель $userId"
        else
            textView.text = "Группа: $userGroup. Номер: $userOrder"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        menuInflater.inflate(R.menu.activity_main_drawer, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.logout_button -> {
                logOut()
                true
            }
            android.R.id.home -> {
                drawerLayout.openDrawer(GravityCompat.START)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logOut() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }
}



fun downloadSolvedTestsForStudent(userId: String?, callback: (MutableList<SolvedTest?>) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val solvedTestsRaw = mutableListOf<MutableMap<String, Any>>()
    val solvedTests = mutableListOf<SolvedTest?>()
    var testsToLoad = 0
    if (userId != null) {
        db.collection("students").document(userId).get()
            .addOnSuccessListener { document ->

                if (document != null && document.exists()) {
                    val solvedTestsField =
                        document.get("solved_tests") as? MutableList<MutableMap<String, Any>>
                    if (solvedTestsField == null || solvedTestsField.isEmpty()){
                        callback(mutableListOf(null))
                        return@addOnSuccessListener
                    }
                    solvedTestsRaw.addAll(solvedTestsField ?: emptyList())

                    testsToLoad = solvedTestsRaw.size

                    if (testsToLoad == 0) {
                        callback(solvedTests)
                        return@addOnSuccessListener
                    }

                    for (i in solvedTestsRaw.indices) {
                        val testId = solvedTestsRaw[i]["id"].toString()
                        getMistakes(userId, testId) { mistakes ->
                            downloadTest(testId) { test ->
                                if (test != null) {
                                    val newSolvedTest = SolvedTest(test, mistakes)
                                    solvedTests.add(newSolvedTest)
                                }

                                testsToLoad--

                                if (testsToLoad == 0) {
                                    callback(solvedTests)
                                }
                            }
                        }
                    }
                } else {
                    Log.d(ContentValues.TAG, "No such document")
                    callback(mutableListOf(null))
                }
            }
            .addOnFailureListener { exception ->
                Log.d(ContentValues.TAG, "get failed with ", exception)
                callback(mutableListOf(null))
            }
    }
}

fun downloadTestsForTeacher(teacherName: String, callback: (MutableList<MutableMap<String, Any?>?>) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var givenIds: MutableList<String>? = null
    var tests = mutableListOf<Test?>(null)
    val result = mutableListOf<MutableMap<String, Any?>?>()

    db.collection("teachers").document(teacherName).get()
        .addOnSuccessListener {
            if (it.get("given_tests") == null) { callback(mutableListOf(null)); return@addOnSuccessListener }
            givenIds = it.get("given_tests") as MutableList<String>
            tests = MutableList(givenIds!!.size) { null }
            for (i in givenIds!!.indices) {
                downloadTest(givenIds!![i]) { test ->
                    tests[i] = test
                    var studentsSolvedIds: MutableList<String>?

                    db.collection("given_tests").document(givenIds!![i]).get()
                        .addOnSuccessListener { document ->
                            studentsSolvedIds =
                                document.get("students_solved") as MutableList<String>?

                            if (studentsSolvedIds != null) {
                                val mistakesForStudent: MutableList<MutableList<String>?> =
                                    MutableList(studentsSolvedIds!!.size) { null }
                                var mistakesLoaded = 0

                                for (j in studentsSolvedIds!!.indices) {
                                    getMistakes(studentsSolvedIds!![j], givenIds!![i]) { mistakes ->
                                        mistakesForStudent[j] = mistakes
                                        mistakesLoaded++

                                        if (mistakesLoaded == studentsSolvedIds!!.size) {
                                            val testResult = mutableMapOf<String, Any?>()
                                            testResult["test"] = test
                                            testResult["students_solved"] = studentsSolvedIds
                                            testResult["mistakes"] = mistakesForStudent
                                            result.add(testResult)

                                            if (result.size == givenIds!!.size) {
                                                callback(result)
                                            }
                                        }
                                    }
                                }
                            } else {
                                val testResult = mutableMapOf<String, Any?>()
                                testResult["test"] = test
                                testResult["students_solved"] = null
                                testResult["mistakes"] = null
                                result.add(testResult)

                                if (result.size == givenIds!!.size) {
                                    callback(result)
                                }
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.d(ContentValues.TAG, "get failed with ", exception)
                            callback(mutableListOf(null))
                        }
                }
            }
        }
        .addOnFailureListener { exception ->
            Log.d(ContentValues.TAG, "get failed with ", exception)
            callback(mutableListOf(null))
        }
}

fun downloadUnsolvedTestsForStudent(userId: String?, callback: (MutableList<Test?>) -> Unit) {
    println(userId + "userId")
    val database = FirebaseFirestore.getInstance()
    var unsolvedIds: MutableList<String>?
    val tests = mutableListOf<Test?>()
    if (userId != null) {
        println(userId.toString() + "Success")
        val doc = database.collection("students").document(userId)
        println("Hui")
        doc.get()
            /*.addOnSuccessListener {
                println(it.get("unsolved_tests") as? MutableList<String>)
            }*/
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    println(document.toString() + "Data")
                    unsolvedIds = document?.get("unsolved_tests") as? MutableList<String>
                    println(userId)
                    println(unsolvedIds)
                    if (unsolvedIds != null && unsolvedIds!!.isNotEmpty()) {
                        val idsToDownload = unsolvedIds!!.toList()
                        var testsDownloaded = 0
                        val downloadedTests = mutableListOf<Test?>()
                        for (i in idsToDownload.indices) {
                            downloadTest(idsToDownload[i]) { downloadedTest ->
                                downloadedTests.add(downloadedTest)
                                println(i.toString() + downloadedTests.toString())
                                println("ttttt")
                                testsDownloaded++
                                if (testsDownloaded == idsToDownload.size) {
                                    callback(downloadedTests)
                                }
                            }
                        }
                    } else {
                        println("goofy ah")
                        callback(mutableListOf(null))
                        //callback(tests)
                    }
                } else {
                    println("Error occurred")
                    println(task.exception)
                    Log.d(ContentValues.TAG, "get failed with ", task.exception)
                    //callback(tests)
                }
            }
    } else {
        println("userId is null")
        callback(tests)
    }
    println("Completed")
}



fun getMistakes(userId: String?, testId: String, callback: (MutableList<String>?) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("students").document(userId?: "").get()
        .addOnSuccessListener { documentSnapshot ->
            println("СУКсекс")
            val solvedTestsMap = documentSnapshot.get("solved_tests") as? MutableList<MutableMap<String, Any>>
            var solvedTestMap : MutableMap<String, Any>? = null
            if (solvedTestsMap != null) {
                for (i in solvedTestsMap.indices){
                    solvedTestMap = solvedTestsMap[i]
                    if (solvedTestMap["id"] == testId){
                        break
                    }
                }
                    println(solvedTestsMap)
            }
            if (solvedTestMap!= null) {
                val mistakesMap = solvedTestMap["mistakes"] as? MutableMap<String, Any>
                if (mistakesMap!= null) {
                    val answers = mistakesMap["answers"] as? List<String>
                    val positions = mistakesMap["positions"] as? List<Int>
                    val mistakes = mutableListOf<String>()
                    if (positions!= null && answers!= null) {
                        for (j in positions.indices) {
                            mistakes.add("${positions[j]}" + "/" + answers[positions[j]])
                        }
                    }
                    callback(mistakes)
                } else {
                    callback(mutableListOf())
                }
            } else {
                callback(mutableListOf())
            }
        }
        .addOnFailureListener { exception ->
            println(exception)
            callback(null)
        }
}
fun capitalizeFirstLetter(str: String): String {
    if (str.isEmpty()) {
        return str
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1)
}