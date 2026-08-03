package com.yiqun.nav.annotation

/**
 *
 * @Description :记字段为必传参数（若不传或为空则触发容错降级）
 * @Author : Dalingge
 * @Time :2026/8/3  10:22
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class Required