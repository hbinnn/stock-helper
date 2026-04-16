package com.example.stockhelper.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockhelper.domain.model.TradeRecord
import com.example.stockhelper.ui.theme.Green
import com.example.stockhelper.ui.theme.Red
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史记录") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 筛选按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = { viewModel.setFilter(null) },
                    label = { Text("全部") },
                    selected = uiState.filterType == null
                )
                FilterChip(
                    onClick = { viewModel.setFilter("BUY_ALERT") },
                    label = { Text("买入提醒") },
                    selected = uiState.filterType == "BUY_ALERT"
                )
                FilterChip(
                    onClick = { viewModel.setFilter("SELL_ALERT") },
                    label = { Text("卖出提醒") },
                    selected = uiState.filterType == "SELL_ALERT"
                )
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.records.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无历史记录", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.records) { record ->
                        TradeRecordCard(
                            record = record,
                            onMarkHandled = { viewModel.markAsHandled(record.id) },
                            onDelete = { viewModel.deleteRecord(record.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TradeRecordCard(
    record: TradeRecord,
    onMarkHandled: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val dateStr = dateFormat.format(Date(record.triggeredAt))
    val isBuy = record.recordType == "BUY_ALERT"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = record.stockName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = record.stockCode,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                AssistChip(
                    onClick = { },
                    label = {
                        Text(if (isBuy) "买入提醒" else "卖出提醒")
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isBuy) Green.copy(alpha = 0.2f) else Red.copy(alpha = 0.2f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("触发价格", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(
                        text = String.format("%.2f 元", record.currentPrice),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column {
                    Text("目标价格", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(
                        text = String.format("%.2f 元", record.targetPrice),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column {
                    Text("做T数量", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(
                        text = "${record.tShares} 股",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            if (record.isHandled) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✓ 已处理",
                    style = MaterialTheme.typography.bodySmall,
                    color = Green
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!record.isHandled) {
                    TextButton(onClick = onMarkHandled) {
                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("标记已处理")
                    }
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
