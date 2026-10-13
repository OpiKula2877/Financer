package cz.financer.ui.projects

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.financer.FinancerApplication
import cz.financer.data.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProjectsViewModel(private val app: FinancerApplication) : ViewModel() {

    val projects: StateFlow<List<Project>> = app.repository.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Vytvoří projekt. Logo se uloží do úložiště aplikace, pokud bylo vybráno. */
    fun createProject(
        name: String,
        ownerFirstName: String,
        ownerLastName: String,
        logo: Uri?,
        onCreated: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val logoPath = logo?.let { withContext(Dispatchers.IO) { app.images.saveSquare(it) } }
            val id = app.repository.createProject(name, ownerFirstName, ownerLastName, logoPath)
            onCreated(id)
        }
    }
}
