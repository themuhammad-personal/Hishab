package com.hishab.app.data.local

import androidx.room.TypeConverter
import com.hishab.app.domain.DueDirection
import com.hishab.app.domain.EntryKind
import com.hishab.app.domain.TodoPriority

class Converters {
    @TypeConverter fun entryKind(value: String): EntryKind = EntryKind.valueOf(value)
    @TypeConverter fun entryKind(value: EntryKind): String = value.name
    @TypeConverter fun dueDirection(value: String?): DueDirection? = value?.let { DueDirection.valueOf(it) }
    @TypeConverter fun dueDirection(value: DueDirection?): String? = value?.name
    @TypeConverter fun todoPriority(value: String): TodoPriority = TodoPriority.valueOf(value)
    @TypeConverter fun todoPriority(value: TodoPriority): String = value.name
}
