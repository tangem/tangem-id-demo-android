package com.tangem.id.features.issuecredentials.ui.textwatchers

import android.text.Editable
import android.text.TextWatcher

class SsnTextWatcher : TextWatcher {
    private var backspaced = false

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        backspaced = count == 0
    }

    override fun afterTextChanged(s: Editable?) {
        if (s?.length ?: 0 > 10 ) s?.delete(10, s.length)

        s?.apply {

            if (!isEmpty()) {
                if (get(lastIndex) != '-' && (length > 1 && get(lastIndex - 1) != '-') && !backspaced)
                    count { x -> x.isDigit() }
                        .let {
                            mapOf(3 to 3, 4 to 3, 5 to 6, 6 to 6)[it]
                                ?.let { index -> insert(index, "-") }
                        }
                else if ((get(lastIndex) == '-' && backspaced)
                    || (lastIndexOf('-') in listOf(0, 1, 2, 4, 5, 7, 8, 9, 10))
                )
                    delete(length - 1, length)
            }
        }
    }

    override fun beforeTextChanged(
        s: CharSequence?,
        start: Int,
        count: Int,
        after: Int
    ) {
    }
}