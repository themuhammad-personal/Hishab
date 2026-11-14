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

/**
 * Conservative offline parser. It only returns typed drafts; it has no repository/DAO dependency
 * and never writes. Ambiguous targets, wallets, categories or amounts produce a question instead
 * of a guess. The ViewModel must consume a draft exactly once before any confirmed mutation.
 */
class LocalAssistant {
    fun parse(
        input: String,
        categories: List<CategoryEntity>,
        wallets: List<WalletEntity>,
        transactions: List<TransactionEntity>,
        today: String = Dates.today().toString()
    ): AssistantReply {
        val text = Money.normalizeDigits(input).trim()
        if (text.isBlank()) return AssistantReply("কী হিসাব করতে চান লিখুন। যেমন: আজ বাজারে ৫০০ টাকা খরচ।")
        val baseDate = Dates.requireDate(today)
        val amount = extractAmount(text)
        val date = when {
            text.contains("গতকাল") || text.contains("yesterday", true) -> baseDate.minusDays(1).toString()
            text.contains("আগামীকাল") || text.contains("tomorrow", true) -> baseDate.plusDays(1).toString()
            else -> baseDate.toString()
        }
        val lower = text.lowercase()
        val activeWallets = wallets.filter { !it.archived }
        val activeCategories = categories.filter { !it.archived }
        val activeTransactions = transactions.filter { it.deletedAt == null }
        val isPay = listOf("পরিশোধ", "পেমেন্ট", "paid", "pay", "শোধ").any { lower.contains(it) }
        val isDelete = listOf("মুছে", "মুছুন", "delete", "remove", "বাদ").any { lower.contains(it) }

        if (isDelete) {
            val candidates = activeTransactions.filter { matchesTransaction(text, it) }
            if (candidates.size != 1) return AssistantReply("কোন লেনদেন মুছব নিশ্চিতভাবে চিহ্নিত করতে পারিনি। লেনদেন তালিকা থেকে নির্বাচন করুন।")
            val tx = candidates.single()
            return AssistantReply(
                "মুছে ফেলার খসড়া তৈরি করেছি। নিশ্চিত না করলে কিছু মুছবে না।",
                AssistantDraft.DeleteTransaction(
                    UUID.randomUUID().toString(), tx.id, "লেনদেন মুছে ফেলা",
                    listOf("শিরোনাম" to tx.title, "পরিমাণ" to Money.formatPoisha(tx.amountPoisha), "তারিখ" to Dates.formatDate(tx.date))
                )
            )
        }

        val walletMatches = activeWallets.filter { text.contains(it.name, true) || matchesWalletAlias(text, it.type) }.distinctBy { it.id }
        if (walletMatches.size > 1) {
            return AssistantReply("একাধিক ওয়ালেট মিলেছে: ${walletMatches.joinToString { it.name }}। কোন ওয়ালেট ব্যবহার করব স্পষ্ট করে লিখুন।")
        }
        val wallet = walletMatches.singleOrNull() ?: activeWallets.singleOrNull()

        if (isPay) {
            val candidates = activeTransactions.filter { it.dueDirection != null && it.amountPoisha - it.paidPoisha > 0 && matchesTransaction(text, it) }
            if (candidates.size != 1) return AssistantReply("কোন বাকি পরিশোধ করব নিশ্চিতভাবে চিহ্নিত করতে পারিনি। দোকান/ব্যক্তির নাম ও পরিমাণ লিখুন অথবা বাকির খাতা থেকে নির্বাচন করুন।")
            if (amount == null && Regex("\\d").containsMatchIn(text)) return AssistantReply("পরিশোধের পরিমাণ স্পষ্ট নয়। একটি টাকার পরিমাণ লিখুন, যেমন: ৫০০ টাকা পরিশোধ।")
            val tx = candidates.single()
            val remaining = tx.amountPoisha - tx.paidPoisha
            val payAmount = amount ?: remaining
            if (payAmount <= 0 || payAmount > remaining) return AssistantReply("পরিশোধের পরিমাণ বাকি টাকার মধ্যে হতে হবে। বাকি: ${Money.formatPoisha(remaining)}")
            if (wallet == null) return AssistantReply("কোন ওয়ালেট থেকে পরিশোধ করব? ${walletPrompt(activeWallets)}")
            return AssistantReply(
                "পরিশোধের খসড়া তৈরি করেছি। নিশ্চিত করার আগে মিলিয়ে নিন।",
                AssistantDraft.PayDue(
                    UUID.randomUUID().toString(), tx.id, wallet.id, payAmount, date, "বাকি পরিশোধ",
                    listOf(
                        "দোকান/ব্যক্তি" to (tx.shopOrPerson ?: tx.title),
                        "বাকি ছিল" to Money.formatPoisha(remaining),
                        "পরিশোধ" to Money.formatPoisha(payAmount),
                        "ওয়ালেট" to wallet.name,
                        "বাকি থাকবে" to Money.formatPoisha(remaining - payAmount)
                    )
                )
            )
        }

        if (amount == null || amount <= 0) return AssistantReply("টাকার পরিমাণ বুঝতে পারিনি। একটি পরিমাণ লিখুন, যেমন: আজ বাজারে ৫০০ টাকা খরচ।")
        val kind = if (listOf("আয়", "আয়ের", "income", "বেতন", "salary", "পেলাম", "received").any { lower.contains(it) }) EntryKind.INCOME else EntryKind.EXPENSE
        val kindCategories = activeCategories.filter { it.kind == kind }
        val categoryMatches = kindCategories.filter { text.contains(it.name, true) || matchesCategoryAlias(lower, it.name) }.distinctBy { it.id }
        if (categoryMatches.size > 1) return AssistantReply("একাধিক ক্যাটাগরি মিলেছে: ${categoryMatches.joinToString { it.name }}। কোন ক্যাটাগরি ব্যবহার করব স্পষ্ট করে লিখুন।")
        val category = categoryMatches.singleOrNull() ?: kindCategories.singleOrNull()
            ?: return AssistantReply("ক্যাটাগরি নিশ্চিতভাবে চিহ্নিত করতে পারিনি। ক্যাটাগরির নাম লিখুন বা লেনদেন ফর্ম থেকে নির্বাচন করুন।")
        val paid = !listOf("বাকি", "বাকিতে", "due", "unpaid", "পাব", "দেব").any { lower.contains(it) }
        if (paid && wallet == null) return AssistantReply("কোন ওয়ালেট ব্যবহার করব? ${walletPrompt(activeWallets)}")
        val title = buildTitle(text, category.name)
        val lines = listOf(
            "ধরন" to if (kind == EntryKind.EXPENSE) "খরচ" else "আয়",
            "শিরোনাম" to title,
            "পরিমাণ" to Money.formatPoisha(amount),
            "ক্যাটাগরি" to category.name,
            "ওয়ালেট" to (wallet?.name ?: "বাকি হিসাবে থাকবে"),
            "তারিখ" to Dates.formatDate(date),
            "অবস্থা" to if (paid) "পরিশোধিত" else "বাকি"
        )
        return AssistantReply(
            "হিসাবের খসড়া তৈরি করেছি। নিশ্চিত না করলে খাতা বদলাবে না।",
            AssistantDraft.AddTransaction(UUID.randomUUID().toString(), kind, title, amount, category.id, wallet?.id, date, paid, if (kind == EntryKind.EXPENSE) "নতুন খরচ" else "নতুন আয়", lines)
        )
    }

