package com.checkout.tokenization.repository

import com.checkout.base.usecase.UseCase
import com.checkout.mock.CardTokenTestData
import com.checkout.network.response.ErrorResponse
import com.checkout.network.response.NetworkApiResponse
import com.checkout.tokenization.TokenNetworkApiClient
import com.checkout.tokenization.error.TokenizationError
import com.checkout.tokenization.logging.TokenizationLogger
import com.checkout.tokenization.mapper.request.CardToTokenRequestMapper
import com.checkout.tokenization.mapper.response.CardTokenizationNetworkDataMapper
import com.checkout.tokenization.model.Card
import com.checkout.tokenization.model.CardTokenRequest
import com.checkout.tokenization.model.GooglePayTokenRequest
import com.checkout.tokenization.response.TokenDetailsResponse
import com.checkout.tokenization.utils.TokenizationConstants
import com.checkout.validation.model.ValidationResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
internal class TokenRepositoryImplTest {
    @RelaxedMockK
    private lateinit var mockTokenNetworkApiClient: TokenNetworkApiClient

    @RelaxedMockK
    private lateinit var mockValidateTokenizationDataUseCase: UseCase<Card, ValidationResult<Unit>>

    @RelaxedMockK
    private lateinit var mockTokenizationLogger: TokenizationLogger

    private lateinit var tokenRepositoryImpl: TokenRepositoryImpl

    @BeforeEach
    fun setUp() {
        tokenRepositoryImpl = TokenRepositoryImpl(
            mockTokenNetworkApiClient,
            CardToTokenRequestMapper(),
            CardTokenizationNetworkDataMapper(),
            mockValidateTokenizationDataUseCase,
            mockTokenizationLogger,
            "test_key"
        )
    }

    @DisplayName("CardToken Details invocation")
    @Nested
    inner class GetCardTokenDetails {
        @Test
        fun `when sendCardTokenRequest invoked with success response then success handler invoked`() {
            testCardTokenResultInvocation(
                true,
                NetworkApiResponse.Success(CardTokenTestData.tokenDetailsResponse())
            )
        }

        @Test
        fun `when sendCardTokenRequest invoked with network error response then failure handler invoked`() {
            testCardTokenResultInvocation(
                false,
                NetworkApiResponse.NetworkError(NullPointerException())
            )
        }

        @Test
        fun `when sendCardTokenRequest invoked with server error response then failure handler invoked`() {
            testCardTokenResultInvocation(
                false,
                NetworkApiResponse.ServerError(null, 123)
            )
        }

        @Test
        fun `when sendCardTokenRequest invoked with internal error response then failure handler invoked`() {
            testCardTokenResultInvocation(
                false,
                NetworkApiResponse.InternalError(
                    TokenizationError(
                        "dummy code",
                        "exception.message",
                        null
                    )
                )
            )
        }

        @Test
        fun `when sendCardTokenRequest invoked with success then log tokenResponseEvent along with resetSession`() {
            testCardTokenEventInvocation(true)
        }

        @Test
        fun `when sendCardTokenRequest invoked with servererror then log tokenResponseEvent along with resetSession`() {
            testCardTokenEventInvocation(false)
        }

        @Test
        fun `when sendCardTokenRequest invoked then send card token request with correct data is invoked`() = runTest {
            // Given
            val response = mockk<NetworkApiResponse<TokenDetailsResponse>>()

            val testDispatcher = UnconfinedTestDispatcher(testScheduler)
            Dispatchers.setMain(testDispatcher)

            tokenRepositoryImpl.networkCoroutineScope = CoroutineScope(StandardTestDispatcher(testScheduler))

            every { mockValidateTokenizationDataUseCase.execute(any()) } returns ValidationResult.Success(Unit)
            coEvery { mockTokenNetworkApiClient.sendCardTokenRequest(any()) } returns response

            // When
            tokenRepositoryImpl.sendCardTokenRequest(
                CardTokenRequest(
                    CardTokenTestData.card,
                    onSuccess = { },
                    onFailure = { }
                )
            )

            // Then
            launch {
                verify(exactly = 1) {
                    mockTokenizationLogger.logTokenRequestEvent(
                        TokenizationConstants.CARD,
                        "test_key"
                    )
                }
            }
        }
    }

