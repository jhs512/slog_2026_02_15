package com.back.boundedContexts.post.out

import com.back.boundedContexts.member.app.MemberFacade
import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.post.app.PostFacade
import com.back.boundedContexts.post.domain.Post
import com.back.standard.dto.post.type1.PostSearchSortType1
import com.back.standard.extensions.getOrThrow
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
 * PGroonga 4.x 복합 인덱스 검색 테스트 — Post (title + content)
 *
 * 인덱스 : idx_post_title_content_pgroonga  (title TEXT, content TEXT, TokenBigram)
 * 연산자 : ARRAY[title, content]
 *              &@~ (query, NULL, 'idx_post_title_content_pgroonga')
 *              ::pgroonga_full_text_search_condition
 *
 * 특징
 *  - 단일 인덱스 스캔 (BitmapOr 없음)
 *  - NOT(-) 가 제목·본문 전체에 전역 적용됨 (크로스컬럼 제외)
 *  - PGroonga query syntax 를 그대로 지원
 *
 * 커버하는 문법
 *  1. 기본 키워드
 *  2. AND  — 공백(묵시적) / + (명시적)
 *  3. OR
 *  4. NOT  — 단일·복수·크로스컬럼
 *  5. 구문 검색  "..."
 *  6. 전위 검색  word*
 *  7. 복합 조건  — AND+NOT / OR+NOT / 그룹화·구문 혼합
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PostRepositoryTest {

    @Autowired private lateinit var postRepository: PostRepository
    @Autowired private lateinit var postFacade: PostFacade
    @Autowired private lateinit var memberFacade: MemberFacade

    private fun user1(): Member = memberFacade.findByUsername("user1").getOrThrow()

    /** 공개 글 생성 헬퍼 */
    private fun write(title: String, content: String): Post =
        postFacade.write(user1(), title, content, published = true, listed = true)

    /** 공개 글 대상 키워드 검색 (publicOnly = true) */
    private fun search(kw: String): Page<Post> =
        postRepository.findQPagedByKw(
            kw,
            PageRequest.of(0, 100, PostSearchSortType1.CREATED_AT.sortBy),
        )

    // ────────────────────────────────────────────────────────────────
    // 1. 기본 키워드 검색
    // ────────────────────────────────────────────────────────────────

    @Nested
    inner class `1 기본 키워드 검색` {

        @Test
        fun `단일 키워드 — 제목에 있으면 매칭된다`() {
            write("딸기 맛 디저트", "아이스크림 후기")
            write("초콜릿 케이크", "맛있는 과자")

            val result = search("딸기")

            assertThat(result.content)
                .isNotEmpty
                .allMatch { it.title.contains("딸기") || it.content.contains("딸기") }
        }

        @Test
        fun `단일 키워드 — 본문에 있으면 매칭된다`() {
            write("평범한 제목", "본문에 망고가 등장한다")
            write("전혀 관계없는 과일 글", "전혀 관계없는 내용")  // 제목·본문 모두 망고 없음

            val result = search("망고")

            assertThat(result.content).anyMatch { it.content.contains("망고") }
            assertThat(result.content).noneMatch { it.title == "전혀 관계없는 과일 글" }
        }

        @Test
        fun `단일 키워드 — 제목과 본문 중 어느 하나에만 있어도 매칭된다`() {
            write("파파야 아이스크림", "달콤한 여름 디저트")  // 제목에만
            write("평범한 제목", "파파야 주스 레시피")       // 본문에만

            val result = search("파파야")

            assertThat(result.content).hasSize(2)
        }

        @Test
        fun `단일 키워드 — 제목·본문 어디에도 없으면 매칭되지 않는다`() {
            write("사과 이야기", "배 이야기")

            val result = search("두리안")

            assertThat(result.content)
                .noneMatch { it.title.contains("두리안") || it.content.contains("두리안") }
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 2. AND 검색 — 공백(묵시적) / +(명시적)
    // ────────────────────────────────────────────────────────────────

    @Nested
    inner class `2 AND 검색` {

        @Test
        fun `AND(공백) — 두 키워드를 공백으로 나열하면 둘 다 포함한 글만 매칭된다`() {
            write("스프링 부트 배포", "서버 설정 방법")       // ✓ 스프링 AND 부트
            write("스프링 입문", "파이썬 배포 방법")          // ✗ 부트 없음
            write("리액트 가이드", "부트스트랩 사용법")       // ✗ 스프링 없음

            val result = search("스프링 부트")

            assertThat(result.content).allMatch { post ->
                (post.title + post.content).contains("스프링") &&
                    (post.title + post.content).contains("부트")
            }
        }

        @Test
        fun `AND(공백) — 한 키워드만 있는 글은 AND 검색에서 제외된다`() {
            write("스프링 프레임워크", "자바 기반")      // ✗ 부트 없음
            write("부트 캠프 후기", "개발자 커리어")     // ✗ 스프링 없음
            write("스프링 부트 입문", "초보자 가이드")   // ✓

            val result = search("스프링 부트")

            assertThat(result.content).noneMatch {
                !(it.title + it.content).contains("스프링") ||
                    !(it.title + it.content).contains("부트")
            }
        }

        @Test
        fun `AND(공백) — 제목에 한 단어, 본문에 다른 단어가 있어도 AND 로 매칭된다`() {
            // 제목: "스프링", 본문: "도커" → 두 단어를 합산하면 두 컬럼에 걸쳐 AND 성립
            write("스프링 프레임워크", "도커 컨테이너와 함께 배포")

            val result = search("스프링 도커")

            assertThat(result.content).anyMatch {
                it.title.contains("스프링") && it.content.contains("도커")
            }
        }

        @Test
        fun `AND(+) — 플러스로 연결된 두 키워드도 AND 로 동작한다`() {
            write("쿠버네티스 클러스터", "테라폼으로 인프라 관리")  // ✓
            write("쿠버네티스 입문", "도커 컨테이너 기초")          // ✗ 테라폼 없음
            write("인프라 코드화", "앤서블로 테라폼 대체")          // ✗ 쿠버네티스 없음

            val result = search("+쿠버네티스 +테라폼")

            assertThat(result.content).isNotEmpty
            assertThat(result.content).allMatch { post ->
                (post.title + post.content).contains("쿠버네티스") &&
                    (post.title + post.content).contains("테라폼")
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
            write("파이썬 기초 강의", "변수와 자료형")
            write("자바스크립트 입문", "비동기 처리")
            write("데이터베이스 설계", "정규화 원칙")  // 둘 다 없음

            val result = search("파이썬 OR 자바스크립트")

            assertThat(result.content).anyMatch { (it.title + it.content).contains("파이썬") }
            assertThat(result.content).anyMatch { (it.title + it.content).contains("자바스크립트") }
            assertThat(result.content).noneMatch { it.title == "데이터베이스 설계" }
        }

        @Test
        fun `OR — 두 키워드 모두 포함한 글도 OR 검색에서 매칭된다`() {
            write("파이썬과 자바스크립트 비교", "두 언어의 차이점")

            val result = search("파이썬 OR 자바스크립트")

            assertThat(result.content).anyMatch {
                it.title.contains("파이썬") && it.title.contains("자바스크립트")
            }
        }

        @Test
        fun `OR — 세 키워드 중 하나라도 있으면 매칭된다`() {
            write("리눅스 서버 관리", "우분투 설정")
            write("맥OS 개발환경", "홈브류 패키지 관리")
            write("윈도우 서버", "IIS 웹서버 구성")
            write("클라우드 아키텍처", "마이크로서비스 패턴")  // 셋 다 없음

            val result = search("리눅스 OR 맥OS OR 윈도우")

            assertThat(result.content.map { it.title })
                .containsAnyOf("리눅스 서버 관리", "맥OS 개발환경", "윈도우 서버")
            assertThat(result.content).noneMatch { it.title == "클라우드 아키텍처" }
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 4. NOT 검색 (마이너스) — 전역 제외
    // ────────────────────────────────────────────────────────────────

    @Nested
    inner class `4 NOT 검색` {

        @Test
        fun `NOT(-) 단일 — 마이너스 앞 단어를 포함하는 글은 제외된다`() {
            write("사과와 배 이야기", "과일 정보")  // ✗ 배 포함
            write("사과 이야기", "다른 과일")        // ✓

            val result = search("사과 -배")

            assertThat(result.content).isNotEmpty
            assertThat(result.content)
                .noneMatch { (it.title + it.content).contains("배") }
        }

        @Test
        fun `NOT(-) 크로스컬럼 — 제목에 제외 단어가 있으면 본문이 매칭돼도 전역 제외된다`() {
            // ARRAY[] + pgroonga_full_text_search_condition 방식이기 때문에
            // OR 방식이었다면 content 매칭으로 포함됐겠지만, 단일 복합 인덱스 스캔에서 -는 전역 적용됨
            write("제외단어 포함 제목", "검색어 있는 본문")  // ✗ 제목에 제외단어
            write("일반 제목", "검색어 있는 본문")           // ✓

            val result = search("검색어 -제외단어")

            assertThat(result.content)
                .noneMatch { (it.title + it.content).contains("제외단어") }
            assertThat(result.content)
                .anyMatch { it.title == "일반 제목" }
        }

        @Test
        fun `NOT(-) 복수 — 여러 단어를 동시에 제외할 수 있다`() {
            write("사과 바나나", "맛있는 과일")   // ✗ 바나나 포함
            write("사과 오렌지", "비타민 과일")   // ✗ 오렌지 포함
            write("포도 바나나", "달콤한 과일")   // ✗ 바나나 포함
            write("포도 키위", "과일 정보")       // ✓

            val result = search("과일 -바나나 -오렌지")

            assertThat(result.content)
                .noneMatch { (it.title + it.content).contains("바나나") }
            assertThat(result.content)
                .noneMatch { (it.title + it.content).contains("오렌지") }
        }

        @Test
        fun `AND+NOT — 필수 포함(+)과 제외(-) 조합이 동시에 적용된다`() {
            write("사과 바나나", "맛있는 과일")  // ✗ 바나나 포함
            write("사과 딸기", "달콤한 과일")    // ✓
            write("포도 바나나", "새콤달콤")     // ✗ 사과 없음

            val result = search("+사과 -바나나")

            assertThat(result.content).isNotEmpty
            assertThat(result.content).allMatch { post ->
                (post.title + post.content).contains("사과") &&
                    !(post.title + post.content).contains("바나나")
            }
        }

        @Test
        fun `NOT(-) 실전 시나리오 — 기술 블로그에서 특정 스택을 전역 제외한다`() {
            write("스프링부트/GIT", "테라폼 한밭대 강의 정리")   // ✗ GIT 포함
            write("리눅스/도커", "테라폼 한밭대 강의 정리")       // ✗ 리눅스 포함
            write("테라폼 한밭대 입문", "클라우드 수업")           // ✓

            val result = search("테라폼 +한밭대 -리눅스 -GIT")

            assertThat(result.content)
                .noneMatch { (it.title + it.content).contains("GIT") }
            assertThat(result.content)
                .noneMatch { (it.title + it.content).contains("리눅스") }
            assertThat(result.content)
                .anyMatch { it.title.contains("테라폼") && it.title.contains("한밭대") }
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 5. 구문 검색 "..."
    // ────────────────────────────────────────────────────────────────

    @Nested
    inner class `5 구문 검색` {

        @Test
        fun `구문검색 — 따옴표 안 단어들이 연속·인접해야 매칭된다`() {
            write("사과 바나나 조합 레시피", "과일 샐러드")      // ✓ "사과 바나나" 인접
            write("사과가 좋고 바나나도 좋다", "과일 비교")      // ✗ 사이에 다른 단어

            val result = search("\"사과 바나나\"")

            assertThat(result.content).anyMatch { it.title.startsWith("사과 바나나") }
            assertThat(result.content).noneMatch { it.title.startsWith("사과가 좋고") }
        }

        @Test
        fun `구문검색 — 단어 사이에 다른 단어가 끼면 구문 검색에서 제외된다`() {
            write("바른 처리 방법", "효율적인 알고리즘")        // ✓ 바른-처리 인접
            write("바른 방식으로 처리하는 법", "설계 원칙")    // ✗ 바른-처리 비인접

            val result = search("\"바른 처리\"")

            assertThat(result.content).anyMatch { it.title == "바른 처리 방법" }
            assertThat(result.content).noneMatch { it.title.startsWith("바른 방식으로") }
        }

        @Test
        fun `구문검색 — 본문에서도 정확한 구문이 매칭된다`() {
            write("알고리즘 강의", "빠른 정렬 알고리즘 설명")          // ✓ 본문에 "빠른 정렬" 인접
            write("정렬 알고리즘", "매우 빠른 방식으로 정렬 수행")     // ✗ 비인접

            val result = search("\"빠른 정렬\"")

            assertThat(result.content).anyMatch { it.content.contains("빠른 정렬") }
            assertThat(result.content).noneMatch { it.title == "정렬 알고리즘" }
        }

        @Test
        fun `구문검색 — 영문 구문도 정확히 인접한 경우만 매칭된다`() {
            write("spring boot quickstart", "getting started guide")  // ✓
            write("spring and boot separately", "not adjacent here") // ✗

            val result = search("\"spring boot\"")

            assertThat(result.content).anyMatch { it.title.contains("spring boot") }
            assertThat(result.content).noneMatch { it.title.contains("spring and boot") }
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 6. 전위 검색 word*
    // ────────────────────────────────────────────────────────────────

    @Nested
    inner class `6 전위 검색` {

        @Test
        fun `전위검색(word*) — 지정 접두사로 시작하는 단어 모두 매칭된다`() {
            write("spring framework 소개", "enterprise java 개발")     // ✓ spring
            write("springframework 의존성", "maven build 설정")        // ✓ spring*
            write("springboot 자동설정", "annotation 기반 설정")       // ✓ spring*
            write("hibernate orm", "jpa 표준 구현체")                  // ✗ spring 없음

            val result = search("spring*")

            assertThat(result.content).anyMatch { (it.title + it.content).contains("spring") }
            assertThat(result.content).noneMatch { it.title == "hibernate orm" }
        }

        @Test
        fun `전위검색(word*) — 접두사 이후 문자가 달라도 모두 매칭된다`() {
            write("docker run 명령어", "컨테이너 실행")          // ✓
            write("dockerfile 작성법", "이미지 빌드")            // ✓
            write("docker-compose 설정", "다중 컨테이너 관리")   // ✓
            write("kubernetes 클러스터", "컨테이너 오케스트레이션")  // ✗ docker 없음

            val result = search("docker*")

            assertThat(result.content).allMatch { (it.title + it.content).contains("docker") }
            assertThat(result.content).noneMatch { it.title == "kubernetes 클러스터" }
        }

        @Test
        fun `전위검색(word*) — TokenBigram에서 한국어는 2글자 바이그램으로 분리되어 prefix 검색이 동작하지 않는다`() {
            // TokenBigram: "스프링" → ["스프", "프링"] (2글자 단위로 분리)
            // "스프링*" prefix 검색은 "스프링"으로 시작하는 인덱스 토큰을 찾지만
            // 바이그램 토큰("스프", "프링")은 모두 2글자이므로 "스프링"으로 시작하지 않음
            // → 한국어 prefix 검색은 TokenBigram에서 빈 결과를 반환함
            write("스프링 개발 가이드", "백엔드 프레임워크")
            write("스프링부트 자동설정", "의존성 주입")

            val result = search("스프링*")

            // 한국어 전위 검색은 TokenBigram에서 지원되지 않음
            assertThat(result.content).isEmpty()
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 7. 복합 조건
    // ────────────────────────────────────────────────────────────────

    @Nested
    inner class `7 복합 조건` {

        @Test
        fun `복합 AND+NOT — 특정 단어 포함하되 다른 단어 제외`() {
            write("자바 스프링 웹 개발", "MVC 패턴 적용")
            write("자바 코틀린 비교", "JVM 언어 선택")          // ✗ 코틀린 포함
            write("파이썬 장고 웹 개발", "MTV 패턴 적용")        // ✗ 자바 없음

            val result = search("자바 -코틀린")

            assertThat(result.content).allMatch { (it.title + it.content).contains("자바") }
            assertThat(result.content).noneMatch { (it.title + it.content).contains("코틀린") }
        }

        @Test
        fun `복합 OR+NOT — 둘 중 하나라도 있지만 특정 단어는 없어야 한다`() {
            write("파이썬 머신러닝", "딥러닝 모델")              // ✗ 머신러닝 포함
            write("R 통계 분석", "머신러닝 알고리즘")            // ✗ 머신러닝 포함
            write("파이썬 웹 크롤링", "데이터 수집")             // ✓
            write("R 데이터 시각화", "ggplot 그래프")            // ✓

            val result = search("(파이썬 OR R) -머신러닝")

            assertThat(result.content)
                .noneMatch { (it.title + it.content).contains("머신러닝") }
            assertThat(result.content)
                .allMatch { post ->
                    (post.title + post.content).contains("파이썬") ||
                        (post.title + post.content).contains("R ")  // R 은 단어 단위 검색
                }
        }

        @Test
        fun `복합 그룹화 (A OR B) AND C — 괄호 그룹과 AND 조합`() {
            write("MySQL 데이터베이스 설계", "인덱스 최적화")       // ✓
            write("PostgreSQL 데이터베이스 설계", "인덱스 최적화") // ✓
            write("MongoDB 설계 패턴", "도큐먼트 모델")            // ✗ 인덱스 없음
            write("MySQL 백업 방법", "복구 전략")                  // ✗ 설계 없음

            val result = search("(MySQL OR PostgreSQL) 설계")

            assertThat(result.content).allMatch { post ->
                ((post.title + post.content).contains("MySQL") ||
                    (post.title + post.content).contains("PostgreSQL")) &&
                    (post.title + post.content).contains("설계")
            }
            assertThat(result.content).noneMatch { it.title.contains("MongoDB") }
            assertThat(result.content).noneMatch { it.title.contains("백업") }
        }

        @Test
        fun `복합 구문검색+AND — 구문이 포함된 글 중 추가 키워드로 좁힌다`() {
            write("스프링 부트 시작하기", "의존성 주입 패턴 설명")  // ✓ 구문+의존성
            write("스프링 부트 고급 설정", "AOP 설명")              // ✗ 의존성 없음
            write("스프링 MVC 가이드", "의존성 주입 방법")          // ✗ "스프링 부트" 구문 없음

            val result = search("\"스프링 부트\" 의존성")

            assertThat(result.content).anyMatch {
                it.title.contains("스프링 부트") &&
                    (it.title + it.content).contains("의존성")
            }
            assertThat(result.content).noneMatch { it.title.contains("고급 설정") }
            assertThat(result.content).noneMatch { it.title.contains("MVC 가이드") }
        }

        @Test
        fun `복합 OR+구문검색 — 두 구문 중 하나라도 있으면 매칭된다`() {
            write("빠른 정렬 알고리즘", "효율적인 정렬")    // ✓ "빠른 정렬" 구문
            write("이진 탐색 알고리즘", "효율적인 검색")    // ✓ "이진 탐색" 구문
            write("해시 맵 구현", "충돌 처리")              // ✗ 두 구문 모두 없음

            val result = search("\"빠른 정렬\" OR \"이진 탐색\"")

            assertThat(result.content).hasSize(2)
            assertThat(result.content).noneMatch { it.title.contains("해시") }
        }

        @Test
        fun `복합 다중AND+다중NOT — 실제 클라우드 기술 블로그 검색 시나리오`() {
            write("AWS EKS 클러스터 배포", "쿠버네티스 관리형 서비스")    // ✓
            write("GCP GKE 클러스터 배포", "쿠버네티스 구글 클라우드")    // ✗ GCP 포함
            write("AWS Lambda 서버리스", "이벤트 기반 실행")              // ✗ Lambda 포함
            write("Azure AKS 클러스터 배포", "쿠버네티스 마이크로소프트") // ✗ AWS 없음

            val result = search("AWS 클러스터 -Lambda -GCP")

            assertThat(result.content).allMatch { (it.title + it.content).contains("AWS") }
            assertThat(result.content).noneMatch { (it.title + it.content).contains("Lambda") }
            assertThat(result.content).noneMatch { (it.title + it.content).contains("GCP") }
        }

        @Test
        fun `복합 전위검색+NOT — 접두사 매칭 결과에서 특정 단어 제외`() {
            write("spring boot 입문", "의존성 주입")          // ✓
            write("spring security 설정", "인증 인가")        // ✗ security 제외 대상
            write("springframework core", "bean 생명주기")   // ✓

            val result = search("spring* -security")

            assertThat(result.content)
                .noneMatch { (it.title + it.content).contains("security") }
            assertThat(result.content)
                .allMatch { (it.title + it.content).contains("spring") }
        }
    }
}
