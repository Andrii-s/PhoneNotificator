package com.example.autodialer.domain.model

data class Debtor(
    val id: Long,
    val name: String,
    val phone: String,
    val debt: Double
)
