package com.back.global.security.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository

@Configuration
@Profile("test")
class TestOAuth2Config {

    @Bean
    @ConditionalOnMissingBean(ClientRegistrationRepository::class)
    fun clientRegistrationRepository(): ClientRegistrationRepository = object : ClientRegistrationRepository {
        override fun findByRegistrationId(registrationId: String): ClientRegistration? = null
    }
}
