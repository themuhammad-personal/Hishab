@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hishab.app.ui.sheets

import com.hishab.app.ai.AssistantDraft
import com.hishab.app.domain.EntryKind

sealed interface Sheet {
    data class Entry(val txId: String? = null, val kind: EntryKind = EntryKind.EXPENSE, val todoId: String? = null, val draft: AssistantDraft.AddTransaction? = null) : Sheet
    data class Payment(val txId: String) : Sheet
    data class Wallet(val id: String? = null) : Sheet
    data class Category(val id: String? = null) : Sheet
    data class Todo(val id: String? = null) : Sheet
    data object Transfer : Sheet
    data class Budget(val categoryId: String? = null) : Sheet
}
