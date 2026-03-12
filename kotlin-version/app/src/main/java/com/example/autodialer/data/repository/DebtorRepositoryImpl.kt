package com.example.autodialer.data.repository

import com.example.autodialer.data.remote.ApiService
import com.example.autodialer.domain.model.Debtor
import com.example.autodialer.domain.repository.DebtorRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebtorRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : DebtorRepository {

    /**
     * Fetches the debtor list from the remote API.
     *
     * @throws Exception with a descriptive message when the network call fails
     *         or the server returns a non-2xx status code.
     */
    override suspend fun fetchDebtors(): List<Debtor> {
        return try {
            val response = apiService.getDebtors()
            if (response.isSuccessful) {
                response.body()?.map { dto ->
                    Debtor(
                        id    = dto.id,
                        name  = dto.name,
                        phone = dto.phone,
                        debt  = dto.debt
                    )
                } ?: emptyList()
            } else {
                throw Exception("Failed to fetch debtors: HTTP ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            // Re-throw so the caller / ViewModel can handle it appropriately.
            throw Exception("Error fetching debtors: ${e.message}", e)
        }
    }
}
