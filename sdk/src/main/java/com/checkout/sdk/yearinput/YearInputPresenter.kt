package com.checkout.sdk.yearinput

import com.checkout.sdk.architecture.BasePresenter
import com.checkout.sdk.architecture.MvpView
import java.util.*


class YearInputPresenter(
    initialState: YearInputUiState = YearInputUiState.create(Calendar.getInstance())
) : BasePresenter<MvpView<YearInputUiState>, YearInputUiState>(initialState) {

    fun yearSelected(yearSelectedUseCaseBuilder: YearSelectedUseCase.Builder) {
        val yearSelectedUseCase = yearSelectedUseCaseBuilder.years(uiState.years).build()
        yearSelectedUseCase.execute()
        safeUpdateView(uiState.copy(position = yearSelectedUseCaseBuilder.position))
    }

    fun reset(yearResetUseCase: YearResetUseCase) {
        yearResetUseCase.execute()
        safeUpdateView(YearInputUiState(uiState.years))
    }

    fun showError(show: Boolean) {
        safeUpdateView(uiState.copy(showError = show))
    }
}