    private fun walletPrompt(wallets: List<WalletEntity>): String =
        if (wallets.isEmpty()) "একটি সক্রিয় ওয়ালেট আগে তৈরি করুন।" else "সক্রিয় ওয়ালেট: ${wallets.joinToString { it.name }}।"

    private fun matchesTransaction(text: String, tx: TransactionEntity): Boolean =
        text.contains(tx.title, true) || tx.shopOrPerson?.let { text.contains(it, true) } == true

    private fun extractAmount(text: String): Long? {
        val moneyWords = Regex("(\\d+(?:\\.\\d{1,2})?)\\s*(?:টাকা|৳|taka|bdt)", RegexOption.IGNORE_CASE)
            .findAll(text).mapNotNull { Money.parsePoisha(it.groupValues[1]) }.distinct().toList()
        if (moneyWords.size == 1) return moneyWords.single()
        if (moneyWords.size > 1) return null
        val symbols = Regex("(?:৳|\\$)\\s*(\\d+(?:\\.\\d{1,2})?)")
            .findAll(text).mapNotNull { Money.parsePoisha(it.groupValues[1]) }.distinct().toList()
        if (symbols.size == 1) return symbols.single()
        if (symbols.size > 1) return null
        val nums = Regex("\\d+(?:\\.\\d{1,2})?").findAll(text).map { it.value }.distinct().toList()
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
        val cleaned = text.replace(Regex("(আজ|গতকাল|আগামীকাল|টাকা|৳|খরচ|আয়|পরিশোধ|করলাম|করেছি|দিলাম|পেলাম|\\d+(?:\\.\\d{1,2})?)"), " ")
            .replace(Regex("\\s+"), " ").trim()
        return cleaned.ifBlank { categoryName }.take(80)
    }
}
