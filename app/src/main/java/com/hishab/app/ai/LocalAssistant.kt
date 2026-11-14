package com.hishab.app.ai

import com.hishab.app.core.Dates
import com.hishab.app.core.Money
import com.hishab.app.data.local.CategoryEntity
import com.hishab.app.data.local.TransactionEntity
import com.hishab.app.data.local.WalletEntity
import com.hishab.app.domain.EntryKind
import java.util.UUID

sealed interface AssistantDraft {
    val id: String
    val summary: String
    val lines: List<Pair<String, String>>
    data class AddTransaction(
        override val id: String,
        val kind: EntryKind,
        val title: String,
        val amountPoisha: Long,
        val categoryId: String,
        val walletId: String?,
        val date: String,
        val paid: Boolean,
        override val summary: String,
        override val lines: List<Pair<String, String>>
    ) : AssistantDraft
    data class PayDue(
        override val id: String,
        val transactionId: String,
        val walletId: String,
        val amountPoisha: Long,
        val date: String,
        override val summary: String,
        override val lines: List<Pair<String, String>>
    ) : AssistantDraft
    data class DeleteTransaction(
        override val id: String,
        val transactionId: String,
        override val summary: String,
        override val lines: List<Pair<String, String>>
    ) : AssistantDraft
}

data class AssistantReply(val text: String, val draft: AssistantDraft? = null)

/** Conservative offline parser. It never writes; the UI must confirm a draft. */
class LocalAssistant {
    fun parse(input: String, categories: List<CategoryEntity>, wallets: List<WalletEntity>, transactions: List<TransactionEntity>, today: String = Dates.today().toString()): AssistantReply {
        val text = Money.normalizeDigits(input).trim()
        if (text.isBlank()) return AssistantReply("কী হিসাব করতে চান লিখুন। যেমন: আজ বাজারে ৫০০ টাকা খরচ।")
        val amount = extractAmount(text)
        val date = when {
            text.contains("গতকাল") || text.contains("yesterday", true) -> Dates.today().minusDays(1).toString()
            text.contains("আগামীকাল") || text.contains("tomorrow", true) -> Dates.today().plusDays(1).toString()
            else -> today
        }
        val wallet = wallets.firstOrNull { !it.archived && (text.contains(it.name, true) || matchesWalletAlias(text, it.type)) }
            ?: wallets.firstOrNull { !it.archived }
        val lower = text.lowercase()
        val isPay = listOf("পরিশোধ", "পেমেন্ট", "paid", "pay", "শোধ").any { lower.contains(it) }
        val isDelete = listOf("মুছে", "মুছুন", "delete", "remove", "বাদ").any { lower.contains(it) }
        if (isDelete) {
            val candidates = transactions.filter { it.deletedAt == null && (text.contains(it.title, true) || (it.shopOrPerson?.let { s -> text.contains(s, true) } == true)) }
            if (candidates.size != 1) return AssistantReply("কোন লেনদেন মুছব নিশ্চিতভাবে চিহ্নিত করতে পারিনি। লেনদেন তালিকা থেকে নির্বাচন করুন।")
            val tx = candidates.single()
            return AssistantReply("মুছে ফেলার খসড়া তৈরি করেছি। নিশ্চিত না করলে কিছু মুছবে না।", AssistantDraft.DeleteTransaction(UUID.randomUUID().toString(), tx.id, "লেনদেন মুছে ফেলা", listOf("শিরোনাম" to tx.title, "পরিমাণ" to Money.formatPoisha(tx.amountPoisha), "তারিখ" to Dates.formatDate(tx.date))))
        }
        if (isPay) {
            val candidates = transactions.filter { it.deletedAt == null && it.dueDirection != null && (it.amountPoisha - it.paidPoisha) > 0 && (text.contains(it.title, true) || (it.shopOrPerson?.let { s -> text.contains(s, true) } == true)) }
            if (candidates.size != 1) return AssistantReply("কোন বাকি পরিশোধ করব নিশ্চিতভাবে চিহ্নিত করতে পারিনি। দোকান/ব্যক্তির নাম ও পরিমাণ লিখুন অথবা বাকির খাতা থেকে নির্বাচন করুন।")
            val tx = candidates.single()
            val remaining = tx.amountPoisha - tx.paidPoisha
            val payAmount = amount ?: remaining
            if (payAmount <= 0 || payAmount > remaining) return AssistantReply("পরিশোধের পরিমাণ বাকি টাকার মধ্যে হতে হবে। বাকি: ${Money.formatPoisha(remaining)}")
            if (wallet == null) return AssistantReply("কোন ওয়ালেট থেকে পরিশোধ করব? একটি সক্রিয় ওয়ালেট তৈরি করুন।")
            return AssistantReply("পরিশোধের খসড়া তৈরি করেছি। নিশ্চিত করার আগে মিলিয়ে নিন।", AssistantDraft.PayDue(UUID.randomUUID().toString(), tx.id, wallet.id, payAmount, date, "বাকি পরিশোধ", listOf("দোকান/ব্যক্তি" to (tx.shopOrPerson ?: tx.title), "পরিমাণ" to Money.formatPoisha(payAmount), "ওয়ালেট" to wallet.name, "বাকি থাকবে" to Money.formatPoisha(remaining - payAmount))))
        }
        if (amount == null || amount <= 0) return AssistantReply("টাকার পরিমাণ বুঝতে পারিনি। যেমন: আজ বাজারে ৫০০ টাকা খরচ।")
        val kind = if (listOf("আয়", "আয়ের", "income", "বেতন", "salary", "পেলাম", "received").any { lower.contains(it) }) EntryKind.INCOME else EntryKind.EXPENSE
        val category = categories.firstOrNull { !it.archived && it.kind == kind && text.contains(it.name, true) }
            ?: categories.firstOrNull { !it.archived && it.kind == kind && matchesCategoryAlias(lower, it.name) }
            ?: categories.firstOrNull { !it.archived && it.kind == kind }
            ?: return AssistantReply("এই ধরনের একটি সক্রিয় ক্যাটাগরি আগে তৈরি করুন।")
        val paid = !listOf("বাকি", "বাকিতে", "due", "unpaid", "পাব", "দেব").any { lower.contains(it) }
        if (paid && wallet == null) return AssistantReply("পরিশোধিত হিসাবের জন্য একটি সক্রিয় ওয়ালেট দরকার।")
        val title = buildTitle(text, category.name)
        val lines = buildList {
            add("ধরন" to if (kind == EntryKind.EXPENSE) "খরচ" else "আয়")
            add("শিরোনাম" to title); add("পরিমাণ" to Money.formatPoisha(amount)); add("ক্যাটাগরি" to category.name)
            add("ওয়ালেট" to (wallet?.name ?: "নির্বাচন করা হয়নি")); add("তারিখ" to Dates.formatDate(date)); add("অবস্থা" to if (paid) "পরিশোধিত" else "বাকি")
        }
        return AssistantReply("হিসাবের খসড়া তৈরি করেছি। নিশ্চিত না করলে খাতা বদলাবে না।", AssistantDraft.AddTransaction(UUID.randomUUID().toString(), kind, title, amount, category.id, wallet?.id, date, paid, if (kind == EntryKind.EXPENSE) "নতুন খরচ" else "নতুন আয়", lines))
    }

