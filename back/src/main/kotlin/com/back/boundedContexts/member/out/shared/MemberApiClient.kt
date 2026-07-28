package com.back.boundedContexts.member.out.shared

import com.back.global.app.app.AppFacade
import com.back.standard.lib.InternalRestClient
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service

@Service
class MemberApiClient(
    private val internalRestClient: InternalRestClient
) {
    // 빈 생성 시점의 AppFacade 초기화 순서 문제를 피하려고 지연 평가
    private val authHeaders
        get() = mapOf(
            HttpHeaders.AUTHORIZATION to "Bearer ${AppFacade.systemMemberApiKey}"
        )

    val randomSecureTip: String
        get() {
            val response = internalRestClient.get(
                "/member/api/v1/members/randomSecureTip",
                authHeaders
            )

            return response.body
        }
}
