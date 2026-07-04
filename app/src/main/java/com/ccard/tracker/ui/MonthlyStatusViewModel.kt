package com.ccard.tracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ccard.tracker.CCardApplication
import com.ccard.tracker.data.CardCondition
import com.ccard.tracker.domain.CardStatus
import com.ccard.tracker.domain.MonthlyPerformanceCalculator
import com.ccard.tracker.sms.SmsImporter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MonthlyStatusViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as CCardApplication).database

    val statuses = combine(
        db.cardConditionDao().observeAll(),
        db.transactionDao().observeAll(),
    ) { conditions, transactions ->
        conditions.map { condition -> MonthlyPerformanceCalculator.calculate(condition, transactions) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList<CardStatus>(),
    )

    val conditions = db.cardConditionDao().observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun addCondition(condition: CardCondition) {
        viewModelScope.launch { db.cardConditionDao().upsert(condition) }
    }

    fun deleteCondition(condition: CardCondition) {
        viewModelScope.launch { db.cardConditionDao().delete(condition) }
    }

    fun importExistingSms() {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val imported = SmsImporter.importExisting(context)
            db.transactionDao().insertAll(imported)
        }
    }
}