        private fun testCardTokenResultInvocation(
            successHandlerInvoked: Boolean,
            response: NetworkApiResponse<TokenDetailsResponse>
        ) =
            runTest {
                // Given
                var isSuccess = false

                val testDispatcher = UnconfinedTestDispatcher(testScheduler)
                Dispatchers.setMain(testDispatcher)

                tokenRepositoryImpl.networkCoroutineScope = CoroutineScope(StandardTestDispatcher(testScheduler))

                coEvery { mockValidateTokenizationDataUseCase.execute(any()) } returns ValidationResult.Success(Unit)

                coEvery { mockTokenNetworkApiClient.sendCardTokenRequest(any()) } returns response

                // When
                tokenRepositoryImpl.sendCardTokenRequest(
                    CardTokenRequest(
                        CardTokenTestData.card,
                        onSuccess = { isSuccess = true },
                        onFailure = { isSuccess = false }
                    )
                )

                // Then
                launch {
                    if (successHandlerInvoked) assertTrue(isSuccess)
                    else assertFalse(isSuccess)
                }
            }

        private fun testCardTokenEventInvocation(isSuccessResponse: Boolean) =
            runTest {
                // Given
                val successBody = mockk<TokenDetailsResponse>()
                val serverErrorBody = mockk<ErrorResponse>()

                val response = if (isSuccessResponse) {
                    NetworkApiResponse.Success(successBody)
                } else {
                    NetworkApiResponse.ServerError(serverErrorBody, 501)
                }

                val testDispatcher = UnconfinedTestDispatcher(testScheduler)
                Dispatchers.setMain(testDispatcher)

                tokenRepositoryImpl.networkCoroutineScope = CoroutineScope(StandardTestDispatcher(testScheduler))

                every { mockValidateTokenizationDataUseCase.execute(any()) } returns ValidationResult.Success(Unit)
                coEvery { mockTokenNetworkApiClient.sendCardTokenRequest(any()) } returns response

                // When
                tokenRepositoryImpl.sendCardTokenRequest(
                    CardTokenRequest(
                        CardTokenTestData.card,
                        onSuccess = { },
                        onFailure = { }
                    )
                )

                // Then
                launch {
                    if (isSuccessResponse) {
                        verify(exactly = 1) {
                            mockTokenizationLogger.logTokenResponseEvent(
                                eq(TokenizationConstants.CARD),
                                eq("test_key"),
                                eq(successBody)
                            )
                        }
                    } else {
                        verify(exactly = 1) {
                            mockTokenizationLogger.logTokenResponseEvent(
                                eq(TokenizationConstants.CARD),
                                eq("test_key"),
                                null,
                                501,
                                serverErrorBody
                            )
                        }
                    }

                    verify(exactly = 1) { mockTokenizationLogger.resetSession() }
                }
    }

