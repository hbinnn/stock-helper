package com.example.stockhelper.ui.screens.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockhelper.ui.theme.Green
import com.example.stockhelper.ui.theme.Red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStockScreen(
    viewModel: AddStockViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加股票") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
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
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 市场标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "市场: ${if (uiState.market == "sh") "上海" else "深圳"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                if (uiState.code.length >= 6) {
                    Text(
                        text = "代码以 ${uiState.code.first()} 开头",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // 股票代码输入
            OutlinedTextField(
                value = uiState.code,
                onValueChange = { viewModel.updateCode(it) },
                label = { Text("股票代码") },
                placeholder = { Text("如: 600000 或 000001") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 获取股票信息按钮
            Button(
                onClick = { viewModel.fetchStockInfo() },
                enabled = uiState.code.length >= 6 && !uiState.isLoadingInfo,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoadingInfo) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("获取中...")
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("获取股票信息")
                }
            }

            // 股票信息显示
            if (uiState.fetchedName != null || uiState.currentPrice != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            text = uiState.fetchedName ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "现价",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Text(
                                    text = String.format("%.2f", uiState.currentPrice ?: 0.0),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if ((uiState.changePercent ?: 0.0) >= 0) Red else Green
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "涨跌幅",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                val percent = uiState.changePercent ?: 0.0
                                Text(
                                    text = "${if (percent >= 0) "+" else ""}${String.format("%.2f", percent)}%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (percent >= 0) Red else Green
                                )
                            }
                        }
                    }
                }
            }

            // 成本价输入
            OutlinedTextField(
                value = uiState.costPriceInput,
                onValueChange = { viewModel.updateCostPrice(it) },
                label = { Text("成本价") },
                placeholder = { Text("如: 10.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                suffix = { Text("元") },
                supportingText = { Text("获取股票信息后默认为现价，可自行修改") }
            )

            // 持仓数量
            OutlinedTextField(
                value = uiState.shares,
                onValueChange = { viewModel.updateShares(it) },
                label = { Text("持仓数量") },
                placeholder = { Text("如: 1000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                suffix = { Text("股") }
            )

            // 交易模式选择
            Text(
                text = "交易模式",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = { viewModel.updateTradeType("BUY_FIRST") },
                    label = { Text("先买后卖") },
                    selected = uiState.tradeType == "BUY_FIRST"
                )
                FilterChip(
                    onClick = { viewModel.updateTradeType("SELL_FIRST") },
                    label = { Text("先卖后买") },
                    selected = uiState.tradeType == "SELL_FIRST"
                )
            }
            Text(
                text = if (uiState.tradeType == "BUY_FIRST")
                    "必须先达到买入价，才会提醒卖出"
                else
                    "必须先达到卖出价，才会提醒买入",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            // 做T数量设置
            Text(
                text = "做T数量",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = { viewModel.updateTTradeType("PERCENT") },
                    label = { Text("比例") },
                    selected = uiState.tTradeType == "PERCENT"
                )
                FilterChip(
                    onClick = { viewModel.updateTTradeType("SHARES") },
                    label = { Text("固定股数") },
                    selected = uiState.tTradeType == "SHARES"
                )
            }

            if (uiState.tTradeType == "PERCENT") {
                // 百分比选项
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = { viewModel.updateTSharesPercent("25") },
                        label = { Text("1/4仓") },
                        selected = uiState.tSharesPercent == "25"
                    )
                    FilterChip(
                        onClick = { viewModel.updateTSharesPercent("33") },
                        label = { Text("1/3仓") },
                        selected = uiState.tSharesPercent == "33"
                    )
                    FilterChip(
                        onClick = { viewModel.updateTSharesPercent("50") },
                        label = { Text("1/2仓") },
                        selected = uiState.tSharesPercent == "50"
                    )
                    FilterChip(
                        onClick = { viewModel.updateTSharesPercent("100") },
                        label = { Text("全仓") },
                        selected = uiState.tSharesPercent == "100"
                    )
                }
                Text(
                    text = when (uiState.tSharesPercent) {
                        "25" -> "每次做T: 1/4仓"
                        "33" -> "每次做T: 1/3仓"
                        "50" -> "每次做T: 1/2仓"
                        "100" -> "每次做T: 全仓"
                        else -> "每次做T: ${uiState.tSharesPercent}%"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            } else {
                OutlinedTextField(
                    value = uiState.tShares,
                    onValueChange = { viewModel.updateTShares(it) },
                    label = { Text("做T股数") },
                    placeholder = { Text("如: 100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text("股") },
                    supportingText = {
                        if (uiState.tradeType == "SELL_FIRST" && uiState.shares.isNotBlank()) {
                            val shares = uiState.shares.toIntOrNull() ?: 0
                            Text("不能超过持仓 ${shares}股")
                        } else null
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 目标买入价设置
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = { viewModel.updateTargetBuyPriceType("PRICE") },
                    label = { Text("固定价格") },
                    selected = uiState.buyPriceType == "PRICE"
                )
                FilterChip(
                    onClick = { viewModel.updateTargetBuyPriceType("PERCENT") },
                    label = { Text("百分比") },
                    selected = uiState.buyPriceType == "PERCENT"
                )
            }

            if (uiState.buyPriceType == "PRICE") {
                OutlinedTextField(
                    value = uiState.targetBuyPrice,
                    onValueChange = { viewModel.updateTargetBuyPrice(it) },
                    label = { Text("目标买入价") },
                    placeholder = { Text("如: 9.50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text("元") }
                )
            } else {
                OutlinedTextField(
                    value = uiState.buyPercent,
                    onValueChange = { viewModel.updateBuyPercent(it) },
                    label = { Text("买入下跌") },
                    placeholder = { Text("如: 5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text("%") },
                    supportingText = { Text("相对于成本价下跌百分之多少买入") }
                )
            }

            // 目标卖出价设置
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = { viewModel.updateTargetSellPriceType("PRICE") },
                    label = { Text("固定价格") },
                    selected = uiState.sellPriceType == "PRICE"
                )
                FilterChip(
                    onClick = { viewModel.updateTargetSellPriceType("PERCENT") },
                    label = { Text("百分比") },
                    selected = uiState.sellPriceType == "PERCENT"
                )
            }

            if (uiState.sellPriceType == "PRICE") {
                OutlinedTextField(
                    value = uiState.targetSellPrice,
                    onValueChange = { viewModel.updateTargetSellPrice(it) },
                    label = { Text("目标卖出价") },
                    placeholder = { Text("如: 10.80") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text("元") }
                )
            } else {
                OutlinedTextField(
                    value = uiState.sellPercent,
                    onValueChange = { viewModel.updateSellPercent(it) },
                    label = { Text("卖出上涨") },
                    placeholder = { Text("如: 8") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text("%") },
                    supportingText = { Text("相对于成本价上涨百分之多少卖出") }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = { viewModel.saveStock() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.code.isNotBlank() &&
                        uiState.name.isNotBlank() &&
                        uiState.costPriceInput.isNotBlank() &&
                        uiState.shares.isNotBlank()
            ) {
                Text("保存")
            }
        }
    }
}
