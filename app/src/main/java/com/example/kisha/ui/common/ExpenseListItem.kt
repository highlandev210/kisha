package com.example.kisha.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.kisha.R
import com.example.kisha.data.db.CategoryEntity
import com.example.kisha.data.db.ExpenseEntity
import com.example.kisha.ui.format.LocalMoneyFormatter
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class ExpenseRow(val expense: ExpenseEntity, val category: CategoryEntity?)

private val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

@Composable
fun ExpenseListItem(row: ExpenseRow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val formatter = LocalMoneyFormatter.current
    val time = row.expense.occurredAt.atZone(ZoneId.systemDefault()).toLocalTime().format(timeFormat)
    val note = row.expense.note.takeIf { it.isNotBlank() }
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Icon(
                    categoryIcon(row.category?.icon),
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp).size(24.dp),
                )
            }
        },
        supportingContent = { Text(listOfNotNull(note, time).joinToString(" · ")) },
        trailingContent = {
            Text(formatter.format(row.expense.amountPaise), style = MaterialTheme.typography.titleMedium)
        },
    ) {
        Text(row.category?.name ?: stringResource(R.string.expense_no_category))
    }
}
