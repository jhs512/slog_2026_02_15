package gg.slog.app

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class SlogApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val key = getString(R.string.kakao_native_app_key)
        if (key.isNotBlank()) {
            KakaoSdk.init(this, key)
        }
    }
}
