package com.checkout.frames.mapper

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.checkout.base.mapper.Mapper
import com.checkout.frames.model.InputFieldColors
import com.checkout.frames.style.component.TextLabelStyle
import com.checkout.frames.style.component.InputFieldStyle
import com.checkout.frames.style.component.ContainerStyle
import com.checkout.frames.style.component.TextStyle
import com.checkout.frames.style.component.InputFieldIndicatorStyle
import com.checkout.frames.style.view.InputFieldViewStyle
import com.checkout.frames.style.view.TextLabelViewStyle
import com.checkout.frames.utils.extensions.errorIndicatorColor
import com.checkout.frames.utils.extensions.focusedIndicatorColor
import com.checkout.frames.utils.extensions.toComposeShape
import com.checkout.frames.utils.extensions.toComposeTextStyle
import com.checkout.frames.utils.extensions.unfocusedIndicatorColor
import com.checkout.frames.view.TextLabel
import com.checkout.frames.view.TextLabelState

internal class InputFieldStyleToViewStyleMapper(
    private val textLabelStyleMapper: Mapper<TextLabelStyle, TextLabelViewStyle>
) : Mapper<InputFieldStyle, InputFieldViewStyle> {

    override fun map(from: InputFieldStyle): InputFieldViewStyle = InputFieldViewStyle(
        modifier = provideModifier(from.containerStyle),
        textStyle = from.textStyle.toComposeTextStyle(),
        maxLines = from.textStyle.maxLines,
        maxLength = from.textStyle.maxLength,
        placeholder = providePlaceholder(from.placeholderText, from.placeholderStyle),
        containerShape = from.containerStyle.shape.toComposeShape(from.containerStyle.cornerRadius),
        borderShape = provideBorderShape(from.indicatorStyle),
        colors = provideColors(from)
    )

    @SuppressLint("ModifierFactoryExtensionFunction")
    private fun provideModifier(containerStyle: ContainerStyle): Modifier = with(containerStyle) {
        var modifier = Modifier.background(Color.Transparent)

        height?.let { modifier = modifier.height(it.dp) }
        modifier = width?.let { modifier.width(it.dp) } ?: modifier.fillMaxWidth()
        margin?.let { modifier = modifier.padding(it.start.dp, it.top.dp, it.end.dp, it.bottom.dp) }
        padding?.let { modifier = modifier.padding(it.start.dp, it.top.dp, it.end.dp, it.bottom.dp) }

        return modifier
    }

    private fun providePlaceholder(placeholderText: String, placeholderStyle: TextStyle): @Composable (() -> Unit) =
        @Composable {
            TextLabel(
                textLabelStyleMapper.map(TextLabelStyle(placeholderText, placeholderStyle)),
                TextLabelState(mutableStateOf(placeholderText), isVisible = mutableStateOf(true))
            )
        }

    private fun provideBorderShape(indicatorStyle: InputFieldIndicatorStyle): Shape? = when (indicatorStyle) {
        is InputFieldIndicatorStyle.Underline -> null
        is InputFieldIndicatorStyle.Border -> indicatorStyle.shape.toComposeShape(indicatorStyle.cornerRadius)
    }

    private fun provideColors(style: InputFieldStyle): InputFieldColors = InputFieldColors(
        textColor = Color(style.textStyle.color),
        placeholderColor = Color(style.placeholderStyle.color),
        focusedIndicatorColor = Color(style.indicatorStyle.focusedIndicatorColor()),
        unfocusedIndicatorColor = Color(style.indicatorStyle.unfocusedIndicatorColor()),
        errorIndicatorColor = Color(style.indicatorStyle.errorIndicatorColor()),
        containerColor = Color(style.containerStyle.color)
    )
}