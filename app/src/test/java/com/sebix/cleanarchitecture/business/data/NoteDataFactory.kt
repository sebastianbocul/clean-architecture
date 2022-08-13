package com.sebix.cleanarchitecture.business.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sebix.cleanarchitecture.business.domain.model.Note

class NoteDataFactory(
    private val testClassLoader: ClassLoader
) {

    fun produceListOfNotes(): List<Note> {
        return Gson().fromJson(
            getNotesFromFile("note_list.json"),
            object : TypeToken<List<Note>>() {}.type
        )
    }

    fun produceHashMapOfNotes(noteList: List<Note>): HashMap<String, Note> {
        val map = hashMapOf<String, Note>()
        noteList.iterator().forEach {
            map[it.id] = it
        }
        return map
    }

    fun produceEmptyListOfNotes(): List<Note> {
        return listOf()
    }

    fun getNotesFromFile(fileName: String): String {
        return testClassLoader.getResource(fileName).readText()
    }
}