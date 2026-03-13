package com.example.autodialer.domain.repository

import com.example.autodialer.domain.model.Debtor

interface DebtorRepository {
    suspend fun fetchDebtors(): List<Debtor>
}
