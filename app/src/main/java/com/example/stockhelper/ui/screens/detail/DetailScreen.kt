package com.example.stockhelper.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockhelper.ui.theme.Green
import com.example.stockhelper.ui.theme.Red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    stockCode: String,
    viewModel: DetailViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(stockCode) {
        viewModel.loadStock(stockCode)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.stockName.ifEmpty { "股票详情" }) },
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEdit(stockCode) },
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Default.Edit, contentDescription = "编辑")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 当前行情
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "当前行情",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.quote != null) {
                        val quote = uiState.quote!!

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = String.format("%.2f", quote.currentPrice),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (quote.changePercent >= 0) Red else Green
                                )
                                Text(
                                    text = "${if (quote.changePercent >= 0) "+" else ""}${String.format("%.2f", quote.changePercent)}%",
                                    fontSize = 16.sp,
                                    color = if (quote.changePercent >= 0) Red else Green
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "今开: ${String.format("%.2f", quote.open)}", fontSize = 14.sp)
                                Text(text = "昨收: ${String.format("%.2f", quote.previousClose)}", fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "最高", fontSize = 12.sp, color = Color.Gray)
                                Text(
                                    text = String.format("%.2f", quote.high),
                                    fontSize = 16.sp,
                                    color = Red
                                )
                            }
                            Column {
                                Text(text = "最低", fontSize = 12.sp, color = Color.Gray)
                                Text(
                                    text = String.format("%.2f", quote.low),
                                    fontSize = 16.sp,
                                    color = Green
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (uiState.isLoading) "加载中..." else "无法获取行情",
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 交易设置
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "交易设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    DetailRow("股票代码", uiState.stockCode)
                    DetailRow("交易模式", if (uiState.tradeType == "BUY_FIRST") "先买后卖" else "先卖后买")
                    DetailRow("持仓数量", "${uiState.shares}股")
                    DetailRow(
                        "做T数量",
                        if (uiState.tTradeType == "PERCENT") {
                            when (uiState.tSharesPercent) {
                                25 -> "1/4仓"
                                33 -> "1/3仓"
                                50 -> "1/2仓"
                                100 -> "全仓"
                                else -> "${uiState.tSharesPercent}%"
                            }
                        } else {
                            "${uiState.tShares}股"
                        }
                    )
                    DetailRow(
                        "目标买入价",
                        "${String.format("%.2f", uiState.targetBuyPrice)}元",
                        uiState.quote?.let {
                            if (it.currentPrice <= uiState.targetBuyPrice) "✓ 已达" else null
                        }
                    )
                    DetailRow(
                        "目标卖出价",
                        "${String.format("%.2f", uiState.targetSellPrice)}元",
                        uiState.quote?.let {
                            if (it.currentPrice >= uiState.targetSellPrice) "✓ 已达" else null
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 收益计算
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "预估收益",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "预估收益金额", fontSize = 14.sp)
                            Text(
                                text = "${String.format("%.2f", uiState.expectedProfit)}元",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.expectedProfit >= 0) Red else Green
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "收益率", fontSize = 14.sp)
                            Text(
                                text = "${String.format("%.2f", uiState.profitRate)}%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.profitRate >= 0) Red else Green
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "手续费按0.1%计算",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    note: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, fontSize = 14.sp)
            if (note != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = note,
                    fontSize = 12.sp,
                    color = Green,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
