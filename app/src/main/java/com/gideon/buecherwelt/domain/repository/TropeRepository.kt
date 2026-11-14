package com.gideon.buecherwelt.domain.repository

import com.gideon.buecherwelt.data.local.TropeDao
import com.gideon.buecherwelt.data.local.TropeEntity
import com.gideon.buecherwelt.data.local.TropesCatalog
import com.gideon.buecherwelt.domain.model.Trope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TropeRepository(private val tropeDao: TropeDao) {

    fun observeAll(): Flow<List<Trope>> =
        tropeDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAll(): List<Trope> = tropeDao.getAll().map { it.toDomain() }

    suspend fun search(query: String): List<Trope> =
        tropeDao.search(query).map { it.toDomain() }

    suspend fun ensureSeeded() {
        if (tropeDao.getAll().isEmpty()) {
            TropesCatalog.entries.forEach { (name, category) ->
                tropeDao.insert(TropeEntity(name = name, category = category, isCustom = false))
            }
        }
    }

    suspend fun getOrCreateCustom(name: String, category: String = "Eigen"): Trope {
        val trimmed = name.trim()
        tropeDao.findByName(trimmed)?.let { return it.toDomain() }
        val id = tropeDao.insert(TropeEntity(name = trimmed, category = category, isCustom = true))
        return Trope(id = id, name = trimmed, category = category, isCustom = true)
    }

    suspend fun delete(trope: Trope) {
        tropeDao.delete(TropeEntity(trope.id, trope.name, trope.category, trope.isCustom))
    }

    suspend fun resolveIds(names: Collection<String>): Set<Long> =
        names.mapNotNull { name ->
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return@mapNotNull null
            (tropeDao.findByName(trimmed) ?: tropeDao.search(trimmed).firstOrNull())?.id
                ?: getOrCreateCustom(trimmed).id
        }.toSet()

    private fun TropeEntity.toDomain(): Trope =
        Trope(id = id, name = name, category = category, isCustom = isCustom)
}
