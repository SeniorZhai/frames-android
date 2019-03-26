package com.checkout.sdk.architecture

/**
 * BasePresenter is responsible for
 * - starting and stopping of the presenter
 * - keeping track of ui state
 * - safely updating the view
 */
abstract class BasePresenter<in V : MvpView<U>, U: UiState> (uiState: U) {

    private var view: V? = null

    protected var uiState: U = uiState
        private set

    fun start(view: V) {
        this.view = view
        safeUpdateView(uiState)
    }

    fun stop() {
        view = null
    }

    fun safeUpdateView(uiState: U) {
        this.uiState = uiState
        view?.onStateUpdated(uiState)
    }
}
