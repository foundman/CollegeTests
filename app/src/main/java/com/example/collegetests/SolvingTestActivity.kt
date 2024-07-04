package com.example.collegetests

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

data class Test(
    val id: String,
    val teacherId: String,
    val subject: String,
    val topic: String,
    val groups: MutableList<String>,
    val tasks: MutableList<String>,
    val answersList: MutableList<MutableList<String>>,
    val keysList: MutableList<MutableList<Boolean>>
)
data class SolvedTest(
    val test: Test,
    val mistakes: MutableList<String>?
)

class SolvingTestActivity : AppCompatActivity()  {

    private lateinit var test : Test
    private lateinit var enteredKeys : MutableList<MutableList<Boolean>>
    private lateinit var studentNumberAdapter : StudentNumberAdapter
    private lateinit var studentItemAdapter: StudentItemAdapter
    var currentNumber = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solving_test)
        val userId : String? = intent.getStringExtra("userId")
        val testId : String? = intent.getStringExtra("testId")

        val questionField : TextView = findViewById(R.id.question_field)
        val goNextButton : Button = findViewById(R.id.go_next_button)
        goNextButton.isEnabled = false
        val goBackButton : Button = findViewById(R.id.go_back_button)
        goBackButton.isEnabled = false
        val finishTestButton : Button = findViewById(R.id.complete_test_button)
        finishTestButton.isEnabled = false
        println(userId + testId)
        runOnUiThread{
            if (testId != null) {
                downloadTest(testId, {
                    if (it != null) {
                        test = it
                        val numberOfTasks = test.tasks.size
                        enteredKeys = test.keysList.map { it.toMutableList() }.toMutableList()
                        for (i in enteredKeys.indices) {
                            for (j in enteredKeys[i].indices) {
                                enteredKeys[i][j] = false
                            }
                        }
                        println("zzzzzzzz" + test.keysList)



                        questionField.setHorizontallyScrolling(true)
                        questionField.isSingleLine = false
                        setQuestionText(questionField, test.tasks[0])

                        val studentRecyclerView : RecyclerView = findViewById(R.id.student_recyclerview)
                        studentRecyclerView.layoutManager =
                            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                        studentNumberAdapter = StudentNumberAdapter(
                            questionField,
                            (1..numberOfTasks).toMutableList(),
                            onNumberClicked = { position, questionFieldInner -> onNumberClicked(position, questionFieldInner);
                                when (position){
                                    0 -> { goBackButton.isEnabled = false; goNextButton.isEnabled = true }
                                    numberOfTasks - 1 -> { goNextButton.isEnabled = false; goBackButton.isEnabled = true }
                                    else -> {
                                        goBackButton.isEnabled = true
                                        goNextButton.isEnabled = true
                                    }
                                }
                            },
                            showToast = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
                        )
                        studentRecyclerView.adapter = studentNumberAdapter


                        val studentItemRecyclerView : RecyclerView = findViewById(R.id.student_items_recycler_view)
                        studentItemRecyclerView.layoutManager = LinearLayoutManager(this)
                        studentItemAdapter = StudentItemAdapter(
                            studentItemRecyclerView,
                            test.answersList[0],
                            MutableList(test.keysList.size){false}
                        )
                        studentItemRecyclerView.adapter = studentItemAdapter
                        goNextButton.isEnabled = true
                        finishTestButton.isEnabled = true
                    } else Toast.makeText(this, "Ошибка загрузки теста", Toast.LENGTH_LONG).show()
                })
            }
        }




        goBackButton.isEnabled = false

        goNextButton.setOnClickListener {
            studentNumberAdapter.goNext()
            if (studentNumberAdapter.getSelectedIndex() >= studentNumberAdapter.getNumbers().size - 1){
                it.isEnabled = false
            }
            questionField.text = test.tasks[studentNumberAdapter.getSelectedIndex()]
            studentItemAdapter.updateData(test.answersList[studentNumberAdapter.getSelectedIndex()], enteredKeys[studentNumberAdapter.getSelectedIndex()])
            goBackButton.isEnabled = true
            println(test)
        }
        goBackButton.setOnClickListener {
            studentNumberAdapter.goBack()
            if (studentNumberAdapter.getSelectedIndex() == 0){
                it.isEnabled = false
            }
            questionField.text = test.tasks[studentNumberAdapter.getSelectedIndex()]
            studentItemAdapter.updateData(test.answersList[studentNumberAdapter.getSelectedIndex()], enteredKeys[studentNumberAdapter.getSelectedIndex()])
            goNextButton.isEnabled = true
        }
        finishTestButton.setOnClickListener {


            val dialog =
                userId?.let { it1 ->
                    FinishTestDialogFragment(it1, test.id, enteredKeys, test.keysList).apply {

                    }
                }
            if (dialog != null) {
                dialog.show(supportFragmentManager, "FinishTestDialogFragment")
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        //do nothing
    }
    fun onNumberClicked(position: Int, questionField: TextView){
        questionField.setText(test.tasks[position])
        currentNumber = position + 1
        studentItemAdapter.updateData(test.answersList[position], enteredKeys[position])
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


        questionField.text = stringBuilder.toString()
    }



    private class StudentNumberAdapter(
        private val questionFieldInner: TextView,
        private val numbers: MutableList<Int>,
        private val onNumberClicked : (Int, TextView) -> Unit,
        private val showToast: (String) -> Unit
    ) :
        RecyclerView.Adapter<StudentNumberAdapter.NumberViewHolder>() {
        private var flag = false
        private var selectedIndex = 0;
        fun getSelectedIndex() : Int { return selectedIndex }
        fun getNumbers() : MutableList<Int> { return numbers }

        class NumberViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumberViewHolder {
            val textView = LayoutInflater.from(parent.context)
                .inflate(R.layout.horizontal_list_item, parent, false) as TextView
            return NumberViewHolder(textView)
        }

        override fun onBindViewHolder(holder: NumberViewHolder, position: Int) {
            if (!flag) { onNumberClicked(0, questionFieldInner); flag = true }
            holder.textView.text = numbers[position].toString()
            holder.textView.setBackgroundColor(
                if (selectedIndex == position) Color.parseColor("#FF03DAC5")
                else Color.parseColor("#6200EE")
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
        fun goNext(){
            if (selectedIndex != numbers.size - 1) {
                selectedIndex++
                notifyItemChanged(selectedIndex)
                notifyItemChanged(selectedIndex - 1)
            }
        }
        fun goBack(){
            if (selectedIndex != 0){
                selectedIndex--
                notifyItemChanged(selectedIndex)
                notifyItemChanged(selectedIndex + 1)
            }
        }

    }



    private class StudentItemAdapter(
        private val itemsRecyclerView: RecyclerView,
        private var items: MutableList<String>,
        private var checkBoxes: MutableList<Boolean>
    ) : RecyclerView.Adapter<StudentItemAdapter.ItemViewHolder>() {

        fun getItems() : MutableList<String>{ return items }
        fun getCheckBoxes() : MutableList<Boolean>{ return checkBoxes}

        private val selectedItems = HashSet<Int>()

        class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val checkBox: CheckBox = view.findViewById(R.id.check_box_to_show)
            val editText: TextView = view.findViewById(R.id.text_view_to_show)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.showing_answer_item, parent, false)
            return ItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) { try {
            holder.checkBox.setOnCheckedChangeListener(null)
            holder.checkBox.isChecked = checkBoxes[holder.bindingAdapterPosition]
            holder.editText.setText(items[holder.bindingAdapterPosition])

            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedItems.add(holder.bindingAdapterPosition)
                } else {
                    selectedItems.remove(holder.bindingAdapterPosition)
                }
                notifyItemChanged(holder.bindingAdapterPosition)
            }

            holder.checkBox.setOnClickListener{
                holder.checkBox.let {
                    it.isChecked = it.isChecked == false
                }

                checkBoxes[position] = !checkBoxes[position]
                notifyItemChanged(position)
            }





        } catch (ise : IllegalStateException){
            Toast.makeText(holder.itemView.context,"ХуйПизда", Toast.LENGTH_SHORT).show()}}

        override fun getItemCount(): Int = items.size


        @SuppressLint("NotifyDataSetChanged")
        fun updateData(newItems: MutableList<String>, newCheckBoxes: MutableList<Boolean>) {

            this.items = newItems
            this.checkBoxes = newCheckBoxes

            this.notifyDataSetChanged()
        }
    }
}
fun checkTest(enteredKeys : MutableList<MutableList<Boolean>>, keysList: MutableList<MutableList<Boolean>>, callback: (MutableList<Int>, MutableList<MutableList<Int>>) -> Unit){
    val mistakesPositions : MutableList<Int> = mutableListOf()
    val wrongAnswers : MutableList<MutableList<Int>> = MutableList(enteredKeys.size){ mutableListOf()}
    for (i in enteredKeys.indices){
        var flag = false
        for (j in enteredKeys[i].indices){
            if (enteredKeys[i][j] != keysList[i][j]){
                flag = true
                wrongAnswers[i].add(j)
            }
        }; if (flag) mistakesPositions.add(i)
    }
    callback(mistakesPositions, wrongAnswers)
}
fun uploadTest(userID: String, mistakesPositions: MutableList<Int>, testID: String, wrongAnswers: MutableList<MutableList<Int>>) {
    val answers: MutableList<String> = MutableList(wrongAnswers.size) { "" }
    for (i in wrongAnswers.indices) {
        answers[i] = wrongAnswers[i].joinToString("|")
    }
    val db = FirebaseFirestore.getInstance()
    val docRef = db.collection("students").document(userID)
    val newElement = hashMapOf(
        "id" to testID,
        "mistakes" to hashMapOf(
            "answers" to answers,
            "positions" to mistakesPositions
        )
    )

    docRef.update("solved_tests", FieldValue.arrayUnion(newElement))
    docRef.update("unsolved_tests", FieldValue.arrayRemove(testID))
    db.collection("given_tests").document(testID).update("students_solved", FieldValue.arrayUnion(userID))
}
fun downloadTest(id: String, callback: (Test?) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("given_tests").document(id).get()
        .addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val subject = document.getString("subject") ?: ""
                val topic = document.getString("topic") ?: ""
                val teacherId = document.getString("teacher_id") ?: ""
                val groups = document.get("groups") as? List<String> ?: emptyList()
                val tasks = document.get("tasks") as? List<String> ?: emptyList()
                val answers = document.get("answers") as? List<String> ?: emptyList()
                val keys = document.get("keys") as? List<String> ?: emptyList()

                val answersList = answers.map { it.split("|").toMutableList() }
                val keysList = keys.map { it.split("|").map(String::toBoolean).toMutableList() }

                val test = Test(
                    id = id,
                    teacherId = teacherId,
                    subject = subject,
                    topic = topic,
                    groups = groups.toMutableList(),
                    tasks = tasks.toMutableList(),
                    answersList = answersList.toMutableList(),
                    keysList = keysList.toMutableList()
                )
                println("Тестик" + test.toString())
                callback(test)
            } else {
                Log.d(TAG, "No such document")
                callback(null)
            }
        }
        .addOnFailureListener { exception ->
            Log.d(TAG, "get failed with ", exception)
            callback(null)
        }
}
class FinishTestDialogFragment(val userID: String, val testID: String, val enteredKeys: MutableList<MutableList<Boolean>>, val keysList: MutableList<MutableList<Boolean>>) : DialogFragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.finish_test_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.go_button).setOnClickListener {
            lateinit var mistakesPositions : MutableList<Int>
            lateinit var wrongAnswers : MutableList<MutableList<Int>>
            checkTest(enteredKeys, keysList, callback = { a, b -> mistakesPositions = a; wrongAnswers = b})
            println(mistakesPositions)
            println(wrongAnswers)
            uploadTest(userID, mistakesPositions, testID, wrongAnswers)
            val intent = Intent(activity, ViewingMistakesActivity::class.java).apply {
                putExtra("ID", testID)
            }
            startActivity(intent)
            requireActivity().finish()
            dismiss()
        }

        view.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            dismiss()
        }
    }

}


