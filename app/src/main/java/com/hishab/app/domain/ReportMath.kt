package com.hishab.app.domain

object ReportMath {
    /** Exact relative sweeps; caller may use raw values for canvas angles. */
    fun percentLabels(values: List<Pair<String, Long>>): List<ChartSlice> {
        val total = values.sumOf { it.second.coerceAtLeast(0) }
        if (total <= 0) return values.map { ChartSlice(it.first, it.second.coerceAtLeast(0), 0) }
        val floors = values.map { (label, value) ->
            val v = value.coerceAtLeast(0)
            val exact = v * 100.0 / total
            Triple(ChartSlice(label, v, exact.toInt()), exact - exact.toInt(), v)
        }
        var remaining = 100 - floors.sumOf { it.first.percent }
        val order = floors.indices.sortedByDescending { floors[it].second }
        val result = floors.map { it.first }.toMutableList()
        var i = 0
        while (remaining > 0 && order.isNotEmpty()) {
            val idx = order[i % order.size]
            result[idx] = result[idx].copy(percent = result[idx].percent + 1)
            remaining--; i++
        }
        return result
    }
}
