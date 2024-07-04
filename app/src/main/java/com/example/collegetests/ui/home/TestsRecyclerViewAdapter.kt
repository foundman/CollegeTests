package com.example.collegetests.ui.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.collegetests.R

/*ЗАГРУЖАТЬ ФАМИЛИЮ УЧИТЕЛЯ*/
sealed class ExclusiveTestData {
    class TeacherData(
        val mistakes: MutableList<MutableList<MutableList<String>>>,
        val studentsSolved: MutableList<MutableList<String>>,
    ) : ExclusiveTestData()

    class StudentSolvedData(
        val mistakes: MutableList<MutableList<String>>,
        val teacherIds: MutableList<String>
    ) : ExclusiveTestData()

    class StudentUnsolvedData(
        val teacherIds: MutableList<String>
    ) : ExclusiveTestData()
}

class TestsRecyclerViewAdapter(
    private val type: Int,
    private var testIds: MutableList<String>,
    private var subjects: MutableList<String>,
    private var topics: MutableList<String>,
    private var numbersOfTasks: MutableList<Int>,
    private var exclusiveTestData: ExclusiveTestData,
    private val onItemClicked: (String, String, Int, Int) -> Unit
) : RecyclerView.Adapter<TestsRecyclerViewAdapter.TestsViewHolder>() {

    private var mistakes: Any? = null
    private var studentsSolved: MutableList<MutableList<String>>? = null
    private var teacherIds: MutableList<String>? = null

    init {
        val testData = exclusiveTestData
        when (testData) {
            is ExclusiveTestData.TeacherData -> {
                mistakes = testData.mistakes
                studentsSolved = testData.studentsSolved
            }
            is ExclusiveTestData.StudentSolvedData -> {
                mistakes = testData.mistakes
                teacherIds = testData.teacherIds
            }
            is ExclusiveTestData.StudentUnsolvedData -> {
                teacherIds = testData.teacherIds
            }
            else -> throw IllegalArgumentException("Unknown type")
        }
    }

    class TestsViewHolder(itemView: View, type: Int) : RecyclerView.ViewHolder(itemView) {
        val subjectTextView: TextView = itemView.findViewById(R.id.subject)
        val topicTextView: TextView = itemView.findViewById(R.id.topic)
        val descriptionTextView: TextView = itemView.findViewById(R.id.description)

        val teacherTextView: TextView? = if (type == 0 || type == 1) itemView.findViewById(R.id.teacher) else null
        val mistakesTextView: TextView? = if (type == 1) itemView.findViewById(R.id.mistakes) else null
    }


    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestsViewHolder {
        val view: View
        view = when (type) {
            0 -> LayoutInflater.from(parent.context).inflate(R.layout.student_unsolved_test_item, parent, false)
            1 -> LayoutInflater.from(parent.context).inflate(R.layout.student_solved_test_item, parent, false)
            2 -> LayoutInflater.from(parent.context).inflate(R.layout.teacher_test_item, parent, false)
            else -> throw IllegalArgumentException("Unknown type")
        }
        return TestsViewHolder(view, type)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: TestsViewHolder, position: Int) {
        /*when (type){
            0 -> ;
            1 -> ;
            2 -> ;
        }*/
        val testId = testIds[position]
        val subject = subjects[position]
        val topic = topics[position]
        val numberOfTasks = numbersOfTasks[position]
        holder.subjectTextView.text = subject
        holder.topicTextView.text = topic
        holder.descriptionTextView.text = "Тест по теме $topic, $numberOfTasks задач"

        // Отображение ошибок (если необходимо)
        when (type) {
            1 -> {
                // Вывод количества ошибок из `exclusiveTestData.mistakes`
                holder.mistakesTextView?.text = "Ошибки: ${(mistakes as MutableList<MutableList<String>>)[position].size}"
            }
            /*else -> {
                holder.mistakesTextView.visibility = View.GONE
            }*/
        }

        holder.itemView.setOnClickListener {
            onItemClicked(testId, subject, type, position)
        }
    }

    override fun getItemCount(): Int {
        return testIds.size
    }
    /*@SuppressLint("NotifyDataSetChanged")
    fun updateData(
        newTestIds: MutableList<String>,
        newSubjects: MutableList<String>,
        newTopics: MutableList<String>,
        newNumbersOfTasks: MutableList<Int>,
        newMistakes: MutableList<MutableList<String>>
    ) {
        testIds = newTestIds
        subjects = newSubjects
        topics = newTopics
        numbersOfTasks = newNumbersOfTasks
        mistakes = newMistakes
        notifyDataSetChanged()
        // Ensure that mistakes list has the same size as the other lists
        while (mistakes.size < subjects.size) {
            mistakes.add(mutableListOf())
        }
    }*/
}


