package com.dalingge.common

import com.dalingge.nav.runtime.IService

/**
 *
 * @Description :
 * @Author : Dalingge
 * @Time :2026/8/21  11:04
 */
interface PayService : IService {
    fun pay(orderId: String, amount: Double): Boolean
}