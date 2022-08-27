package com.sebix.cleanarchitecture.framework.datasource.cache.util

import com.sebix.cleanarchitecture.business.domain.model.Note
import com.sebix.cleanarchitecture.business.domain.util.EntityMapper
import com.sebix.cleanarchitecture.framework.datasource.cache.model.NoteCacheEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheMapper
@Inject
constructor() : EntityMapper<NoteCacheEntity, Note> {

    fun entityListToNoteList(entities: List<NoteCacheEntity>): List<Note> {
        val list = arrayListOf<Note>()
        entities.forEach {
            list.add(mapFromEntity(it))
        }
        return list
    }

    fun noteListToEntityList(notes: List<Note>): List<NoteCacheEntity> {
        val list = arrayListOf<NoteCacheEntity>()
        notes.forEach {
            list.add(mapToEntity(it))
        }
        return list
    }

    override fun mapFromEntity(entity: NoteCacheEntity): Note {
        return Note(
            id = entity.id,
            title = entity.title,
            body = entity.body,
            created_at = entity.created_at,
            updated_at = entity.updated_at
        )
    }

    override fun mapToEntity(domainModel: Note): NoteCacheEntity {
        return NoteCacheEntity(
            id = domainModel.id,
            title = domainModel.title,
            body = domainModel.body,
            created_at = domainModel.created_at,
            updated_at = domainModel.updated_at
        )
    }
}