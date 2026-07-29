package com.yiqun.example

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.yiqun.nav.runtime.NavCenter
import com.yiqun.nav.runtime.Navigator
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(val id: Int, val name: String)

// 依赖 Navigator 接口，纯 Kotlin 环境下 100% 可单元测试！
class HomeViewModel : ViewModel() {

    fun openDetail(userId: Int) {
        // 使用 PopUpTo 高级栈控制
        NavCenter.navigate(
            DetailScreenDestination(user = UserProfile(userId, "Aleyn"))
        )
    }
}

class DetailViewModel : ViewModel() {
    var count by mutableIntStateOf(0)
    fun inc() { count++ }
    override fun onCleared() { println("DetailViewModel 已销毁！") }
}