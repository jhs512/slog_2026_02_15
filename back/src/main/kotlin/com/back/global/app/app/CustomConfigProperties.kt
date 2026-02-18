package com.back.global.app.app

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("custom")
class CustomConfigProperties {
    var notProdMembers: MutableList<NotProdMember> = mutableListOf()

    data class NotProdMember(
        val username: String,
        val apiKey: String,
        val nickname: String,
        val profileImgUrl: String
    )
}
