package com.example.collegetests

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class ViewingMistakesActivity : AppCompatActivity() {

    private lateinit var test: Test
    private lateinit var mistakes: MutableList<String>
    private lateinit var mistakesToUse: MutableList<MutableMap<String, *>>
    private lateinit var enteredKeys: MutableList<MutableList<Boolean>>
    private lateinit var studentNumberAdapter: StudentNumberAdapter
    private lateinit var studentItemAdapter: StudentItemAdapter
    var currentNumber = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewing_mistakes)
        val questionField: TextView = findViewById(R.id.question_field)
        val goNextButton: Button = findViewById(R.id.go_next_button)
        goNextButton.isEnabled = false
        val goBackButton: Button = findViewById(R.id.go_back_button)
        val finishActivityButton: Button = findViewById(R.id.go_button)

        val userId: String? = intent.getStringExtra("userId")
        val testId: String? = intent.getStringExtra("testId")
        println(testId)
        if (testId != null) {
            downloadTest(testId) { test ->
                if (test != null) {
                    this.test = test
                    println("test " + test)
                    getMistakes(userId, testId, {
                        if (it != null) {
                            if (it != null && !it.isEmpty()) {
                                mistakes = it
                                println("mistakes " + it)
                            } else mistakes = MutableList(test.tasks.size) { "" }
                        }
                        mistakesToUse = MutableList(test.tasks.size) {
                            mutableMapOf(
                                "is_correct" to true,
                                "given_answers" to null
                            )
                        }
                        if (!mistakes.isEmpty()){
                            for (i in mistakes.indices) {
                                if (!mistakes[i].isEmpty()) {
                                    mistakes[i].substringAfter("/").split("|").map { it.toInt() }
                                    mistakesToUse[mistakes[i].substringBefore("/").toInt()] =
                                        mutableMapOf(
                                            "is_correct" to false,
                                            "given_answers" to mistakes[i].substringAfter("/")
                                                .split("|")
                                                .map { it.toInt() }
                                        )
                                }
                            }
                        }
                        val numberOfTasks = test.tasks.size
                        enteredKeys = test.keysList.map { it.toMutableList() }.toMutableList()
                        for (i in enteredKeys.indices) {
                            for (j in enteredKeys[i].indices) {
                                enteredKeys[i][j] = false
                            }
                        }



                        questionField.setHorizontallyScrolling(true)
                        questionField.isSingleLine = false
                        setQuestionText(questionField, test.tasks[0])

                        val studentRecyclerView: RecyclerView =
                            findViewById(R.id.student_recyclerview)
                        studentRecyclerView.layoutManager =
                            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                        studentNumberAdapter = StudentNumberAdapter(
                            questionField,
                            (1..test.tasks.size).toMutableList(),
                            mistakesToUse,
                            onNumberClicked = { position, questionFieldInner ->
                                onNumberClicked(position, questionFieldInner);
                                studentItemAdapter.updateData(
                                    studentItemAdapter,
                                    test.answersList[position],
                                    if ((mistakesToUse[position]["is_correct"].also { println(it.toString() + "also") } as Boolean)) test.keysList[studentNumberAdapter.getSelectedIndex()] else returnGivenAnswers(
                                        (mistakesToUse[position]["given_answers"] as MutableList<Int>),
                                        test.answersList[position].size.also { println(it) },
                                    ),
                                    test.keysList[position]
                                )

                                when (position) {
                                    0 -> {
                                        goBackButton.isEnabled = false; goNextButton.isEnabled =
                                            true
                                    }

                                    numberOfTasks - 1 -> {
                                        goNextButton.isEnabled = false; goBackButton.isEnabled =
                                            true
                                    }

                                    else -> {
                                        goBackButton.isEnabled = true
                                        goNextButton.isEnabled = true
                                    }
                                }
                            },
                            showToast = { message ->
                                Toast.makeText(
                                    this,
                                    message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                        studentRecyclerView.adapter = studentNumberAdapter


                        val studentItemRecyclerView: RecyclerView =
                            findViewById(R.id.student_items_recycler_view)
                        studentItemRecyclerView.layoutManager = LinearLayoutManager(this)
                        studentItemAdapter = StudentItemAdapter(
                            studentItemRecyclerView,
                            test.answersList[0],
                            if ((mistakesToUse[0]["is_correct"].also { println(it.toString() + "also") } as Boolean)) test.keysList[0] else returnGivenAnswers(
                                (mistakesToUse[0]["given_answers"] as MutableList<Int>),
                                test.answersList[0].size.also { println(it) },
                            ),
                            test.keysList[0],
                        )
                        studentItemRecyclerView.adapter = studentItemAdapter
                    })
                }
            }
        } else {
            Toast.makeText(this, "Ошибка загрузки теста, возврат в меню...", Toast.LENGTH_SHORT)
                .show(); finish()
        }

        goBackButton.isEnabled = false

        goNextButton.setOnClickListener {
            studentNumberAdapter.goNext()
            if (studentNumberAdapter.getSelectedIndex() >= studentNumberAdapter.getNumbers().size - 1) {
                it.isEnabled = false
            }
            questionField.text = test.tasks[studentNumberAdapter.getSelectedIndex()]
            studentItemAdapter.updateData(
                studentItemAdapter,
                test.answersList[studentNumberAdapter.getSelectedIndex()],
                if (!(mistakesToUse[0]["is_correct"] as Boolean)) test.keysList[studentNumberAdapter.getSelectedIndex()] else returnGivenAnswers(
                    (mistakesToUse[studentNumberAdapter.getSelectedIndex()]["given_answers"] as MutableList<Int>),
                    test.answersList[0].size,
                ),
                test.keysList[studentNumberAdapter.getSelectedIndex()]/*enteredKeys[studentNumberAdapter.getSelectedIndex()]*/
            )
            goBackButton.isEnabled = true
        }
        goBackButton.setOnClickListener {
            studentNumberAdapter.goBack()
            if (studentNumberAdapter.getSelectedIndex() == 0) {
                it.isEnabled = false
            }
            questionField.text = test.tasks[studentNumberAdapter.getSelectedIndex()]
            studentItemAdapter.updateData(
                studentItemAdapter,
                test.answersList[studentNumberAdapter.getSelectedIndex()],
                if (!(mistakesToUse[0]["is_correct"] as Boolean)) test.keysList[studentNumberAdapter.getSelectedIndex()] else returnGivenAnswers(
                    (mistakesToUse[studentNumberAdapter.getSelectedIndex()]["given_answers"] as MutableList<Int>),
                    test.answersList[0].size,
                ),
                test.keysList[studentNumberAdapter.getSelectedIndex()]/*enteredKeys[studentNumberAdapter.getSelectedIndex()]*/
            )
            goNextButton.isEnabled = true
        }
        finishActivityButton.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("Закрыть окно")
            dialog.setMessage("Закрыть окно просмотра ошибок?")
            dialog.setPositiveButton("Да") { _, _ ->
                finish()
            }
            dialog.setNegativeButton("Нет") { _, _ -> }
            dialog.show()
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        //do nothing
    }

    fun onNumberClicked(position: Int, questionField: TextView) {
        questionField.setText(test.tasks[position])
        runOnUiThread {
            studentItemAdapter.updateData(
                studentItemAdapter,
                test.answersList[position],
                if (!(mistakesToUse[0]["is_correct"] as Boolean)) test.keysList[position].also {
                    println(it.toString() + "it")
                } else returnGivenAnswers(
                    ( if (mistakesToUse[position]["given_answers"] != null)
                            mistakesToUse[position]["given_answers"] as MutableList<Int> else  mutableListOf()
                            ),
                    test.answersList[0].size.also { println(it.toString() + "сайзик") },
                ),
                test.keysList[position]
            )
        }
    }

    fun setQuestionText(questionField: TextView, questionText: String) {
        val maxLineLength = 23
        val stringBuilder = StringBuilder(questionText)
        var lastSpaceIndex = -1
        var currentIndex = 0

        while (currentIndex < stringBuilder.length) {
            if (stringBuilder[currentIndex] == ' ') {
                lastSpaceIndex = currentIndex
            }

            if (currentIndex - (lastSpaceIndex + 1) >= maxLineLength) {
                if (lastSpaceIndex != -1) {
                    stringBuilder.setCharAt(lastSpaceIndex, 'n')
                    currentIndex = lastSpaceIndex + 1
                    lastSpaceIndex = -1
                }
            }
            currentIndex++
        }

        // Устанавливаем текст в TextView
        questionField.text = stringBuilder.toString()
    }


    private class StudentNumberAdapter(
        private val questionFieldInner: TextView,
        private val numbers: MutableList<Int>,
        private val mistakes: MutableList<MutableMap<String, *>>,
        private val onNumberClicked: (Int, TextView) -> Unit,
        private val showToast: (String) -> Unit,
    ) :
        RecyclerView.Adapter<StudentNumberAdapter.NumberViewHolder>() {

        private var selectedIndex = 0;
        private var previousColor = if (mistakes[0]["is_correct"] == true) {
            R.color.correct
        } else {
            R.color.incorrect
        }

        fun getSelectedIndex(): Int {
            return selectedIndex
        }

        fun getNumbers(): MutableList<Int> {
            return numbers
        }

        class NumberViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumberViewHolder {
            val textView = LayoutInflater.from(parent.context)
                .inflate(R.layout.horizontal_list_item, parent, false) as TextView
            return NumberViewHolder(textView)
        }


        override fun onBindViewHolder(holder: NumberViewHolder, position: Int) {
            holder.textView.text = numbers[position].toString()

            holder.textView.setBackgroundColor(
                if (selectedIndex == position) {
                    if (mistakes[position]["is_correct"] == true) {
                        Color.parseColor("#006400")
                    } else {
                        Color.parseColor("#640000")
                    }
                } else if (mistakes[position]["is_correct"] == true) {
                    ContextCompat.getColor(holder.itemView.context, R.color.correct)
                } else {
                    ContextCompat.getColor(holder.itemView.context, R.color.incorrect)
                }
            )
            holder.textView.setOnClickListener {

                val adapterPosition = holder.bindingAdapterPosition
                if (adapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener


                val previousIndex = selectedIndex
                selectedIndex = adapterPosition
                notifyItemChanged(previousIndex)
                notifyItemChanged(adapterPosition)


                onNumberClicked(position, questionFieldInner)
            }
        }

        override fun getItemCount() = numbers.size
        fun goNext() {
            selectedIndex++
            if (selectedIndex >= numbers.size) {
                selectedIndex = numbers.size - 1
            }
            notifyItemChanged(selectedIndex - 1)
            notifyItemChanged(selectedIndex)
        }

        fun goBack() {
            selectedIndex--
            if (selectedIndex < 0) {
                selectedIndex = 0
            }
            notifyItemChanged(selectedIndex + 1)
            notifyItemChanged(selectedIndex)
        }
    }


    private class StudentItemAdapter(
        private val itemsRecyclerView: RecyclerView,
        private var items: MutableList<String>,
        private var checkBoxes: MutableList<Boolean>,
        private var keys: MutableList<Boolean>,
        //private var mistakes: MutableList<Boolean>
    ) : RecyclerView.Adapter<StudentItemAdapter.ItemViewHolder>() {

        fun getItems(): MutableList<String> {
            return items
        }

        fun getCheckBoxes(): MutableList<Boolean> {
            return checkBoxes
        }

        private val selectedItems = HashSet<Int>()

        class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val checkBox: CheckBox = view.findViewById(R.id.check_box_to_show)
            val editText: TextView = view.findViewById(R.id.text_view_to_show)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.showing_answer_item, parent, false)
            return ItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            println(position.toString() + "It's position")
            println(checkBoxes.toString() + "checkboxes")
            println(keys.toString() + "keys")
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                val correctPosition = adapterPosition
                if (correctPosition < items.size && correctPosition < keys.size) {
                    holder.checkBox.isEnabled = false

                    holder.editText.setText(items[correctPosition])
                    if (keys[position])
                        holder.editText.setTextColor(
                            ContextCompat.getColor(
                                holder.itemView.context,
                                R.color.correct
                            )
                        )
                    else
                        holder.editText.setTextColor(
                            ContextCompat.getColor(
                                holder.itemView.context,
                                R.color.incorrect
                            )
                        )
                    if (checkBoxes[correctPosition]) {
                        if (keys[correctPosition]) {
                            holder.checkBox.isChecked = true
                            holder.checkBox.setBackgroundColor(
                                ContextCompat.getColor(
                                    holder.itemView.context,
                                    R.color.correct
                                )
                            )
                        } else {
                            holder.checkBox.isChecked = true
                            holder.checkBox.setBackgroundColor(
                                ContextCompat.getColor(
                                    holder.itemView.context,
                                    R.color.incorrect
                                )
                            )
                        }
                    } else {
                        holder.checkBox.isChecked =
                            false
                        if (keys[correctPosition])
                            holder.checkBox.background = ContextCompat.getDrawable(
                                holder.itemView.context,
                                R.drawable.checkbox_border_correct
                            )
                        else
                            holder.checkBox.background = ContextCompat.getDrawable(
                                holder.itemView.context,
                                R.drawable.checkbox_border_incorrect
                            )

                    }


                } else {

                    Toast.makeText(
                        holder.itemView.context,
                        "Error: Position out of bounds",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }




        override fun getItemCount(): Int = items.size


        @SuppressLint("NotifyDataSetChanged")
        fun updateData(adapter: StudentItemAdapter, newItems: MutableList<String>, newCheckBoxes: MutableList<Boolean>, newKeys: MutableList<Boolean>) {

            adapter.items = newItems
            adapter.checkBoxes = newCheckBoxes
            adapter.keys = newKeys

            adapter.notifyDataSetChanged()
        }
    }

}

fun returnGivenAnswers (positions : MutableList<Int>, length : Int) : MutableList<Boolean>{
    val answers = MutableList(length){ false }
    positions.forEach {
        answers[it] = true
    }
    return answers
}


