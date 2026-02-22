package dk.itu.moapd.x9.elie

import android.app.DatePickerDialog
import android.content.Context
import android.widget.ArrayAdapter
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar
import java.util.Locale

object UiPickers {

    fun attachDatePicker(
        context: Context,
        input: TextInputEditText,
        format: (year: Int, month: Int, day: Int) -> String = { y, m, d ->
            val mm = String.format(Locale.US, "%02d", m + 1)
            val dd = String.format(Locale.US, "%02d", d)
            "$y-$mm-$dd"
        }
    ) {
        // Make it behave like a picker field, not a typing field
        input.isFocusable = false
        input.isClickable = true
        input.inputType = 0

        input.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                context,
                { _, y, m, d ->
                    input.setText(format(y, m, d))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    fun attachDropdown(
        context: Context,
        input: MaterialAutoCompleteTextView,
        items: List<String>
    ) {
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, items)
        input.setAdapter(adapter)
        input.setOnClickListener { input.showDropDown() }
        input.inputType = 0
    }
}