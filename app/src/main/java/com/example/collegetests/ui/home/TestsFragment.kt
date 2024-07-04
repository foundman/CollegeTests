package com.example.collegetests.ui.home

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.collegetests.StartSolvingTestDialogFragment
import com.example.collegetests.R
import com.example.collegetests.SolvedTest
import com.example.collegetests.SolvingTestActivity
import com.example.collegetests.Test
import com.example.collegetests.ViewingMistakesActivity
import com.example.collegetests.downloadSolvedTestsForStudent
import com.example.collegetests.downloadTestsForTeacher
import com.example.collegetests.downloadUnsolvedTestsForStudent
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TestsFragment : Fragment() {

    companion object {
        fun newInstance(userId: String, testsType: String): TestsFragment {
            val fragment = TestsFragment()
            val args = Bundle()
            args.putString("userId", userId)
            args.putString("testsType", testsType)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var userId : String
    private lateinit var testsType : String

    private lateinit var students : MutableList<MutableList<String>>
    private lateinit var mistakesCounts : MutableList<MutableList<Int>>
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bundle = arguments
        println("Хуйуйуй" + arguments?.getString("userId").toString() + arguments)
        if (bundle != null){
            userId = arguments?.getString("userId").toString()
            testsType = arguments?.getString("testsType").toString()
        }
        return inflater.inflate(R.layout.fragment_tests, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val bundle = arguments
        sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bundle = arguments
        if (bundle != null){
            userId = arguments?.getString("userId").toString()
            testsType = arguments?.getString("testsType").toString()
        }

        val isTeacherLogged = sharedPreferences.getInt("isTeacherLogged", -1) == 1
        val textHome = view.findViewById<TextView>(R.id.text_home)
        val textHome2 = view.findViewById<TextView>(R.id.text_home2)
        textHome.text = "Идёт загрузка тестов..."

        if (!isTeacherLogged){
            if (testsType == "given") {
                val unsolvedTestsRecyclerView: RecyclerView =
                    view.findViewById(R.id.student_unsolved_tests_recycler_view)
            } else { val unsolvedTestsRecyclerView: RecyclerView =
                view.findViewById(R.id.student_unsolved_tests_recycler_view) }
        } else { val unsolvedTestsRecyclerView: RecyclerView =
            view.findViewById(R.id.student_unsolved_tests_recycler_view) }
        val testsRecyclerView: RecyclerView =
            view.findViewById(R.id.student_unsolved_tests_recycler_view)
        testsRecyclerView.layoutManager = LinearLayoutManager(context)
        lateinit var unsolvedTests : MutableList<Test?>// = mutableListOf(null)
        lateinit var solvedTests : MutableList<SolvedTest?>
        lateinit var teacherTests : MutableList<MutableMap<String, Any?>?>
        userId = arguments?.getString("userId").toString()
        testsType = arguments?.getString("testsType").toString()
        println(userId + "fragment userId")
        //textHome2.text = testsType
        fun setupView(){
            if (!isTeacherLogged){
            if (testsType == "given") {
                downloadUnsolvedTestsForStudent(userId, {
                    unsolvedTests = it
                    if (it != mutableListOf(null)){
                        println("fragment" + unsolvedTests)
                        val testIds = MutableList(unsolvedTests.size) { "" }
                        val teacherIds = MutableList(unsolvedTests.size) { "" }
                        val subjects = MutableList(unsolvedTests.size) { "" }
                        val topics = MutableList(unsolvedTests.size) { "" }
                        val numbersOfTasks = MutableList(unsolvedTests.size) { 0 }
                        for (i in unsolvedTests.indices) {
                            testIds[i] = unsolvedTests[i]?.id.toString()
                            teacherIds[i] = unsolvedTests[i]?.teacherId.toString()
                            subjects[i] = unsolvedTests[i]?.subject.toString()
                            topics[i] = unsolvedTests[i]?.topic.toString()
                            numbersOfTasks[i] = unsolvedTests.size
                        }
                        testsRecyclerView.adapter = TestsRecyclerViewAdapter(
                            0,
                            testIds,
                            subjects,
                            topics,
                            numbersOfTasks,
                            ExclusiveTestData.StudentUnsolvedData(teacherIds)
                        ) { testId, topic, type, position -> onItemClicked(testId, topic, 0, position) }
                        textHome.text = ""
                    } else {
                        textHome.text = "Новых тестов нет"
                    }
                })
            } else {
                downloadSolvedTestsForStudent(userId, {
                    solvedTests = it
                    if (it != mutableListOf(null)) {
                        val testIds = MutableList(solvedTests.size) { "" }
                        val teacherIds = MutableList(solvedTests.size) { "" }
                        val subjects = MutableList(solvedTests.size) { "" }
                        val topics = MutableList(solvedTests.size) { "" }
                        val mistakes = MutableList(solvedTests.size) { mutableListOf("") }
                        val numbersOfTasks = MutableList(solvedTests.size) { 0 }
                        for (i in solvedTests.indices) {
                            testIds[i] = solvedTests[i]?.test?.id.toString()
                            teacherIds[i] = solvedTests[i]?.test?.teacherId.toString()
                            subjects[i] = solvedTests[i]?.test?.subject.toString()
                            topics[i] = solvedTests[i]?.test?.topic.toString()
                            mistakes[i] = solvedTests[i]?.mistakes ?: mutableListOf("")
                            numbersOfTasks[i] = solvedTests[i]?.test?.tasks?.size!!
                        }
                        testsRecyclerView.adapter = TestsRecyclerViewAdapter(
                            1,
                            testIds,
                            subjects,
                            topics,
                            numbersOfTasks,
                            ExclusiveTestData.StudentSolvedData(mistakes, teacherIds)
                        ) { testId, topic, type, position -> onItemClicked(testId, topic, 1, position) }
                        textHome.text = ""
                    } else {
                        textHome.text = "Решённых тестов нет"
                    }
                })
            }
        } else {
            downloadTestsForTeacher(userId, {
                teacherTests = it
                if (teacherTests != mutableListOf(null)){
                    val testIds = MutableList(teacherTests.size) { "" }
                    val subjects = MutableList(teacherTests.size) { "" }
                    val topics = MutableList(teacherTests.size) { "" }
                    val numbersOfTasks = MutableList(teacherTests.size) { 0 }
                    val studentsSolved : MutableList<MutableList<String>> = MutableList(teacherTests.size) { mutableListOf() }

                    val mistakes : MutableList<MutableList<MutableList<String>>> = MutableList(teacherTests.size) { mutableListOf(
                        mutableListOf()) }
                    for (i in teacherTests.indices) {
                        val test = teacherTests[i]?.get("test") as Test
                        testIds[i] = test.id
                        subjects[i] = test.subject
                        topics[i] = test.topic
                        numbersOfTasks[i] = test.tasks.size
                        if (teacherTests[i]?.get("students_solved") != null)
                            studentsSolved[i] = (teacherTests[i]?.get("students_solved") as? MutableList<String>) ?: mutableListOf()
                        mistakes[i] = (teacherTests[i]?.get("mistakes") as? MutableList<MutableList<String>>) ?: mutableListOf(mutableListOf())
                    }
                    testsRecyclerView.adapter = TestsRecyclerViewAdapter(
                        2,
                        testIds,
                        subjects,
                        topics,
                        numbersOfTasks,
                        ExclusiveTestData.TeacherData(mistakes, studentsSolved)
                    ) { testId, topic, type, position -> onItemClicked(testId, topic, 2, position) }
                    textHome.text = ""
                    students = studentsSolved
                    println(students.toString() + " s")
                    mistakesCounts = MutableList(teacherTests.size){ mutableListOf()}
                    println(mistakesCounts)
                    for (i in mistakesCounts.indices){
                        mistakesCounts[i] = MutableList(students[i].size){ 0 }
                        for (j in students[i].indices){
                            //mistakesCounts[i] = MutableList(j){ 0 }
                            mistakesCounts[i][j] = mistakes[i][j].size
                        }
                    }
                    println(students.toString() + " a " + mistakesCounts.toString())
                } else {
                    textHome.text = "Выданных тестов нет"
                }
            })
        }}
        setupView()

        /*println("fragment" + unsolvedTests)
        val testIds = MutableList(unsolvedTests.size){""}
        val subjects = MutableList(unsolvedTests.size){""}
        val topics = MutableList(unsolvedTests.size){""}
        val numbersOfTasks = MutableList(unsolvedTests.size){0}
        for (i in unsolvedTests.indices){
            testIds[i] = unsolvedTests[i]?.id.toString()
            subjects[i] = unsolvedTests[i]?.subject.toString()
            topics[i] = unsolvedTests[i]?.topic.toString()
            numbersOfTasks[i] = 0
        }*/
        //val testsRecyclerViewAdapter = TestsRecyclerViewAdapter(mutableListOf("Ххууйй", "Пиздаа"), mutableListOf("ФИЗИК$testsType", "ХИМИЯ"), mutableListOf("Строение атома", "Гибридизация орбиталей"), mutableListOf(12, 15), mutableListOf()){ testId, topic -> onItemClicked(testId, topic)}

        //unsolvedTestsRecyclerView.adapter = testsRecyclerViewAdapter
        val fab : FloatingActionButton = view.findViewById(R.id.create_test_fab)
        val fab2 : FloatingActionButton = view.findViewById(R.id.update_fab)
        fab2.setOnClickListener{
            setupView()
        }
        if (isTeacherLogged){
            fab.setOnClickListener{
                val dialog = StartSolvingTestDialogFragment(userId)
                dialog.show(parentFragmentManager, "MyCustomDialogFragment")
            }
        } else { fab.isEnabled = false; fab.isVisible = false }
    }
    override fun onDestroyView() {
        super.onDestroyView()
    }

    fun onItemClicked(testId: String, topic: String, type: Int, position : Int){
        when (type){
            0 -> {
                println(userId)
                val dialog = StartSolvingTestDialogFragment().apply {
            arguments = Bundle().apply {
                putString("userId", userId)
                putString("testId", testId)
                putString("topic", topic)
            }
        }
            dialog.show(parentFragmentManager, "StartSolvingTestDialogFragment");}
            1 -> {
                val dialog = AlertDialog.Builder(requireContext())
                dialog.setTitle("Закрыть окно")
                dialog.setMessage("Посмотреть результаты?")
                dialog.setPositiveButton("Да") { _, _ ->
                    val intent = Intent(requireContext(), ViewingMistakesActivity::class.java).apply {
                        putExtra("userId", userId)
                        putExtra("testId", testId)
                    }
                    startActivity(intent)
                }
                dialog.setNegativeButton("Нет") { _, _ -> }
                dialog.show() }
            2 -> {
                val dialog = TeacherTestResultsDialogFragment(students, mistakesCounts, position).apply {
                    arguments = Bundle().apply {
                        putString("userId", userId)
                        putString("testId", testId)
                    }
                }
                dialog.show(parentFragmentManager, "TeacherTestResultsDialogFragment")
            }
        }

    }

}

class TeacherTestResultsDialogFragment(val students : MutableList<MutableList<String>>, val mistakesCounts : MutableList<MutableList<Int>>, val position : Int) : DialogFragment() {
    private lateinit var userId: String
    private lateinit var testId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString("userId", "")
            testId = it.getString("testId", "")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.teacher_test_results_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.teacher_test_results_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Assume you have a list of students who solved the test, with their mistakes count
        students
        val results : MutableList<MutableMap<String, Any>> = MutableList(students[position].size){ mutableMapOf() }
        for (i in results.indices){
            results[i] = mutableMapOf(
                "userId" to students[position][i],
                "mistakesCount" to mistakesCounts[position][i]
            )
        }
        /*val students = listOf(
            Student("Student 1", 3),
            Student("Student 2", 2),
            Student("Student 3", 1)
        )*/

        recyclerView.adapter = TeacherTestResultsAdapter(results) { student ->
            val intent = Intent(activity, ViewingMistakesActivity::class.java).apply {
                putExtra("userId", student["userId"].toString())
                putExtra("testId", testId)
                //putExtra("studentId", student.id)
            }
            startActivity(intent)
        }
    }
    class TeacherTestResultsAdapter(private val results: MutableList<MutableMap<String, Any>>, private val onItemClick: (student: MutableMap<String, Any>) -> Unit) : RecyclerView.Adapter<TeacherTestResultsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.student_result_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val result = results[position]
            holder.studentIdTextView.text = result["userId"] as String
            holder.mistakesCountTextView.text = "${result["mistakesCount"] as Int} ошибок"
            holder.itemView.setOnClickListener { onItemClick(result) }
        }

        override fun getItemCount(): Int = results.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val studentIdTextView: TextView = itemView.findViewById(R.id.student_id)
            val mistakesCountTextView: TextView = itemView.findViewById(R.id.mistakes_count)
        }
    }
}
class StartSolvingTestDialogFragment() : DialogFragment() {
    private lateinit var userId: String
    private lateinit var testId: String
    private lateinit var topic: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString("userId", "")
            testId = it.getString("testId", "")
            topic = it.getString("topic", "")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.start_solving_test_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val topicTextView = view.findViewById<TextView>(R.id.show_topic)
        topicTextView.text = "Тема: " + topic
        view.findViewById<Button>(R.id.go_button).setOnClickListener {
            val intent = Intent(activity, SolvingTestActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("testId", testId)
            }
            startActivity(intent)
            dismiss()
        }

        view.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            dismiss()
        }
    }

}
