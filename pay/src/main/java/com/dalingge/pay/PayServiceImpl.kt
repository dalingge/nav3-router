package com.dalingge.pay

import com.dalingge.common.PayService
import com.dalingge.nav.annotation.Service
import com.dalingge.nav.runtime.NavCenter
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 *
 * @Description :
 * @Author : Dalingge
 * @Time :2026/8/21  11:07
 */
@Service(contract = PayService::class, path = "pay/service")
class PayServiceImpl : PayService {

    override suspend fun pay(orderId: String, amount: Double): Boolean {
        println("💳 [PayService] 开始调用后端扣款接口，订单号: $orderId ...")

        // 模拟网络请求延时 2 秒
        delay(2000L.milliseconds)

        println("✅ [PayService] 扣款成功！")
        return true
    }

    override fun showPayDialog(orderId: String, amount: Double) {
        // 在当前页面最上层弹出收银台！
        NavCenter.showOverlay {
            PayDialog(
                orderId = orderId,
                amount = amount,
                onPaySuccess = {
                    NavCenter.dismissOverlay() // 关闭弹窗
                    NavCenter.popWithResult("pay_result", true) // 回传成功状态
                },
                onDismiss = {
                    NavCenter.dismissOverlay() // 关闭弹窗
                    NavCenter.popWithResult("pay_result", false)
                }
            )
        }
    }


}