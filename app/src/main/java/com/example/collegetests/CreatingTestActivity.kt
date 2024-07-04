package com.example.collegetests

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone


class CreatingTestActivity : AppCompatActivity(), NumberAdapter.NumberClickListener, NumberAdapter.SetColor/*, NewItemAdapter.updateItemsRecyclerViewImpl*/ {



    private lateinit var tasksListToShow: MutableList<String>
    //private lateinit var creatingTestViewModel: CreatingTestViewModel

    private lateinit var answers: MutableList<MutableList<String>>
    private lateinit var checkboxesStates: MutableList<MutableList<Boolean>>

    private lateinit var numberAdapter: NumberAdapter
    private val numbers = mutableListOf<Int>(1)
    private lateinit var itemsAdapter: NewItemAdapter
    private var itemsList: MutableList<String> = mutableListOf("")
    private var checkboxesList: MutableList<Boolean> = mutableListOf(false)

    var currentNumber: Int = 1


    override fun onCreate(savedInstanceState: Bundle?) { try {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creating_test)

        val userID = intent.getStringExtra("userId")
        val subject = intent.getStringExtra("SUBJECT")
        val topic = intent.getStringExtra("TOPIC")
        val groups = intent.getStringExtra("GROUPS")


        //creatingTestViewModel = ViewModelProvider(this).get(CreatingTestViewModel::class.java)

        tasksListToShow = mutableListOf("")
        answers = mutableListOf(mutableListOf(""))
        checkboxesStates = mutableListOf(mutableListOf(false))

        val enter_question_field: TextView = findViewById(R.id.enter_question_field)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerview)
        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        numberAdapter = NumberAdapter(
            enter_question_field,
            numbers,
            this,
            textProvider = { enter_question_field.text.toString() },
            showToast = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() },
            checkIfCorrectlyEntered = { itemsAdapter.checkIfCorrectlyEntered() })
        recyclerView.adapter = numberAdapter

        val itemsRecyclerView: RecyclerView = findViewById(R.id.items_recycler_view)
        itemsRecyclerView.layoutManager = LinearLayoutManager(this)

        itemsAdapter = NewItemAdapter(itemsRecyclerView, itemsList, checkboxesList)
        itemsRecyclerView.adapter = itemsAdapter

        val addButton: Button = findViewById(R.id.add_button)
        val removeButton: Button = findViewById(R.id.remove_button)
        val publishTestButton: Button = findViewById(R.id.publish_test_button)
        removeButton.isEnabled = false
        removeButton.setBackgroundTintList(
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    this,
                    R.color.light_grey
                )
            )
        )
        removeButton.setTextColor(
            (ColorStateList.valueOf(
                ContextCompat.getColor(
                    this,
                    R.color.grey
                )
            ))
        )
        addButton.setOnLongClickListener {

            true
        }
        addButton.setOnClickListener {
            try {
                if (itemsAdapter.checkIfCorrectlyEntered() && !(enter_question_field.text.toString() == "" || enter_question_field.text.toString().contains("|"))
                ) {
                    tasksListToShow.add("")
                    tasksListToShow[currentNumber - 1] = enter_question_field.text.toString()
                    answers[currentNumber - 1] = itemsAdapter.getItems()
                    answers.add(mutableListOf(""))
                    checkboxesStates[currentNumber - 1] = itemsAdapter.getCheckBoxes()
                    checkboxesStates.add(mutableListOf(false))
                    val newItem = (numbers.maxOrNull() ?: 0) + 1
                    numberAdapter.addItem(newItem)
                    currentNumber++
                    recyclerView.smoothScrollToPosition(numberAdapter.itemCount - 1)
                    removeButton.isEnabled = true
                    removeButton.setBackgroundTintList(
                        ColorStateList.valueOf(
                            ContextCompat.getColor(
                                this,
                                R.color.purple_500
                            )
                        )
                    )
                    removeButton.setTextColor(
                        (ColorStateList.valueOf(
                            ContextCompat.getColor(
                                this,
                                R.color.white
                            )
                        ))
                    )
                    itemsAdapter.updateData(answers[currentNumber - 1], checkboxesStates[currentNumber - 1])
                    enter_question_field.setText(tasksListToShow[currentNumber - 1])
                } else {
                    Toast.makeText(this, "Неправильный ввод", Toast.LENGTH_SHORT).show()
                }
            } catch (illegalStateException: IllegalStateException) {
                println("Exception has been caught!")
                Toast.makeText(this, "Illigal State Exception", Toast.LENGTH_SHORT).show()
            }
        }

        removeButton.setOnClickListener {
            if (numbers.isNotEmpty() && currentNumber != 1) {
                if (numbers.size > 1) {
                    tasksListToShow.removeAt(currentNumber - 1)
                    numberAdapter.removeItem(currentNumber - 1)
                    enter_question_field.setText(tasksListToShow[currentNumber - 2])
                    currentNumber--
                }

            }
            if (numbers.size == 1) {
                removeButton.setBackgroundTintList(
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            this,
                            R.color.light_grey
                        )
                    )
                )
                removeButton.setTextColor(
                    (ColorStateList.valueOf(
                        ContextCompat.getColor(
                            this,
                            R.color.grey
                        )
                    ))
                )
                removeButton.isEnabled = false
            }
        }
        publishTestButton.setOnClickListener {

            val dialogView = LayoutInflater.from(this).inflate(R.layout.publish_test_dialog, null)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            dialogView.findViewById<Button>(R.id.go_button).setOnClickListener {
                val id = getCurrentDateTime()
                val subject = intent.getStringExtra("SUBJECT")
                val topic = intent.getStringExtra("TOPIC")
                val groups = intent.getStringExtra("GROUPS")
                val tasks = tasksListToShow
                val answersList = answers
                val keysList = checkboxesStates
                val teacherId = intent.getStringExtra("userId")

                uploadTestTeacher(id, subject!!, topic!!, groups!!, tasks, answersList, keysList, teacherId!!) { success ->
                    if (success) {
                        Toast.makeText(this, "Test published successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error publishing test", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
                finish()
            }

            dialogView.findViewById<Button>(R.id.cancel_button).setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

    } catch (ilstex : IllegalStateException) {Toast.makeText(this, "ISE", Toast.LENGTH_SHORT).show()}
    }


    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (currentFocus != null) {
            val inputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
            currentFocus!!.clearFocus()
        }
        return super.dispatchTouchEvent(event)
    }
    override fun onNumberClicked(position: Int, previousPosition: Int, enter_question_field: TextView) {
        if (previousPosition >= 0 && previousPosition < tasksListToShow.size) {
            tasksListToShow[previousPosition] = enter_question_field.text.toString()
            answers[previousPosition] = itemsAdapter.getItems()
            checkboxesStates[previousPosition] = itemsAdapter.getCheckBoxes()
        }

        currentNumber = position + 1
        enter_question_field.setText(tasksListToShow[currentNumber - 1])

        itemsAdapter.updateData(answers[currentNumber - 1], checkboxesStates[currentNumber - 1])
    }


    override fun setColor(holder: NumberAdapter.NumberViewHolder, position: Int, colorCode: String) {
        val newColor = Color.parseColor(colorCode)
        holder.textView.setBackgroundColor(newColor)
    }
}

