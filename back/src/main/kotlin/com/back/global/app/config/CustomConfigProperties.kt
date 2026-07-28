package com.back.global.app.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("custom")
class CustomConfigProperties(
    val notProdMembers: List<NotProdMember> = emptyList(),
) {
    data class NotProdMember(
        val username: String,
        val apiKey: String,
        val nickname: String,
        val profileImgUrl: String
    )
}
