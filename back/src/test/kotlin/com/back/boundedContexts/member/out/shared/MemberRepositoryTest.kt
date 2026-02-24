package com.back.boundedContexts.member.out.shared

import com.back.boundedContexts.member.app.MemberFacade
import com.back.boundedContexts.member.domain.shared.Member
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

/**
 * PGroonga 4.x 복합 인덱스 검색 테스트 — Member (username + nickname)
 *
 * 인덱스 : idx_member_username_nickname_pgroonga  (username VARCHAR, nickname VARCHAR, TokenBigram)
 * 연산자 : ARRAY[username, nickname]
 *              &@~ (query, NULL, 'idx_member_username_nickname_pgroonga')
 *              ::pgroonga_full_text_search_condition
 *
 * 특징
 *  - username (VARCHAR) + nickname (VARCHAR) 복합 단일 인덱스 스캔
 *  - NOT(-) 가 username·nickname 전체에 전역 적용됨 (크로스컬럼 제외)
 *  - PGroonga query syntax 를 그대로 지원
 *
 * 커버하는 문법
 *  1. 기본 키워드 — username / nickname 교차 검색
 *  2. AND  — 공백(묵시적) / + (명시적)
 *  3. OR
 *  4. NOT  — 단일·복수·크로스컬럼
 *  5. 구문 검색  "..."
 *  6. 전위 검색  word*
 *  7. 복합 조건  — AND+NOT / OR+NOT / 그룹화
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberRepositoryTest {

    @Autowired private lateinit var memberRepository: MemberRepository
    @Autowired private lateinit var memberFacade: MemberFacade

    /** 회원 생성 헬퍼 */
    private fun join(username: String, nickname: String): Member =
        memberFacade.join(username, "testpass", nickname)

    /** username + nickname 통합 키워드 검색 */
    private fun search(kw: String): Page<Member> =
        memberRepository.findQPagedByKw(kw, PageRequest.of(0, 100))

    // ────────────────────────────────────────────────────────────────
    // 1. 기본 키워드 검색
    // ────────────────────────────────────────────────────────────────

    @Nested
    inner class `1 기본 키워드 검색` {

        @Test
        fun `단일 키워드 — username 에 있으면 매칭된다`() {
            join("android-developer", "모바일 개발자")
            join("ios-developer", "아이폰 개발자")

            val result = search("android")

            assertThat(result.content)
                .isNotEmpty
                .allMatch { it.username.contains("android") || it.nickname.contains("android") }
        }

        @Test
        fun `단일 키워드 — nickname 에 있으면 매칭된다`() {
            join("user-alpha", "안드로이드 개발자")
            join("user-beta", "iOS 개발자")

            val result = search("안드로이드")

            assertThat(result.content).anyMatch { it.nickname.contains("안드로이드") }
            assertThat(result.content).noneMatch { it.username == "user-beta" }
        }

        @Test
        fun `단일 키워드 — username 과 nickname 중 어느 하나에만 있어도 매칭된다`() {
            join("spring-dev", "백엔드 개발자")           // username 에 spring
            join("frontend-ace", "spring 생태계 전문가")  // nickname 에 spring

            val result = search("spring")

            assertThat(result.content).hasSize(2)
        }

        @Test
        fun `단일 키워드 — username·nickname 어디에도 없으면 매칭되지 않는다`() {
            join("android-a", "안드로이드 가이드")

            val result = search("두리안")

            assertThat(result.content)
                .noneMatch { it.username.contains("두리안") || it.nickname.contains("두리안") }
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 2. AND 검색 — 공백(묵시적) / +(명시적)
    // ────────────────────────────────────────────────────────────────

    @Nested
    inner class `2 AND 검색` {

        @Test
        fun `AND(공백) — 두 단어 모두 포함한 회원만 매칭된다`() {
            join("android-a", "안드로이드 가이드")   // ✓ android + 가이드
            join("android-b", "iOS 개발자")          // ✗ 가이드 없음
            join("guide-c", "가이드 작성자")         // ✗ android 없음

            val result = search("android 가이드")

            assertThat(result.content).allMatch { m ->
                (m.username + m.nickname).contains("android") &&
                    (m.username + m.nickname).contains("가이드")
            }
        }

        @Test
        fun `AND(공백) — username 에 한 단어, nickname 에 다른 단어가 있어도 AND 로 매칭된다`() {
            // username "backend-ace" 에 backend, nickname "스프링 전문가" 에 스프링
            join("backend-ace", "스프링 전문가")

            val result = search("backend 스프링")

            assertThat(result.content).anyMatch {
                it.username.contains("backend") && it.nickname.contains("스프링")
            }
        }

        @Test
        fun `AND(공백) — 한 단어만 있으면 AND 검색에서 제외된다`() {
            join("android-a", "안드로이드 가이드")  // ✓
            join("android-b", "iOS 개발자")         // ✗ 가이드 없음
            join("guide-only", "가이드 작성자")     // ✗ android 없음

            val result = search("안드로이드 가이드")

            assertThat(result.content).allMatch { m ->
                (m.username + m.nickname).contains("안드로이드") &&
                    (m.username + m.nickname).contains("가이드")
            }
        }

        @Test
        fun `AND(+) — 플러스 AND 도 공백 AND 와 동일하게 동작한다`() {
            join("dev-guide", "개발 가이드")    // ✓
            join("dev-writer", "개발 블로거")   // ✗ 가이드 없음

            val result = search("+개발 +가이드")

            assertThat(result.content).isNotEmpty
            assertThat(result.content).allMatch { m ->
                (m.username + m.nickname).contains("개발") &&
                    (m.username + m.nickname).contains("가이드")
            }
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 3. OR 검색
    // ────────────────────────────────────────────────────────────────

    @Nested
    inner class `3 OR 검색` {

        @Test
        fun `OR — 두 키워드 중 하나라도 있으면 매칭된다`() {
            join("android-guide", "안드로이드 가이드")  // ✓ 안드로이드
            join("ios-recipe", "iOS 레시피")            // ✓ 레시피
            join("web-dev", "웹 개발자")                // ✗ 둘 다 없음

            val result = search("안드로이드 OR 레시피")

            assertThat(result.content).anyMatch { (it.username + it.nickname).contains("안드로이드") }
            assertThat(result.content).anyMatch { (it.username + it.nickname).contains("레시피") }
            assertThat(result.content).noneMatch { it.username == "web-dev" }
        }

        @Test
        fun `OR — 두 키워드 모두 가진 회원도 OR 검색에서 매칭된다`() {
            join("fullstack-dev", "안드로이드와 iOS 전문가")

            val result = search("안드로이드 OR iOS")

            assertThat(result.content).anyMatch {
                it.nickname.contains("안드로이드") && it.nickname.contains("iOS")
            }
        }

        @Test
        fun `OR — username OR nickname 어디에 있어도 매칭된다`() {
            join("backend-ace", "스프링 전문가")    // username 에 backend
            join("frontend-pro", "리액트 개발자")   // nickname 에 리액트

            val result = search("backend OR 리액트")

            assertThat(result.content).hasSize(2)
        }

        @Test
        fun `OR — 세 키워드 중 하나라도 있으면 매칭된다`() {
            join("android-a", "안드로이드 가이드")
            join("guide-search", "안드로이드 레시피")
            join("dev-guide", "개발 가이드")
            join("plain-user", "일반 사용자")  // 셋 다 없음

            val result = search("android OR 레시피 OR 개발")

            assertThat(result.content).noneMatch { it.username == "plain-user" }
            assertThat(result.content.size).isGreaterThanOrEqualTo(3)
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 4. NOT 검색 (마이너스) — 전역 제외
    // ────────────────────────────────────────────────────────────────

    @Nested
    inner class `4 NOT 검색` {

        @Test
        fun `NOT(-) 단일 — 마이너스 단어를 포함하는 회원은 제외된다`() {
            join("android-a", "안드로이드 가이드")   // ✓ guide 없음
            join("android-guide", "일반 사용자")     // ✗ username 에 guide

            val result = search("android -guide")

            assertThat(result.content)
                .noneMatch { it.username.contains("guide") || it.nickname.contains("guide") }
        }

        @Test
        fun `NOT(-) 크로스컬럼 — username 에 제외 단어가 있으면 nickname 이 매칭돼도 전역 제외된다`() {
            // ARRAY[] + pgroonga_full_text_search_condition: -는 username·nickname 통합 적용
            join("excluded-writer", "검색 대상 닉네임")  // ✗ username 에 excluded
            join("normal-writer", "검색 대상 닉네임")    // ✓ excluded 없음

            val result = search("닉네임 -excluded")

            assertThat(result.content).noneMatch { it.username.contains("excluded") }
            assertThat(result.content).anyMatch { it.username == "normal-writer" }
        }

        @Test
        fun `NOT(-) 복수 — 여러 단어를 동시에 제외할 수 있다`() {
            join("guide-search", "안드로이드 레시피")   // ✗ search 포함
            join("dev-guide", "개발 가이드")            // ✓
            join("android-guide", "일반 사용자")        // ✗ android 포함

            val result = search("guide -android -search")

            assertThat(result.content).noneMatch { (it.username + it.nickname).contains("android") }
            assertThat(result.content).noneMatch { (it.username + it.nickname).contains("search") }
            assertThat(result.content).anyMatch { it.username == "dev-guide" }
        }

        @Test
        fun `AND+NOT — 필수 포함(+)과 제외(-) 조합이 동시에 적용된다`() {
            join("android-a", "안드로이드 가이드")   // ✓ android + 가이드, 레시피 없음
            join("android-b", "안드로이드 레시피")   // ✗ 레시피 포함
            join("guide-c", "개발 가이드")           // ✗ android 없음

            val result = search("+android -레시피")

            assertThat(result.content).allMatch { (it.username + it.nickname).contains("android") }
            assertThat(result.content).noneMatch { it.nickname.contains("레시피") }
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 5. 구문 검색 "..."
    // ────────────────────────────────────────────────────────────────

    @Nested
    inner class `5 구문 검색` {

        @Test
        fun `구문검색 — nickname 에서 정확히 인접한 구문만 매칭된다`() {
            join("user-a", "안드로이드 가이드")      // ✓ "안드로이드 가이드" 인접
            join("user-b", "안드로이드 앱 가이드")   // ✗ 사이에 "앱" 끼어 있음

            val result = search("\"안드로이드 가이드\"")

            assertThat(result.content).anyMatch { it.nickname == "안드로이드 가이드" }
            assertThat(result.content).noneMatch { it.nickname == "안드로이드 앱 가이드" }
        }

        @Test
        fun `구문검색 — username 에서도 정확한 구문이 매칭된다`() {
            join("spring-boot-dev", "백엔드 개발자")    // ✓ spring-boot 인접 (하이픈 포함 구문)
            join("spring-and-boot", "다른 개발자")      // ✗ spring-boot 아님

            val result = search("\"spring-boot\"")

            assertThat(result.content).anyMatch { it.username.contains("spring-boot") }
            assertThat(result.content).noneMatch { it.username == "spring-and-boot" }
        }

        @Test
        fun `구문검색 — 단어 사이에 다른 단어가 끼면 구문 검색에서 제외된다`() {
            join("user-c", "개발 가이드")        // ✓ "개발 가이드" 인접
            join("user-d", "개발을 위한 가이드") // ✗ 비인접

            val result = search("\"개발 가이드\"")

            assertThat(result.content).anyMatch { it.nickname == "개발 가이드" }
            assertThat(result.content).noneMatch { it.nickname.startsWith("개발을 위한") }
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 6. 전위 검색 word*
    // ────────────────────────────────────────────────────────────────

    @Nested
    inner class `6 전위 검색` {

        @Test
        fun `전위검색(word*) — username 에서 지정 접두사로 시작하는 단어 모두 매칭된다`() {
            join("android-a", "안드로이드 가이드")    // ✓ android*
            join("android-guide", "일반 사용자")      // ✓ android*
            join("dev-guide", "개발 가이드")          // ✗ android 없음

            val result = search("android*")

            assertThat(result.content).hasSize(2)
            assertThat(result.content).allMatch { it.username.startsWith("android") }
        }

        @Test
        fun `전위검색(word*) — nickname 에 영문이 있는 경우 전위 검색이 동작한다`() {
            // TokenBigram: 영문은 단어 단위로 토큰화되어 prefix 검색이 정상 동작
            // 한국어는 바이그램으로 분리되어 prefix 검색이 동작하지 않음 (username 전위 검색 테스트 참조)
            join("user-a", "spring developer")      // ✓ spring*
            join("user-b", "springboot expert")     // ✓ springboot 도 spring* 에 매칭
            join("user-c", "react developer")       // ✗ spring 없음

            val result = search("spring*")

            assertThat(result.content).hasSize(2)
            assertThat(result.content).allMatch { it.nickname.startsWith("spring") }
        }

        @Test
        fun `전위검색(word*) — 접두사 이후 문자가 달라도 모두 매칭된다`() {
            join("backend-dev", "백엔드 개발자")
            join("backend-guru", "시니어 엔지니어")
            join("backend-manager", "팀 리더")
            join("frontend-dev", "프런트엔드 개발자")  // ✗ backend 없음

            val result = search("backend*")

            assertThat(result.content).allMatch { it.username.startsWith("backend") }
            assertThat(result.content).noneMatch { it.username.startsWith("frontend") }
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 7. 복합 조건
    // ────────────────────────────────────────────────────────────────

    @Nested
    inner class `7 복합 조건` {

        @Test
        fun `복합 AND+NOT — 특정 단어 포함 + 다른 단어 제외`() {
            join("android-a", "안드로이드 가이드")   // ✓
            join("android-b", "안드로이드 레시피")   // ✗ 레시피 포함
            join("guide-c", "개발 가이드")           // ✗ android 없음

            val result = search("android -레시피")

            assertThat(result.content).allMatch { (it.username + it.nickname).contains("android") }
            assertThat(result.content).noneMatch { it.nickname.contains("레시피") }
        }

        @Test
        fun `복합 OR+NOT — 두 단어 중 하나 포함하되 특정 단어는 제외`() {
            join("android-a", "안드로이드 가이드")           // ✓ 안드로이드, 레시피 없음
            join("ios-a", "iOS 가이드")                     // ✓ iOS, 레시피 없음
            join("android-b", "안드로이드 개발 레시피")     // ✗ 레시피 포함

            val result = search("(안드로이드 OR iOS) -레시피")

            assertThat(result.content).allMatch { m ->
                (m.username + m.nickname).contains("안드로이드") ||
                    (m.username + m.nickname).contains("iOS")
            }
            assertThat(result.content).noneMatch { it.nickname.contains("레시피") }
        }

        @Test
        fun `복합 구문검색+AND — 구문과 추가 키워드를 조합한다`() {
            join("android-guide-a", "안드로이드 가이드")     // ✓ "안드로이드 가이드" + android
            join("android-other", "안드로이드 앱 가이드")    // ✗ 구문 미일치
            join("guide-only", "안드로이드 가이드")          // ✗ android username 없음

            val result = search("\"안드로이드 가이드\" android")

            assertThat(result.content).allMatch { m ->
                (m.username + m.nickname).contains("android") &&
                    m.nickname == "안드로이드 가이드"
            }
        }

        @Test
        fun `복합 전위검색+NOT — 접두사 매칭 결과에서 특정 단어 제외`() {
            join("backend-dev", "백엔드 개발자")       // ✓
            join("backend-dba", "데이터베이스 전문가") // ✗ dba 포함 (제외 대상)
            join("backend-api", "API 서버 개발자")     // ✓

            val result = search("backend* -dba")

            assertThat(result.content).noneMatch { (it.username + it.nickname).contains("dba") }
            assertThat(result.content).allMatch { it.username.startsWith("backend") }
        }

        @Test
        fun `복합 실전 시나리오 — 관리자가 개발자 회원을 역할별로 조합 검색한다`() {
            join("android-a", "안드로이드 가이드")
            join("guide-search", "안드로이드 레시피")
            join("dev-guide", "개발 가이드")
            join("android-guide", "일반 사용자")

            // username 에 guide 가 있거나 nickname 에 가이드 가 있는 회원
            val result = search("guide OR 가이드")

            assertThat(result.content.size).isGreaterThanOrEqualTo(3)
            assertThat(result.content).allMatch { m ->
                (m.username + m.nickname).contains("guide") ||
                    (m.username + m.nickname).contains("가이드")
            }
        }
    }
}
