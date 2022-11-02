package com.checkout.frames.screen.paymentdetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.checkout.frames.di.base.Injector
import com.checkout.frames.screen.navigation.Screen
import com.checkout.frames.style.screen.PaymentDetailsStyle
import com.checkout.frames.utils.constants.PaymentDetailsScreenConstants
import com.checkout.frames.view.TextLabel

@SuppressWarnings("MagicNumber")
@Composable
internal fun PaymentDetailsScreen(
    style: PaymentDetailsStyle,
    injector: Injector,
    navController: NavController
) {
    val viewModel: PaymentDetailsViewModel = viewModel(
        factory = PaymentDetailsViewModel.Factory(injector, style)
    )

    Column {
        TextLabel(style = viewModel.headerStyle, state = viewModel.headerState)

        Column(
            modifier = Modifier
                .padding(
                    start = PaymentDetailsScreenConstants.padding.dp,
                    end = PaymentDetailsScreenConstants.padding.dp
                )
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            viewModel.componentProvider.CardScheme(style = style.cardSchemeStyle)

            viewModel.componentProvider.CardNumber(style = style.cardNumberStyle)

            Spacer(modifier = Modifier.padding(top = 24.dp))

            viewModel.componentProvider.ExpiryDate(style = style.expiryDateComponentStyle)

            Spacer(modifier = Modifier.padding(top = 24.dp))

            viewModel.componentProvider.Cvv(style = style.cvvComponentStyle)

            Spacer(modifier = Modifier.padding(top = 24.dp))

            viewModel.componentProvider.AddressSummary(style.addressSummaryComponentStyle) {
                navController.navigate(Screen.BillingFormScreen.route)
            }
            Spacer(modifier = Modifier.padding(top = 32.dp))

            viewModel.componentProvider.PayButton(style = style.payButtonComponentStyle)
        }
    }
}