class NumberAdapter(
    private val enter_question_field: TextView,
    private val numbers: MutableList<Int>,
    private val clickListener: NumberClickListener,
    private val textProvider: () -> String,
    private val showToast: (String) -> Unit,
    private val checkIfCorrectlyEntered: () -> Boolean
) :
    RecyclerView.Adapter<NumberAdapter.NumberViewHolder>() {

    private var selectedIndex = 0

    class NumberViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
    fun updateNumberText(position: Int) {
        if (position != numbers.size) {
            for (i in position..numbers.size - 1) {
                numbers[i - 1] = numbers[i - 1] - 1
                notifyItemChanged(i - 1)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumberViewHolder {
        val textView = LayoutInflater.from(parent.context)
            .inflate(R.layout.horizontal_list_item, parent, false) as TextView
        return NumberViewHolder(textView)
    }

    override fun onBindViewHolder(holder: NumberViewHolder, position: Int) {
        holder.textView.text = numbers[position].toString()
        holder.textView.setBackgroundColor(
            if (selectedIndex == position) Color.parseColor("#FF03DAC5")
            else Color.parseColor("#6200EE")
        )
        holder.textView.setOnClickListener {
            if (checkIfCorrectlyEntered()) {
                val adapterPosition = holder.bindingAdapterPosition
                if (adapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                val previousIndex = selectedIndex
                selectedIndex = adapterPosition
                notifyItemChanged(previousIndex)
                notifyItemChanged(adapterPosition)

                clickListener.onNumberClicked(adapterPosition, previousIndex, enter_question_field)
            } else {
                showToast("Неправильный ввод")
            }
        }
    }

    override fun getItemCount() = numbers.size

    fun addItem(item: Int) {
        numbers.add(item)
        if (selectedIndex != -1) {
            notifyItemChanged(selectedIndex)
        }
        selectedIndex = numbers.size - 1
        notifyItemInserted(selectedIndex)
        notifyItemChanged(selectedIndex)
    }

    fun removeItem(position: Int) {
        if (numbers.isNotEmpty() && position < numbers.size) {
            val wasSelected = selectedIndex == position
            numbers.removeAt(selectedIndex)
            notifyItemRemoved(selectedIndex)
            selectedIndex--
            notifyItemChanged(selectedIndex)
        } else {
            Log.e("NumberAdapter", "Index out of bounds: $position")
        }
    }

    sealed interface NumberClickListener {
        fun onNumberClicked(position: Int, previousPosition: Int, enter_question_field: TextView)
    }

    sealed interface SetColor {
        fun setColor(holder: NumberAdapter.NumberViewHolder, position: Int, colorCode: String)
    }
}



class NewItemAdapter(
    private val itemsRecyclerView: RecyclerView,
    private var items: MutableList<String>,
    private var checkBoxes: MutableList<Boolean>
) : RecyclerView.Adapter<NewItemAdapter.ItemViewHolder>() {

    fun getItems() : MutableList<String>{ return items }
    fun getCheckBoxes() : MutableList<Boolean>{ return checkBoxes}

    private val selectedItems = HashSet<Int>()

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.check_box_item)
        val editText: EditText = view.findViewById(R.id.edit_text_item)
        val addAnwserButton: Button = view.findViewById(R.id.addAnswerButton)
        val removeAnwserButton: Button = view.findViewById(R.id.removeAnswerButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.creating_answer_item, parent, false)
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

        holder.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                items[holder.bindingAdapterPosition] = holder.editText.text.toString()
            }
        })

        holder.editText.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                items[holder.bindingAdapterPosition] = holder.editText.text.toString()
            }
        }

        holder.addAnwserButton.setOnClickListener {
            addItem("", false)
        }

        holder.removeAnwserButton.setOnClickListener {
            removeItem(holder.bindingAdapterPosition)
        }
    } catch (ise : IllegalStateException){Toast.makeText(holder.itemView.context,"Иллигал", Toast.LENGTH_SHORT).show()}}

    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newItems: MutableList<String>, newCheckBoxes: MutableList<Boolean>) {
        // Обновите источник данных адаптера
        this.items = mutableListOf("")
        this.items = newItems
        this.checkBoxes = mutableListOf(false)
        this.checkBoxes = newCheckBoxes
        // Уведомите адаптер о том, что данные изменились
        this.notifyDataSetChanged()
    }

    fun checkIfCorrectlyEntered(): Boolean {
        for (i in 0..items.size - 1) {
            if (items[i] == "") {
                return false
            }
        }
        for (i in 0..checkBoxes.size - 1) {
            if (checkBoxes[i] == true) return true
        }
        return false
    }

    fun addItem(text: String, ifChecked: Boolean) {
        items.add(text)
        checkBoxes.add(ifChecked)
        notifyItemInserted(items.size - 1)
    }

    fun removeItem(position: Int) {
        if (items.size > 1 && position >= 0 && position < items.size) {
            items.removeAt(position)
            checkBoxes.removeAt(position)
            selectedItems.remove(position)
            notifyItemRemoved(position)
        } else {
            Log.e("NewItemAdapter", "Index out of bounds: $position")
        }
    }
    interface updateItemsRecyclerViewImpl{
        fun updateItemsRecyclerView(holder: ItemViewHolder, items: MutableList<String>, checkBoxes: MutableList<Boolean>)
    }

}

