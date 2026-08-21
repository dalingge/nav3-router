package com.dalingge.pay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dalingge.nav.annotation.Required
import com.dalingge.nav.annotation.Screen
import com.dalingge.nav.runtime.NavCenter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 *
 * @Description :
 * @Author : Dalingge
 * @Time :2026/8/21  13:42
 */
/**
 * 支付弹窗组件（将在当前页面最上层弹出）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayDialog(
    orderId: String,
    amount: Double,
    onPaySuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = {
            if (!isLoading) onDismiss()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("收银台", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("订单号: $orderId", color = Color.Gray)
            Text("¥$amount", style = MaterialTheme.typography.displayMedium, color = Color(0xFFFF4D4F))

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("正在安全支付中...", color = Color.Gray)
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            delay(2500L.milliseconds) // 模拟 2.5 秒接口请求延时
                            isLoading = false
                            onPaySuccess()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("确认支付 ¥$amount")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}