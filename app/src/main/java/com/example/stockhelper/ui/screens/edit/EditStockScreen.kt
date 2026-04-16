package com.example.stockhelper.ui.screens.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockhelper.ui.screens.add.AddStockScreen
import com.example.stockhelper.ui.screens.add.AddStockViewModel

@Composable
fun EditStockScreen(
    stockCode: String,
    viewModel: AddStockViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(stockCode) {
        viewModel.loadStock(stockCode)
    }

    AddStockScreen(
        viewModel = viewModel,
        onNavigateBack = onNavigateBack,
        isEditMode = true
    )
}
