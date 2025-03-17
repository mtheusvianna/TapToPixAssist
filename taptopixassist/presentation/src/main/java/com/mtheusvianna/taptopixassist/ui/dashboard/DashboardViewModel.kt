package com.mtheusvianna.taptopixassist.ui.dashboard

import android.nfc.Tag
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = ""
    }
    val text: LiveData<String> = _text


    private val _tagDiscoveredEvent = MutableSharedFlow<Tag>()
    val tagDiscoveredEvent: SharedFlow<Tag> = _tagDiscoveredEvent

    fun emitDiscovered(tag: Tag) {
        viewModelScope.launch {
            _tagDiscoveredEvent.emit(tag)
        }
    }

    fun updateTextWith(newText: String) {
        _text.value = newText
    }
}