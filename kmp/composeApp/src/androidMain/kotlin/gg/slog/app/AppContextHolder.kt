package gg.slog.app

import android.app.Activity
import android.content.Context

/**
 * 카카오 SDK 의 로그인 API 는 Activity 컨텍스트를 요구한다.
 * Compose 공용 코드에서 컨텍스트를 넘기지 않으므로 여기에 보관한다.
 */
object AppContextHolder {
    @Volatile
    var activityOrApp: Context? = null
}
