package com.example.kisha.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.kisha.R
import com.example.kisha.allowance.AllowanceStatus
import com.example.kisha.allowance.DayAllowance
import com.example.kisha.ui.format.LocalMoneyFormatter

/** HM-2 state colours, always from theme roles. */
@Composable
fun statusColor(status: AllowanceStatus): Color = when (status) {
    AllowanceStatus.ON_TRACK -> MaterialTheme.colorScheme.primary
    AllowanceStatus.LOW -> MaterialTheme.colorScheme.tertiary
    AllowanceStatus.OVER -> MaterialTheme.colorScheme.error
}

/** DP-6: state is an icon plus words, never colour alone. */
@Composable
fun StatusLabel(allowance: DayAllowance, style: TextStyle, modifier: Modifier = Modifier) {
    val formatter = LocalMoneyFormatter.current
    val color = statusColor(allowance.status)
    val (icon, label) = when (allowance.status) {
        AllowanceStatus.ON_TRACK -> Icons.Filled.CheckCircle to stringResource(R.string.status_on_track)
        AllowanceStatus.LOW -> Icons.Filled.Warning to stringResource(R.string.status_low)
        AllowanceStatus.OVER ->
            Icons.Filled.Error to stringResource(R.string.status_over, formatter.format(-allowance.remainingPaise))
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = color)
        Text(label, style = style, color = color)
    }
}
