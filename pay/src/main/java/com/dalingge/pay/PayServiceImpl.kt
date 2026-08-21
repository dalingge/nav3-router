package com.dalingge.pay

import com.dalingge.common.PayService
import com.dalingge.nav.annotation.Service

/**
 *
 * @Description :
 * @Author : Dalingge
 * @Time :2026/8/21  11:07
 */
@Service(contract = PayService::class, path = "pay/service")
class PayServiceImpl : PayService {

    override fun pay(orderId: String, amount: Double): Boolean {
        println("💳 正在处理订单 $orderId 支付 $amount 元")
        return true
    }
}