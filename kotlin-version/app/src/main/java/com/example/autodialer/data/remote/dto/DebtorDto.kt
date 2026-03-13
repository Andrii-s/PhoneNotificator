package com.example.autodialer.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DebtorDto(
    @SerializedName("id")    val id: Long,
    @SerializedName("name")  val name: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("debt")  val debt: Double
)