fun getCurrentDateTime(): String {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
    val dateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH) // Явно указан язык
    return dateFormat.format(calendar.time)
}
fun uploadTestTeacher(
    id: String,
    subject: String,
    topic: String,
    groups: String,
    tasks: MutableList<String>,
    answersList: MutableList<MutableList<String>>,
    keysList: MutableList<MutableList<Boolean>>,
    teacherId: String,
    callback: (Boolean) -> Unit
) {
    println(answersList)
    println(keysList)
    val answers: MutableList<String> = MutableList(answersList.size) { "" }
    val keys: MutableList<String> = MutableList(keysList.size) { "" }

    for (i in answersList.indices) {
        answers[i] = answersList[i].joinToString("|")
        keys[i] = keysList[i].joinToString("|")
    }

    val groupsList = groups.split(" ")
    val newTest = hashMapOf(
        "id" to id,
        "subject" to subject,
        "topic" to topic,
        "groups" to groupsList,
        "tasks" to tasks,
        "answers" to answers,
        "keys" to keys,
        "teacher_id" to teacherId
    )

    val db = FirebaseFirestore.getInstance()
    db.collection("given_tests").document(id).set(newTest)
        .addOnSuccessListener {
            // Add test ID to unsolved_tests field of all students in the groups
            groupsList.forEach { groupId ->
                db.collection("groups").document(groupId).get()
                    .addOnSuccessListener { groupDocument ->
                        val studentIds = groupDocument["student_ids"] as? MutableList<String> ?: mutableListOf()
                        studentIds.forEach { studentId ->
                            db.collection("students").document(studentId).update("unsolved_tests", FieldValue.arrayUnion(id))
                                .addOnSuccessListener {
                                    Log.d("Firestore", "unsolved_tests updated with new id.")
                                }
                                .addOnFailureListener { e ->
                                    Log.w("Firestore", "Error updating unsolved_tests", e)
                                }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.w("Firestore", "Error getting group document", exception)
                    }
            }

            // Add test ID to given_tests field of the teacher
            db.collection("teachers").document(teacherId).update("given_tests", FieldValue.arrayUnion(id))
                .addOnSuccessListener {
                    Log.d("Firestore", "given_tests updated with new id.")
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error updating given_tests", e)
                    println(teacherId + " teacher")
                }

            callback(true)
        }
        .addOnFailureListener { error ->
            Log.w(TAG, "Error adding document", error)
            println(error)
            callback(false)
        }
}