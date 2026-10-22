package com.company.ithelpdesk.data

import com.company.ithelpdesk.domain.model.Category
import com.company.ithelpdesk.domain.model.Department
import com.company.ithelpdesk.domain.model.SlaSettings
import com.company.ithelpdesk.domain.repository.SettingsRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
) : SettingsRepository {

    private val slaDoc get() = db.collection("settings").document("sla")
    private val categories get() = db.collection("categories")
    private val departments get() = db.collection("departments")

    override fun observeSla(): Flow<SlaSettings> = slaDoc.asFlow().map { it.toSlaSettings() }

    override suspend fun saveSla(settings: SlaSettings) {
        slaDoc.set(settings.toMap()).await()
    }

    override fun observeCategories(): Flow<List<Category>> =
        categories.asFlow().map { s -> s.documents.map { it.toCategory() }.sortedBy { it.name.lowercase() } }

    override suspend fun saveCategory(category: Category) {
        val ref = if (category.id.isBlank()) categories.document() else categories.document(category.id)
        ref.set(category.toMap()).await()
    }

    override suspend fun deleteCategory(id: String) {
        categories.document(id).delete().await()
    }

    override suspend fun addDefaultCategories() {
        val batch = db.batch()
        Category.DEFAULTS.forEach { batch.set(categories.document(it.id), it.toMap()) }
        batch.commit().await()
    }

    override fun observeDepartments(): Flow<List<Department>> =
        departments.asFlow().map { s -> s.documents.map { it.toDepartment() }.sortedBy { it.name.lowercase() } }

    override suspend fun saveDepartment(name: String) {
        departments.document().set(mapOf("name" to name.trim())).await()
    }

    override suspend fun deleteDepartment(id: String) {
        departments.document(id).delete().await()
    }
}
