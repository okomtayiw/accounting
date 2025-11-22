package com.babsnet.accounting.utils

import androidx.lifecycle.*

fun <T> LiveData<T>.observeOnce(
    lifecycleOwner: LifecycleOwner,
    observer: (T) -> Unit
) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(value: T) {
            observer(value)
            removeObserver(this)
        }
    })
}
