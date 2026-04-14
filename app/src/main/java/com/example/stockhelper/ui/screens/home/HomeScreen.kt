package com.example.stockhelper.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockhelper.domain.model.StockQuote
import com.example.stockhelper.ui.theme.Green
import com.example.stockhelper.ui.theme.Red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onAddStock: () -> Unit,
    onStockClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val stocksWithQuotes = viewModel.getStocksWithQuotes()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("做T助手") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddStock,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加股票")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (stocksWithQuotes.isEmpty()) {
                EmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(stocksWithQuotes, key = { it.stock.code }) { item ->
                        StockCard(
                            stockWithQuote = item,
                            onClick = { onStockClick(item.stock.code) },
                            onDelete = { viewModel.deleteStock(item.stock.code) }
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "暂无自选股",
            fontSize = 18.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击 + 添加股票",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun StockCard(
    stockWithQuote: StockWithQuote,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val stock = stockWithQuote.stock
    val quote = stockWithQuote.quote

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                        text = stock.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stock.code,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (stockWithQuote.shouldBuyAlert || stockWithQuote.shouldSellAlert) {
                        AlertBadge(
                            shouldBuyAlert = stockWithQuote.shouldBuyAlert,
                            shouldSellAlert = stockWithQuote.shouldSellAlert
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (quote != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = String.format("%.2f", quote.currentPrice),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (quote.changePercent >= 0) Red else Green
                        )
                        Text(
                            text = "${if (quote.changePercent >= 0) "+" else ""}${String.format("%.2f", quote.changePercent)}%",
                            fontSize = 14.sp,
                            color = if (quote.changePercent >= 0) Red else Green
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row {
                            Text(
                                text = "买价: ${String.format("%.2f", stock.targetBuyPrice)}",
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (stockWithQuote.shouldBuyAlert) {
                                Text(
                                    text = "✓",
                                    fontSize = 12.sp,
                                    color = Green,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Row {
                            Text(
                                text = "卖价: ${String.format("%.2f", stock.targetSellPrice)}",
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (stockWithQuote.shouldSellAlert) {
                                Text(
                                    text = "✓",
                                    fontSize = 12.sp,
                                    color = Red,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "持仓: ${stock.shares * 100}股",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    stockWithQuote.expectedProfit?.let { profit ->
                        Text(
                            text = "预估收益: ${String.format("%.2f", profit)}元",
                            fontSize = 12.sp,
                            color = if (profit >= 0) Red else Green,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Text(
                    text = "加载中...",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun AlertBadge(shouldBuyAlert: Boolean, shouldSellAlert: Boolean) {
    Row {
        if (shouldBuyAlert) {
            Text(
                text = "买",
                fontSize = 10.sp,
                color = Color.White,
                modifier = Modifier
                    .background(Green, shape = MaterialTheme.shapes.small)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        if (shouldSellAlert) {
            Text(
                text = "卖",
                fontSize = 10.sp,
                color = Color.White,
                modifier = Modifier
                    .background(Red, shape = MaterialTheme.shapes.small)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