    @DisplayName("GooglePayToken Details invocation")
    @Nested
    inner class GetGooglePayTokenNetworkRequestDetails {
        @Test
        fun `when sendGooglePayTokenRequest invoked with success response then success handler invoked`() {
            testGooglePayTokenResultInvocation(
                true,
                NetworkApiResponse.Success(CardTokenTestData.tokenDetailsResponse())
            )
        }

        @Test
        fun `when sendGooglePayTokenRequest invoked with network error response then failure handler invoked`() {
            testGooglePayTokenResultInvocation(
                false,
                NetworkApiResponse.NetworkError(Exception("Network connection lost"))
            )
        }

        @Test
        fun `when sendGooglePayTokenRequest invoked with server error response then failure handler invoked`() {
            testGooglePayTokenResultInvocation(
                false,
                NetworkApiResponse.ServerError(null, 123)
            )
        }

        @Test
        fun `when sendGooglePayTokenRequest invoked with internal error response then failure handler invoked`() {
            testGooglePayTokenResultInvocation(
                false,
                NetworkApiResponse.InternalError(
                    TokenizationError(
                        TokenizationError.GOOGLE_PAY_REQUEST_PARSING_ERROR,
                        "exception.message",
                        null
                    )
                )
            )
        }

        @Test
        fun `when sendGooglePayTokenRequest error invoked with invalid jsonPayLoad request then throw custom TokenizationError`() {
            testGooglePayErrorHandlerInvocation(
                NetworkApiResponse.InternalError(
                    TokenizationError(
                        TokenizationError.GOOGLE_PAY_REQUEST_PARSING_ERROR,
                        "JSONObject[\"protocolVersion\"] not found.",
                        null
                    )
                )
            )
        }

        @Test
        fun `when sendGooglePayTokenRequest invoked success then log tokenResponseEvent along with resetSession`() {
            testGooglePayTokenEventInvocation(true)
        }

        @Test
        fun `when sendGooglePayTokenRequest invoked serverError then log tokenResponseEvent along with resetSession`() {
            testGooglePayTokenEventInvocation(false)
        }

        @Test
        fun `when sendGooglePayTokenRequest invoked internalError then log tokenResponseEvent along with resetSession`() =
            runTest {
                // Given
                val expectedInternalErrorBody =
                    TokenizationError(
                        TokenizationError.GOOGLE_PAY_REQUEST_PARSING_ERROR,
                        "testMessage",
                        java.lang.NullPointerException()
                    )
                val captureError = mutableListOf<Throwable?>()

                val testDispatcher = UnconfinedTestDispatcher(testScheduler)
                Dispatchers.setMain(testDispatcher)

                tokenRepositoryImpl.networkCoroutineScope = CoroutineScope(StandardTestDispatcher(testScheduler))

                coEvery {
                    mockTokenNetworkApiClient.sendGooglePayTokenRequest(any())
                } throws (expectedInternalErrorBody)

                // When
                tokenRepositoryImpl.sendGooglePayTokenRequest(
                    GooglePayTokenRequest(
                        "{protocolVersion: ECv1,signature: “test”,signedMessage: testSignedMessage}",
                        onSuccess = { },
                        onFailure = { }
                    )
                )

                // Then
                launch {
                    verify(exactly = 1) {
                        mockTokenizationLogger.logErrorOnTokenRequestedEvent(
                            any(),
                            any(),
                            captureNullable(captureError)
                        )
                    }
                    assertEquals(
                        expectedInternalErrorBody.errorCode,
                        (captureError.firstOrNull() as? TokenizationError)?.errorCode
                    )
                    assertEquals(
                        expectedInternalErrorBody.message,
                        (captureError.firstOrNull() as? TokenizationError)?.message
                    )
                    assertEquals(
                        expectedInternalErrorBody.cause,
                        (captureError.firstOrNull() as? TokenizationError)?.cause
                    )
                }
            }

        @Test
        fun `when sendGooglePayTokenRequest invoked then send google pay token request with correct data is invoked`() =
            runTest {
                // Given
                val response = mockk<NetworkApiResponse<TokenDetailsResponse>>()

                val testDispatcher = UnconfinedTestDispatcher(testScheduler)
                Dispatchers.setMain(testDispatcher)

                tokenRepositoryImpl.networkCoroutineScope = CoroutineScope(StandardTestDispatcher(testScheduler))

                coEvery { mockTokenNetworkApiClient.sendCardTokenRequest(any()) } returns response

                // When
                tokenRepositoryImpl.sendGooglePayTokenRequest(
                    GooglePayTokenRequest(
                        "{protocolVersion: ECv1,signature: “test”,signedMessage: testSignedMessage}",
                        onSuccess = { },
                        onFailure = { }
                    )
                )

                // Then
                launch {
                    verify(exactly = 1) {
                        mockTokenizationLogger.logTokenRequestEvent(TokenizationConstants.GOOGLE_PAY, "test_key")
                    }
                }
            }

        private fun testGooglePayTokenResultInvocation(
            successHandlerInvoked: Boolean,
            response: NetworkApiResponse<TokenDetailsResponse>
        ) =
            runTest {
                // Given
                var isSuccess: Boolean? = null

                val testDispatcher = UnconfinedTestDispatcher(testScheduler)
                Dispatchers.setMain(testDispatcher)

                tokenRepositoryImpl.networkCoroutineScope = CoroutineScope(StandardTestDispatcher(testScheduler))

                coEvery { mockTokenNetworkApiClient.sendGooglePayTokenRequest(any()) } returns response

                // When
                tokenRepositoryImpl.sendGooglePayTokenRequest(
                    GooglePayTokenRequest(
                        "{protocolVersion: ECv1,signature: “test”,signedMessage: testSignedMessage}",
                        onSuccess = { isSuccess = true },
                        onFailure = { isSuccess = false }
                    )
                )

                // Then
                launch {
                    assertEquals(isSuccess.toString(), successHandlerInvoked.toString())
                }
            }

        private fun testGooglePayErrorHandlerInvocation(
            response: NetworkApiResponse<TokenDetailsResponse>
        ) = runTest {
            // Given
            var isSuccess: Boolean? = null
            var errorMessage = ""
            val successHandlerInvoked = false

            val testDispatcher = UnconfinedTestDispatcher(testScheduler)
            Dispatchers.setMain(testDispatcher)

            tokenRepositoryImpl.networkCoroutineScope = CoroutineScope(StandardTestDispatcher(testScheduler))

            coEvery { mockTokenNetworkApiClient.sendGooglePayTokenRequest(any()) } returns response

            // When
            tokenRepositoryImpl.sendGooglePayTokenRequest(
                GooglePayTokenRequest(
                    "{test: ECv1,signature: “testSignature”,signedMessage: testSignedMessage}",
                    onSuccess = { isSuccess = true },
                    onFailure = {
                        isSuccess = false
                        errorMessage = it
                    }
                )
            )

            // Then
            launch {
                assertEquals(isSuccess.toString(), successHandlerInvoked.toString())
                assertEquals(errorMessage, (response as? NetworkApiResponse.InternalError)?.throwable?.message ?: "_")
            }
        }

        private fun testGooglePayTokenEventInvocation(isSuccessResponse: Boolean) =
            runTest {
                // Given
                val successBody = mockk<TokenDetailsResponse>()
                val serverErrorBody = mockk<ErrorResponse>()

                val response = if (isSuccessResponse) {
                    NetworkApiResponse.Success(successBody)
                } else {
                    NetworkApiResponse.ServerError(serverErrorBody, 501)
                }

                val testDispatcher = UnconfinedTestDispatcher(testScheduler)
                Dispatchers.setMain(testDispatcher)

                tokenRepositoryImpl.networkCoroutineScope = CoroutineScope(StandardTestDispatcher(testScheduler))

                coEvery { mockTokenNetworkApiClient.sendGooglePayTokenRequest(any()) } returns response

                // When
                tokenRepositoryImpl.sendGooglePayTokenRequest(
                    GooglePayTokenRequest(
                        "{protocolVersion: ECv1,signature: “test”,signedMessage: testSignedMessage}",
                        onSuccess = { },
                        onFailure = { }
                    )
                )

                // Then
                launch {
                    if (isSuccessResponse) {
                        verify(exactly = 1) {
                            mockTokenizationLogger.logTokenResponseEvent(
                                eq(TokenizationConstants.GOOGLE_PAY),
                                eq("test_key"),
                                eq(successBody)
                            )
                        }
                    } else {
                        verify(exactly = 1) {
                            mockTokenizationLogger.logTokenResponseEvent(
                                eq(TokenizationConstants.GOOGLE_PAY),
                                eq("test_key"),
                                null,
                                501,
                                serverErrorBody
                            )
                        }
                    }
                    verify(exactly = 1) { mockTokenizationLogger.resetSession() }
                }
            }
    }
}
