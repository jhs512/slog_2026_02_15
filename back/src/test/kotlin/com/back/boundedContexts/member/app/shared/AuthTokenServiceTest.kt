package com.back.boundedContexts.member.app.shared

import com.back.boundedContexts.member.dto.shared.AccessTokenPayload
import com.back.standard.extensions.getOrThrow
import com.back.standard.util.Ut
import org.assertj.core.api.Assertions.assertThat
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
    fun `문자열화된 토큰 발급 결과의 페이로드를 검증할 수 있다`() {
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
    fun `발급한 토큰을 검증할 수 있다`() {
        val memberUser1 = actorFacade.findByUsername("user1").getOrThrow()

        val accessToken = authTokenService.genAccessToken(memberUser1)

        assertThat(accessToken).isNotBlank

        val parsedPayload = authTokenService.payload(accessToken)

        val expectedPayload = AccessTokenPayload(memberUser1.id, memberUser1.username, memberUser1.name)

        assertThat(parsedPayload)
            .isEqualTo(expectedPayload)
    }
}
