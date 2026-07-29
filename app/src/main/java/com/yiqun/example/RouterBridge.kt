package com.yiqun.example

/**
 *
 * @Description :
 * @Author : Dalingge
 * @Time :2026/7/28  18:51
 */
object RouterBridge {

    /**
     * 统一跳转入口
     * @param url 可以是旧的 ARouter 路径 (/chat/old_activity)，也可以是 Compose 路由 (app://chat/home?userId=1)
     */
    fun navigate(url: String) {
        if (url.startsWith("app://")) {
            // A. 如果是 Compose 路由，通过 ARouter 唤起 Compose 宿主容器 Activity
//            ARouter.getInstance()
//                .build("/app/compose_container")
//                .withString("target_url", url)
//                .navigation()
        } else {
            // B. 如果是传统的 ARouter Activity 路径，直接按原逻辑跳转
//            ARouter.getInstance()
//                .build(url)
//                .navigation()
        }
    }
}