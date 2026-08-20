package gg.slog.app.platform

import android.content.Context
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import gg.slog.app.AppContextHolder
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 카카오톡 앱이 깔려 있으면 그쪽으로, 아니면 카카오계정 웹 로그인으로 진행한다.
 * 여기서 받은 액세스 토큰을 백엔드(/auth/social/kakao)에 넘겨 slog 세션으로 바꾼다.
 */
internal suspend fun kakaoLoginAndroid(): KakaoLoginResult {
    val context: Context = AppContextHolder.activityOrApp
        ?: return KakaoLoginResult.Failed("액티비티 컨텍스트가 없습니다.")

    val talkAvailable = UserApiClient.instance.isKakaoTalkLoginAvailable(context)

    return suspendCancellableCoroutine { cont ->
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            val result = when {
                error is ClientError && error.reason == ClientErrorCause.Cancelled ->
                    KakaoLoginResult.Cancelled
                error != null -> KakaoLoginResult.Failed(error.message ?: "알 수 없는 오류")
                token != null -> KakaoLoginResult.Success(token.accessToken)
                else -> KakaoLoginResult.Failed("토큰이 비어 있습니다.")
            }
            if (cont.isActive) cont.resume(result)
        }

        if (talkAvailable) {
            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                // 카카오톡 로그인이 실패하면 계정 로그인으로 넘어간다
                if (error != null && !(error is ClientError && error.reason == ClientErrorCause.Cancelled)) {
                    UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
                } else {
                    callback(token, error)
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
        }
    }
}
