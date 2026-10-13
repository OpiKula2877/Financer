package cz.financer.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import cz.financer.FinancerApplication

/** Vytvoří ViewModel s vlastním konstruktorem bez Hilt. */
fun <VM : ViewModel> viewModelFactory(initializer: () -> VM): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            initializer() as T
    }

fun Context.financer(): FinancerApplication = applicationContext as FinancerApplication
