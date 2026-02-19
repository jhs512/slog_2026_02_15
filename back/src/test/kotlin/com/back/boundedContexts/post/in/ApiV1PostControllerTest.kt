package com.back.boundedContexts.post.`in`

import com.back.boundedContexts.member.app.shared.ActorFacade
import com.back.boundedContexts.post.app.PostFacade
import com.back.standard.dto.post.type1.PostSearchSortType1
import com.back.standard.extensions.getOrThrow
import org.hamcrest.Matchers
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1PostControllerTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var postFacade: PostFacade

    @Autowired
    private lateinit var actorFacade: ActorFacade


    @Nested
    inner class Write {
        @Test
        @WithUserDetails("user1")
        fun `인증된 사용자가 글을 작성하면 제목과 내용이 저장된 게시글이 정상 생성된다`() {
            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "title": "제목",
                                "content": "내용"
                            }
                            """
                        )
                )
                .andDo(print())

            val post = postFacade.findLatest().getOrThrow()

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글이 작성되었습니다.".format(post.id)))
                .andExpect(jsonPath("$.data.id").value(post.id))
                .andExpect(jsonPath("$.data.createdAt").value(Matchers.startsWith(post.createdAt.toString().take(20))))
                .andExpect(jsonPath("$.data.modifiedAt").value(Matchers.startsWith(post.modifiedAt.toString().take(20))))
                .andExpect(jsonPath("$.data.authorId").value(post.author.id))
                .andExpect(jsonPath("$.data.authorName").value(post.author.name))
                .andExpect(jsonPath("$.data.title").value("제목"))
                .andExpect(jsonPath("$.data.published").value(false))
                .andExpect(jsonPath("$.data.listed").value(false))
        }

        @Test
        @WithUserDetails("user1")
        fun `성공 - 공개 글 작성`() {
            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "title": "공개 글",
                                "content": "내용",
                                "published": true,
                                "listed": true
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.published").value(true))
                .andExpect(jsonPath("$.data.listed").value(true))
        }

        @Test
        fun `성공 - 잘못된 API 키와 유효한 액세스 토큰이 있어도 글 작성이 처리된다`() {
            val actor = actorFacade.findByUsername("user1").getOrThrow()
            val actorAccessToken = actorFacade.genAccessToken(actor)

            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer wrong-api-key $actorAccessToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "title": "제목",
                                "content": "내용"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isCreated)
        }

        @Test
        @WithUserDetails("user1")
        fun `실패 - 제목 없이`() {
            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "title": "",
                                "content": "내용"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(
                    jsonPath("$.msg").value(
                        """
                        title-NotBlank-must not be blank
                        title-Size-size must be between 2 and 100
                        """.trimIndent()
                    )
                )
        }

        @Test
        @WithUserDetails("user1")
        fun `실패 - 내용 없이`() {
            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "title": "제목",
                                "content": ""
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(
                    jsonPath("$.msg").value(
                        """
                        content-NotBlank-must not be blank
                        content-Size-size must be between 2 and 2147483647
                        """.trimIndent()
                    )
                )
        }

        @Test
        @WithUserDetails("user1")
        fun `실패 - 잘못된 데이터 형식`() {
            val wrongJsonBody = """
                {
                    "title": 제목",
                    content": "내용"

                """.trimIndent()

            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongJsonBody)
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(
                    jsonPath("$.msg").value("요청 본문이 올바르지 않습니다.".trim())
                )
        }

        @Test
        fun `실패 - 인증 없이`() {
            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "title": "제목",
                                "content": "내용"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("로그인 후 이용해주세요."))
        }

        @Test
        fun `실패 - 잘못된 인증 헤더`() {
            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer wrong-api-key")
                        .content(
                            """
                            {
                                "title": "제목",
                                "content": "내용"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("401-3"))
                .andExpect(jsonPath("$.msg").value("API 키가 유효하지 않습니다."))
        }
    }


    @Nested
    inner class GetItem {
        @Test
        fun `성공 - 공개 글`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts/$id")
                )
                .andDo(print())

            val post = postFacade.findById(id).getOrThrow()

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getItem"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(post.id))
                .andExpect(
                    jsonPath("$.createdAt")
                        .value(Matchers.startsWith(post.createdAt.toString().take(20)))
                )
                .andExpect(
                    jsonPath("$.modifiedAt")
                        .value(Matchers.startsWith(post.modifiedAt.toString().take(20)))
                )
                .andExpect(jsonPath("$.authorId").value(post.author.id))
                .andExpect(jsonPath("$.authorName").value(post.author.name))
                .andExpect(jsonPath("$.title").value(post.title))
                .andExpect(jsonPath("$.content").value(post.content))
                .andExpect(jsonPath("$.published").value(post.published))
                .andExpect(jsonPath("$.listed").value(post.listed))
        }

        @Test
        @WithUserDetails("user1")
        fun `성공 - 미공개 글 작성자 조회`() {
            val actor = actorFacade.findByUsername("user1").getOrThrow()
            val post = postFacade.write(actor, "미공개 글", "내용", false, false)

            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts/${post.id}")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.published").value(false))
        }

        @Test
        fun `실패 - 존재하지 않는 글`() {
            val id = Int.MAX_VALUE

            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts/$id")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getItem"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."))
        }

        @Test
        @WithUserDetails("user3")
        fun `실패 - 미공개 글 다른 사용자`() {
            val actor = actorFacade.findByUsername("user1").getOrThrow()
            val post = postFacade.write(actor, "미공개 글", "내용", false, false)

            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts/${post.id}")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.resultCode").value("403-3"))
        }
    }


    @Nested
    inner class GetItems {
        @Test
        fun `성공 - 기본값 조회`() {
            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts")
                )
                .andDo(print())

            val posts = postFacade.findPagedByKw(
                "",
                PostSearchSortType1.CREATED_AT,
                1,
                5
            ).content

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(posts.size))
        }

        @Test
        fun `성공 - 페이지와 페이지 크기 경계 보정 조회`() {
            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts?page=0&pageSize=31")
                )
                .andDo(print())

            val posts = postFacade.findPagedByKw(
                "",
                PostSearchSortType1.CREATED_AT,
                1,
                30
            ).content

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(posts.size))
        }

        @Test
        fun `성공 - 공백 키워드 검색`() {
            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts")
                        .param("kw", "   ")
                )
                .andDo(print())

            val posts = postFacade.findPagedByKw(
                "   ",
                PostSearchSortType1.CREATED_AT,
                1,
                5
            ).content

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(posts.size))
        }

        @Test
        fun `성공 - 기본 페이징`() {
            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts?page=1&pageSize=5")
                )
                .andDo(print())

            val postPage = postFacade.findPagedByKw(
                "",
                PostSearchSortType1.CREATED_AT,
                1,
                5
            )

            val posts = postPage.content

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(posts.size))

            for (i in posts.indices) {
                val post = posts[i]
                resultActions
                    .andExpect(jsonPath("$.content[%d].id".format(i)).value(post.id))
                    .andExpect(
                        jsonPath("$.content[%d].createdAt".format(i))
                            .value(Matchers.startsWith(post.createdAt.toString().take(20)))
                    )
                    .andExpect(
                        jsonPath("$.content[%d].modifiedAt".format(i))
                            .value(Matchers.startsWith(post.modifiedAt.toString().take(20)))
                    )
                    .andExpect(jsonPath("$.content[%d].authorId".format(i)).value(post.author.id))
                    .andExpect(jsonPath("$.content[%d].authorName".format(i)).value(post.author.name))
                    .andExpect(jsonPath("$.content[%d].title".format(i)).value(post.title))
            }
        }

        @Test
        fun `성공 - 키워드 검색`() {
            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts?page=1&pageSize=5&kw=제목 1")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
        }

        @Test
        fun `성공 - PGroonga title 필드 지정 검색`() {
            val actor = actorFacade.findByUsername("user1").getOrThrow()
            val titleOnlyPost = postFacade.write(
                actor,
                "title:@스프링 고급",
                "백엔드와 안드로이드 이야기"
            )
            val contentOnlyPost = postFacade.write(
                actor,
                "일반 글 1",
                "content:@스프링 기본 문서"
            )

            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts")
                        .param("kw", "title:@스프링")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content[*].id").value(Matchers.hasItem(titleOnlyPost.id)))
                .andExpect(jsonPath("$.content[*].id").value(Matchers.not(Matchers.hasItem(contentOnlyPost.id))))
        }

        @Test
        fun `성공 - PGroonga content 필드 지정 검색`() {
            val actor = actorFacade.findByUsername("user1").getOrThrow()
            val contentOnlyPost = postFacade.write(
                actor,
                "자바 소개 글",
                "content:@자바 핵심 정리"
            )
            val titleOnlyPost = postFacade.write(
                actor,
                "title:@자바 개념 정리",
                "스프링 기초부터 시작"
            )

            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts")
                        .param("kw", "content:@자바")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content[*].id").value(Matchers.hasItem(contentOnlyPost.id)))
                .andExpect(jsonPath("$.content[*].id").value(Matchers.not(Matchers.hasItem(titleOnlyPost.id))))
        }

        @Test
        fun `성공 - PGroonga 플러스 마이너스 연산자 검색`() {
            val actor = actorFacade.findByUsername("user1").getOrThrow()
            val targetPost = postFacade.write(
                actor,
                "PGroonga +스프링 대상",
                "실험용 내용"
            )
            val excludedByNotPost = postFacade.write(
                actor,
                "PGroonga 스프링 자바 혼합",
                "제외 대상"
            )
            val excludedByMissingPlusPost = postFacade.write(
                actor,
                "일반 글",
                "자바만 포함된 글"
            )

            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts")
                        .param("kw", "+스프링 -자바")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content[*].id").value(Matchers.hasItem(targetPost.id)))
                .andExpect(jsonPath("$.content[*].id").value(Matchers.not(Matchers.hasItem(excludedByNotPost.id))))
                .andExpect(jsonPath("$.content[*].id").value(Matchers.not(Matchers.hasItem(excludedByMissingPlusPost.id))))
        }

        @Test
        fun `성공 - PGroonga OR 연산자 검색`() {
            val actor = actorFacade.findByUsername("user1").getOrThrow()
            val springPost = postFacade.write(
                actor,
                "PGroonga 제목-스프링",
                "일반 본문"
            )
            val javaPost = postFacade.write(
                actor,
                "일반 제목",
                "PGroonga content-자바"
            )
            val irrelevantPost = postFacade.write(
                actor,
                "연관 없는 제목",
                "전혀 관련없는 내용"
            )

            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts")
                        .param("kw", "스프링 OR 자바")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content[*].id").value(Matchers.hasItem(springPost.id)))
                .andExpect(jsonPath("$.content[*].id").value(Matchers.hasItem(javaPost.id)))
                .andExpect(jsonPath("$.content[*].id").value(Matchers.not(Matchers.hasItem(irrelevantPost.id))))
        }

        @Test
        fun `성공 - PGroonga AND(플러스) 연산자 검색`() {
            val actor = actorFacade.findByUsername("user1").getOrThrow()
            val bothPost = postFacade.write(
                actor,
                "PGroonga 스프링 자바 동시 포함",
                "내용"
            )
            val springOnlyPost = postFacade.write(
                actor,
                "PGroonga 스프링 만 존재",
                "일반 본문"
            )
            val javaOnlyPost = postFacade.write(
                actor,
                "일반 제목",
                "PGroonga 자바 만 존재"
            )

            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts")
                        .param("kw", "+스프링 +자바")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content[*].id").value(Matchers.hasItem(bothPost.id)))
                .andExpect(jsonPath("$.content[*].id").value(Matchers.not(Matchers.hasItem(springOnlyPost.id))))
                .andExpect(jsonPath("$.content[*].id").value(Matchers.not(Matchers.hasItem(javaOnlyPost.id))))
        }
    }


    @Nested
    inner class Modify {
        @Test
        @WithUserDetails("user1")
        fun `인증된 작성자가 기존 글 수정 요청 시 글이 정상 변경된다`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    put("/post/api/v1/posts/$id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "title": "제목 new",
                                "content": "내용 new"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글이 수정되었습니다.".format(id)))
        }

        @Test
        @WithUserDetails("user3")
        fun `실패 - 권한 없음`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    put("/post/api/v1/posts/$id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "title": "제목 new",
                                "content": "내용 new"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.resultCode").value("403-1"))
                .andExpect(jsonPath("$.msg").value("작성자만 글을 수정할 수 있습니다."))
        }

        @Test
        @WithUserDetails("user1")
        fun `실패 - 존재하지 않는 글`() {
            val id = Int.MAX_VALUE

            val resultActions = mvc
                .perform(
                    put("/post/api/v1/posts/$id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "title": "제목 new",
                                "content": "내용 new"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."))
        }
    }


    @Nested
    inner class Delete {
        @Test
        @WithUserDetails("user1")
        fun `작성자가 본인 글 삭제 요청 시 삭제가 성공적으로 처리된다`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    delete("/post/api/v1/posts/$id")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글이 삭제되었습니다.".format(id)))
        }

        @Test
        @WithUserDetails("admin")
        fun `성공 - 관리자`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    delete("/post/api/v1/posts/$id")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글이 삭제되었습니다.".format(id)))
        }

        @Test
        @WithUserDetails("user3")
        fun `실패 - 권한 없음`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    delete("/post/api/v1/posts/$id")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.resultCode").value("403-2"))
                .andExpect(jsonPath("$.msg").value("작성자만 글을 삭제할 수 있습니다."))
        }

        @Test
        @WithUserDetails("user1")
        fun `실패 - 존재하지 않는 글`() {
            val id = Int.MAX_VALUE

            val resultActions = mvc
                .perform(
                    delete("/post/api/v1/posts/$id")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."))
        }
    }


    @Nested
    inner class IncrementHit {
        @Test
        fun `글 조회가 호출되면 조회수 증가가 정상 반영된다`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts/$id/hit")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("incrementHit"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("조회수가 증가했습니다."))
                .andExpect(jsonPath("$.data.hitCount").isNumber)
        }

        @Test
        fun `실패 - 존재하지 않는 글`() {
            val id = Int.MAX_VALUE

            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts/$id/hit")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("incrementHit"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."))
        }
    }


    @Nested
    inner class ToggleLike {
        @Test
        @WithUserDetails("user1")
        fun `성공 - 좋아요 추가`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts/$id/like")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("toggleLike"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("좋아요를 눌렀습니다."))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likesCount").isNumber)
        }

        @Test
        @WithUserDetails("user1")
        fun `성공 - 좋아요 취소`() {
            val id = 1

            // 첫 번째: 좋아요 추가
            mvc.perform(post("/post/api/v1/posts/$id/like"))

            // 두 번째: 좋아요 취소
            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts/$id/like")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("toggleLike"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("좋아요를 취소했습니다."))
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likesCount").isNumber)
        }

        @Test
        fun `실패 - 인증 없이`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts/$id/like")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("로그인 후 이용해주세요."))
        }
    }


    @Nested
    inner class GetMine {
        @Test
        @WithUserDetails("user1")
        fun `로그인한 사용자가 내 글 목록을 조회하면 본인 게시글만 반환된다`() {
            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts/mine?page=1&pageSize=10")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getMine"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
        }

        @Test
        fun `실패 - 인증 없이`() {
            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts/mine?page=1&pageSize=10")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("로그인 후 이용해주세요."))
        }
    }


    @Nested
    inner class GetOrCreateTemp {
        @Test
        @WithUserDetails("user1")
        fun `성공 - 새 임시글`() {
            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts/temp")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getOrCreateTemp"))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.data.published").value(false))
                .andExpect(jsonPath("$.data.listed").value(false))
        }

        @Test
        @WithUserDetails("user1")
        fun `성공 - 기존 임시글`() {
            // 첫 번째 호출: 임시글 생성
            mvc.perform(post("/post/api/v1/posts/temp"))

            // 두 번째 호출: 기존 임시글 반환
            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts/temp")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getOrCreateTemp"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("기존 임시저장 글을 반환합니다."))
        }

        @Test
        fun `실패 - 인증 없이`() {
            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts/temp")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("로그인 후 이용해주세요."))
        }
    }
}
