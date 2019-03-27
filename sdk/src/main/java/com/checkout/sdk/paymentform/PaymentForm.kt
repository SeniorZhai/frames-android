package com.checkout.sdk.paymentform

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.checkout.sdk.CheckoutClient
import com.checkout.sdk.FormCustomizer
import com.checkout.sdk.R
import com.checkout.sdk.animation.SlidingViewAnimator
import com.checkout.sdk.architecture.MvpView
import com.checkout.sdk.architecture.PresenterStore
import com.checkout.sdk.carddetails.CardDetailsView
import com.checkout.sdk.core.RequestGenerator
import com.checkout.sdk.store.DataStore
import com.checkout.sdk.store.InMemoryStore
import com.checkout.sdk.utils.DateFormatter
import com.checkout.sdk.view.BillingDetailsView
import kotlinx.android.synthetic.main.payment_form.view.*

/**
 * Contains helper methods dealing with the tokenisation or payment from customisation
 *
 *
 * Most of the methods that include interaction with the Checkout.com API rely on
 * callbacks to communicate outcomes. Please make sure you set the key/environment
 * and appropriate  callbacks to a ensure successful interaction
 */
class PaymentForm @JvmOverloads constructor(
    private val mContext: Context,
    attrs: AttributeSet? = null
) : RelativeLayout(mContext, attrs), MvpView<PaymentFormUiState> {

    private val inMemoryStore = InMemoryStore.Factory.get()
    private lateinit var checkoutClient: CheckoutClient
    private var formCustomizer: FormCustomizer? = null
    private lateinit var presenter: PaymentFormPresenter
    private val slidingViewAnimator: SlidingViewAnimator = SlidingViewAnimator(context)
    private var m3DSecureListener: On3DSFinished? = null
    private val mDataStore = DataStore.getInstance()

    /**
     * This is a callback used to go back to the card details view from the billing page
     * and based on the action used decide is the billing spinner will be updated
     */
    private val mBillingListener = object : BillingDetailsView.Listener {
        override fun onBillingCompleted() {
            card_details_view.updateBillingSpinner()
            slidingViewAnimator.transitionOutToRight(billing_details_view, card_details_view)
        }

        override fun onBillingCanceled() {
            card_details_view.updateBillingSpinner()
            slidingViewAnimator.transitionOutToRight(billing_details_view, card_details_view)
        }
    }

    private var validPayRequestListener: ValidPayRequestListener =
        object : ValidPayRequestListener {
            override fun onValidPayRequest() {
                val getTokenUseCaseBuilder = GetTokenUseCase.Builder(
                    RequestGenerator(inMemoryStore, mDataStore, DateFormatter()),
                    checkoutClient
                )
                presenter.getToken(getTokenUseCaseBuilder)
            }
        }

    interface ValidPayRequestListener {
        fun onValidPayRequest()
    }

    /**
     * This is interface used as a callback for when the 3D secure functionality is used
     */
    interface On3DSFinished {
        fun onSuccess(token: String)

        fun onError(errorMessage: String)
    }

    /**
     * This method is used to initialise the UI of the module
     */
    init {
        // Set up the layout
        inflate(mContext, R.layout.payment_form, this)

        card_details_view.setGoToBillingListener(object: CardDetailsView.GoToBillingListener {
            override fun onGoToBilling() {
                slidingViewAnimator.transitionInFromRight(billing_details_view, card_details_view)
            }
        })

        card_details_view.setValidPayRequestListener(validPayRequestListener)
        billing_details_view.setGoToCardDetailsListener(mBillingListener)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        presenter = PresenterStore.getOrCreate(
            PaymentFormPresenter::class.java,
            { PaymentFormPresenter() })
        presenter.start(this)
    }

    /**
     * Must be called in order to initialize the Payment form with:
     * - CheckoutClient (mandatory), where you set the key, the environment and the token callback
     * - FormCustomizer (optional), where you can customize elements of the UI
     * (e.g. display icons for you choice of accepted card types)
     */
    fun initialize(checkoutClient: CheckoutClient, formCustomizer: FormCustomizer? = null) {
        this.checkoutClient = checkoutClient
        this.formCustomizer = formCustomizer
    }

    override fun onStateUpdated(uiState: PaymentFormUiState) {
        uiState.inProgress?.let { card_details_view.showProgress(it) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        presenter.stop()
        slidingViewAnimator.cancelAnimations()
    }

    /**
     * This method is used to handle 3D Secure URLs.
     *
     *
     * It wil programmatically generate a WebView and listen for when the url changes
     * in either the success url or the fail url.
     *
     * @param url        the 3D Secure url
     * @param successUrl the Redirection url set up in the Checkout.com HUB
     * @param failsUrl   the Redirection Fail url set up in the Checkout.com HUB
     */
    fun handle3DS(url: String, successUrl: String, failsUrl: String) {
        visibility = View.GONE // dismiss the card form UI
        val web = WebView(mContext)
        web.loadUrl(url)
        web.settings.javaScriptEnabled = true
        web.webViewClient = object : WebViewClient() {
            // Listen for when teh URL changes and match t with either the success of fail url
            override fun onPageFinished(view: WebView, url: String) {
                if (url.contains(successUrl)) {
                    val uri = Uri.parse(url)
                    val paymentToken = uri.getQueryParameter("cko-payment-token")
                    m3DSecureListener?.onSuccess(paymentToken)
                } else if (url.contains(failsUrl)) {
                    val uri = Uri.parse(url)
                    val paymentToken = uri.getQueryParameter("cko-payment-token")
                    m3DSecureListener?.onError(paymentToken)
                }
            }
        }
        // Make WebView fill the layout
        web.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        addView(web)
    }

    /**
     * This method used to clear the state and fields of the Payment Form
     */
    fun clearForm() {
        formCustomizer?.resetState()
        card_details_view.resetFields()
        billing_details_view.resetFields()
    }

    /**
     * This method used to set a callback for when the 3D Secure handling.
     */
    fun set3DSListener(listener: On3DSFinished): PaymentForm {
        this.m3DSecureListener = listener
        return this
    }
}