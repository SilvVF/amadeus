package io.silv.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import io.silv.common.model.CardType

@Composable
fun UseList(
    checked: Boolean,
    onCheckChanged: (Boolean) -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckChanged,
            enabled = true,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Display manga in list.",
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
fun SelectCardType(
    cardType: CardType,
    onCardTypeSelected: (CardType) -> Unit,
) {
    val types = remember { CardType.entries }

    types.fastForEachIndexed { i, type ->
        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    onCardTypeSelected(type)
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            RadioButton(
                selected = cardType == type,
                onClick = {
                    onCardTypeSelected(type)
                },
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = type.string,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}
