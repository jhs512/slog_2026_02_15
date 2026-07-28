package com.back.boundedContexts.member.domain.shared

import java.time.Instant

/**
 * 액세스 토큰(JWT) 페이로드(id, username, nickname)만으로 만들어지는 Member 프록시.
 * DB 조회 없이 요청을 처리할 수 있게 하되, real(진짜 엔티티) 접근이 필요해지는 순간
 * 지연 로딩된 real에 위임한다.
 */
class MemberProxy(
    private val real: Member,
    id: Int,
    username: String,
    nickname: String
) : Member(id, username, nickname) {
    // useRealState가 왜 필요한가:
    // 토큰에 담긴 nickname은 "토큰 발급 시점"의 캐시값이라 DB의 최신값과 다를 수 있다.
    // real 엔티티를 한 번이라도 로드(markUseReal)한 뒤에도 nickname/name을 계속 토큰값에서 읽으면,
    // 같은 객체에서 "DB에서 읽은 값(createdAt, profileImgUrl 등)"과 "토큰의 옛 nickname"이
    // 섞여 보이는 불일치가 생긴다. 그래서 real 로드 이후에는 nickname/name도 real에서 읽는다.
    private var useRealState = false

    private fun markUseReal() {
        useRealState = true
    }

    override var nickname: String
        get() = if (useRealState) real.nickname else super.nickname
        set(value) {
            super.nickname = value
            real.nickname = value
        }

    override val name: String
        get() = if (useRealState) real.name else super.name

    override var createdAt: Instant
        get() {
            markUseReal()
            return real.createdAt
        }
        set(value) {
            markUseReal()
            real.createdAt = value
        }

    override var modifiedAt: Instant
        get() {
            markUseReal()
            return real.modifiedAt
        }
        set(value) {
            markUseReal()
            real.modifiedAt = value
        }

    override var profileImgUrl: String
        get() {
            markUseReal()
            return real.profileImgUrl
        }
        set(value) {
            markUseReal()
            real.profileImgUrl = value
        }

    override val profileImgUrlOrDefault: String
        get() {
            markUseReal()
            return real.profileImgUrlOrDefault
        }

    override var apiKey: String
        get() {
            markUseReal()
            return real.apiKey
        }
        set(value) {
            markUseReal()
            real.apiKey = value
        }

    override var password: String?
        get() {
            markUseReal()
            return real.password
        }
        set(value) {
            markUseReal()
            real.password = value
        }

    override var postsCount: Int
        get() {
            markUseReal()
            return real.postsCount
        }
        set(value) {
            markUseReal()
            real.postsCount = value
        }

    override var postCommentsCount: Int
        get() {
            markUseReal()
            return real.postCommentsCount
        }
        set(value) {
            markUseReal()
            real.postCommentsCount = value
        }
}
