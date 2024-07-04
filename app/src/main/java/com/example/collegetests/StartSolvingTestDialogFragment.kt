package com.example.collegetests

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment

class StartSolvingTestDialogFragment(private val userId: String) : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.start_giving_test_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val go_button = view.findViewById<Button>(R.id.go_button)
        go_button.setOnClickListener {
            val subjectField: EditText = view.findViewById(R.id.enter_subject)
            val topicField: EditText = view.findViewById(R.id.enter_topic)
            val groupsField: EditText = view.findViewById(R.id.enter_groups)
            if (!(subjectField.text.isEmpty() || topicField.text.isEmpty() || groupsField.text.isEmpty()|| subjectField.text.toString() == "" || topicField.text.toString() == "" || groupsField.text.toString() == "")) {
                val intent = Intent(activity, CreatingTestActivity::class.java).apply {
                    putExtra("userId", userId)
                    putExtra("SUBJECT", subjectField.text.toString())
                    putExtra("TOPIC", topicField.text.toString())
                    putExtra("GROUPS", groupsField.text.toString())
                }
                startActivity(intent)
                dismiss()
            } else { Toast.makeText(requireActivity(),"Неправильный ввод данных", Toast.LENGTH_SHORT).show();}
        }

        view.findViewById<Button>(R.id.cancel_button).setOnClickListener {
            dismiss()
        }
    }
}