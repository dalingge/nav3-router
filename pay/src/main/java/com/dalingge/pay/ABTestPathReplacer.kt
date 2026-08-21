package com.dalingge.pay

import com.dalingge.nav.runtime.PathReplaceService

/**
 *
 * @Description :A/B 测试动态替换服务
 * @Author : Dalingge
 * @Time :2026/8/21  11:34
 */
class ABTestPathReplacer : PathReplaceService {
    override fun replace(rawUrl: String): String {
        // 如果命中 A/B 测试人群，将旧详情页重写为 2.0 新详情页
        if (rawUrl == "pay/detail" && ABTestEngine.isGroupA()) {
            return "pay/detail_v2"
        }
        return rawUrl
    }
}