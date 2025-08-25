package org.example.habitstreak.domain.usecase.util

import kotlinx.coroutines.flow.Flow

interface UseCase<in Params, out Result> {
    suspend operator fun invoke(params: Params): Result
}

interface FlowUseCase<in Params, out Result> {
    operator fun invoke(params: Params): Flow<Result>
}