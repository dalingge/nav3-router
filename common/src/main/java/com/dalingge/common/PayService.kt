package com.dalingge.common

import com.dalingge.nav.runtime.IService

/**
 *
 * @Description :
 * @Author : Dalingge
 * @Time :2026/8/21  11:04
 */
interface PayService : IService {

    suspend fun pay(orderId: String, amount: Double): Boolean


    // 🆕 新增拉起支付收银台弹窗方法
    fun showPayDialog(orderId: String, amount: Double)
}