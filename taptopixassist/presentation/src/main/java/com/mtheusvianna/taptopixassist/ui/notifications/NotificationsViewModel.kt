package com.mtheusvianna.taptopixassist.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtheusvianna.taptopixassist.presentation.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply { value = "" }
    val text: LiveData<String> = _text

    private val _iconColor = MutableLiveData<Int>().apply { value = R.color.white }
    val iconColor: LiveData<Int> = _iconColor

    private val _iconDrawable = MutableLiveData<Int>().apply { value = R.drawable.contactless_off }
    val iconDrawable: LiveData<Int> = _iconDrawable

    private var resetTextJob: Job? = null

    fun updateTextWith(newText: String) {
        resetTextJob?.cancel()
        _text.value = newText
        _iconColor.value = android.R.color.holo_green_dark
        resetTextJob = viewModelScope.launch {
            delay(5000)
            _text.value = ""
            _iconColor.value = R.color.white
        }
    }

    fun updateNfcStatusWith(isAvailable: Boolean) {
        _iconDrawable.value = if (isAvailable) R.drawable.contactless_on else R.drawable.contactless_off
    }
}