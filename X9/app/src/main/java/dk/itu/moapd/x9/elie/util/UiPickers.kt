package dk.itu.moapd.x9.elie.util

import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.view.MotionEvent
import android.widget.ArrayAdapter
import com.google.android.material.color.MaterialColors
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
        fun showPicker() {
            val cal = Calendar.getInstance()
            val picker = DatePickerDialog(
                context,
                { _, y, m, d ->
                    input.setText(format(y, m, d))
                    input.error = null
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            picker.show()

            val actionColor = MaterialColors.getColor(
                input,
                androidx.appcompat.R.attr.colorPrimary
            )
            picker.getButton(DialogInterface.BUTTON_POSITIVE)?.setTextColor(actionColor)
            picker.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(actionColor)
        }

        input.isFocusable = false
        input.isFocusableInTouchMode = false
        input.isClickable = true
        input.isLongClickable = false
        input.isCursorVisible = false
        input.inputType = 0

        input.setOnClickListener { showPicker() }
        input.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showPicker()
        }
        input.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                showPicker()
                return@setOnTouchListener true
            }
            false
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
