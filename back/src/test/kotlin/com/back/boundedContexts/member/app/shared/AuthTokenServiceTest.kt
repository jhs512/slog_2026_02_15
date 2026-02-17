package com.back.boundedContexts.member.app.shared

import com.back.boundedContexts.member.app.shared.ActorFacade
import com.back.boundedContexts.member.app.shared.AuthTokenService
import com.back.boundedContexts.member.dto.shared.AccessTokenPayload
import com.back.standard.extensions.getOrThrow
import com.back.standard.util.Ut
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthTokenServiceTest {
    @Autowired
    private lateinit var actorFacade: ActorFacade

    @Autowired
    private lateinit var authTokenService: AuthTokenService

    @Value("\${custom.jwt.secretKey}")
    private lateinit var jwtSecretKey: String

    @Value("\${custom.accessToken.expirationSeconds}")
    private var accessTokenExpirationSeconds: Int = 0

    @Test
    @DisplayName("토큰 유틸을 통해 토큰을 생성하고 페이로드를 검증한다.")
    fun `Ut JWT toString 으로 토큰을 발급하면 페이로드를 검증할 수 있다`() {
        val payload = mapOf("name" to "Paul", "age" to 23)

        val jwt = Ut.JWT.toString(
            jwtSecretKey,
            accessTokenExpirationSeconds,
            payload
        )

        assertThat(jwt).isNotBlank

        assertThat(Ut.JWT.isValid(jwtSecretKey, jwt)).isTrue

        val parsedPayload = Ut.JWT.payload(jwtSecretKey, jwt).getOrThrow()

        assertThat(payload.all { (key, value) ->
            parsedPayload[key] == value
        }).isTrue
    }

    @Test
    @DisplayName("멤버 기반 토큰 발급 결과를 통해 페이로드를 검증한다.")
    fun `genAccessToken 으로 생성한 JWT를 검증할 수 있다`() {
        val memberUser1 = actorFacade.findByUsername("user1").getOrThrow()

        val accessToken = authTokenService.genAccessToken(memberUser1)

        assertThat(accessToken).isNotBlank

        val parsedPayload = authTokenService.payload(accessToken)

        val expectedPayload = AccessTokenPayload(
            id = memberUser1.id,
            username = memberUser1.username,
            name = memberUser1.name
        )

        assertThat(parsedPayload)
            .isEqualTo(expectedPayload)
    }
}