    private fun extractAmount(text: String): Long? {
        val patterns = listOf(Regex("(\\d+(?:\\.\\d{1,2})?)\\s*(?:টাকা|৳|taka|bdt)", RegexOption.IGNORE_CASE), Regex("(?:৳|\\$)\\s*(\\d+(?:\\.\\d{1,2})?)"), Regex("(?:খরচ|আয়|পরিশোধ|পরিমাণ)\\s*(\\d+(?:\\.\\d{1,2})?)"))
        for (p in patterns) p.find(text)?.groupValues?.get(1)?.let { Money.parsePoisha(it)?.let { v -> return v } }
        val nums = Regex("\\d+(?:\\.\\d{1,2})?").findAll(text).map { it.value }.toList()
        return if (nums.size == 1) Money.parsePoisha(nums.single()) else null
    }
    private fun matchesWalletAlias(text: String, type: String): Boolean = when (type) {
        "cash" -> text.contains("নগদ") || text.contains("cash", true) || text.contains("ক্যাশ", true)
        "bkash" -> text.contains("বিকাশ", true) || text.contains("bkash", true)
        "nagad" -> text.contains("নগদ", true) || text.contains("nagad", true)
        "bank" -> text.contains("ব্যাংক", true) || text.contains("bank", true)
        else -> false
    }
    private fun matchesCategoryAlias(lower: String, name: String): Boolean = when {
        name.contains("বাজার") -> lower.contains("বাজার") || lower.contains("grocery") || lower.contains("সবজি")
        name.contains("যাতায়াত") -> lower.contains("যাতায়াত") || lower.contains("ভাড়া") || lower.contains("transport") || lower.contains("রিকশা")
        name.contains("বিল") -> lower.contains("বিল") || lower.contains("bill") || lower.contains("বিদ্যুৎ")
        name.contains("মেডিকেল") -> lower.contains("ওষুধ") || lower.contains("ডাক্তার") || lower.contains("medical")
        name.contains("শিক্ষা") -> lower.contains("স্কুল") || lower.contains("মাদ্রাসা") || lower.contains("শিক্ষা")
        name.contains("মাছ") -> lower.contains("মাছ") || lower.contains("মাংস") || lower.contains("fish") || lower.contains("meat")
        else -> false
    }
    private fun buildTitle(text: String, categoryName: String): String {
        val cleaned = text.replace(Regex("(আজ|গতকাল|আগামীকাল|টাকা|৳|খরচ|আয়|পরিশোধ|করলাম|করেছি|দিলাম|পেলাম|\\d+(?:\\.\\d{1,2})?)"), " ").replace(Regex("\\s+"), " ").trim()
        return cleaned.ifBlank { categoryName }.take(80)
    }
}
