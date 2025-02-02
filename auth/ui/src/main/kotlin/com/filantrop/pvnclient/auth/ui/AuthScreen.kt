package com.filantrop.pvnclient.auth.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filantrop.pvnclient.core.common.Constants.SPACE
import com.filantrop.pvnclient.core.designsystem.icon.PVNIcons
import org.koin.androidx.compose.koinViewModel

private const val WEIGHT = 0.5f

@Composable
fun AuthScreen(viewModel: AuthViewModel = koinViewModel()) {
    val uiState: AuthState by viewModel.uiState.collectAsStateWithLifecycle()
    AuthScreen(
        uiState,
    ) { viewModel.login(it) }
}

@Composable
@Suppress("UnusedParameter")
fun AuthScreen(
    state: AuthState,
    onLoginClick: (token: String) -> Unit,
) {
    Scaffold(
        modifier = Modifier.navigationBarsPadding(),
    ) { padding: PaddingValues ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.weight(WEIGHT))
            Card(
                shape = CircleShape,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
            ) {
                Icon(
                    modifier = Modifier
                        .size(148.dp),
                    imageVector = PVNIcons.Person,
                    contentDescription = "person",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            ClickableLink()
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun ClickableLink() {
    val annotatedString = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
            ),
        ) {
            append(stringResource(id = R.string.use_telegram_bot))
            append(SPACE)
        }
        withLink(LinkAnnotation.Url(url = stringResource(id = R.string.telegram_bot_link))) {
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.secondary,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                append(stringResource(id = R.string.telegram_bot))
            }
        }
        withStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
            ),
        ) {
            append(SPACE)
            append(stringResource(id = R.string.get_token_telegram_bot))
        }
    }
    Text(
        text = annotatedString,
        modifier = Modifier
            .padding(dimensionResource(id = com.filantrop.pvnclient.core.designsystem.R.dimen.padding_medium)),
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultAuthScreen() {
    AuthScreen(
        AuthState(token = "123"),
    ) {}
}
