# slogApp (Kotlin Multiplatform)

슬로그의 Android / iOS / Web 앱. UI 는 Compose Multiplatform 으로 공유하고,
**글 본문만 웹뷰**로 띄운다 (본문 렌더링은 이미 웹에 다 있으므로 중복 구현하지 않는다).

## 구조

```
composeApp/src/
  commonMain/   공유 UI·API 클라이언트 (Compose, Ktor)
  androidMain/  Android 진입점 + WebView + 카카오 SDK 연동 자리
  iosMain/      iOS 진입점 + WKWebView + 카카오 SDK 연동 자리
  wasmJsMain/   Web(Wasm) 진입점 + iframe 웹뷰
```

플랫폼 차이는 `platform/Platform.kt` 의 `expect` 두 개로만 갈린다.

| expect | Android | iOS | Web |
|---|---|---|---|
| `DocumentWebView` | `android.webkit.WebView` | `WKWebView` | 캔버스 위에 겹친 `<iframe>` |
| `kakaoLogin` | 카카오 SDK (예정) | 카카오 SDK (예정) | OAuth 리다이렉트 |

## 실행

```bash
# Web — 이 환경에서 유일하게 빌드·실행 가능한 타깃
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Web 배포물
./gradlew :composeApp:wasmJsBrowserDistribution

# 공용 테스트
./gradlew :composeApp:allTests
```

## 알아둘 것

- **Android 타깃은 SDK 가 있을 때만 활성화**된다 (`ANDROID_HOME` 또는 `local.properties`).
  SDK 없이도 Web/iOS 소스가 빌드되도록 `composeApp/build.gradle.kts` 에서 분기한다.
- **iOS 는 macOS + Xcode 에서만 빌드**된다. Windows 에서는 소스만 관리한다.
- **한글 폰트를 직접 번들한다.** Compose for Web 은 캔버스에 글자를 그려서
  시스템 폰트를 쓰지 않는다. 폰트가 없으면 한글이 전부 두부(□)로 보인다.
  `composeResources/font/` 의 Pretendard 를 `ui/Theme.kt` 에서 전체 타이포그래피에 적용한다.
- **설정 캐시는 꺼져 있다.** Kotlin/JS webpack 태스크가 아직 지원하지 않는다.
- 로컬에서 `https://api.slog.gg` 를 직접 부르면 CORS 로 막힌다
  (백엔드가 `https://www.slog.gg` origin 만 허용). 개발 중에는 프록시나 스텁이 필요하다.

## 카카오 네이티브 로그인

백엔드에 **카카오 액세스 토큰을 slog 세션으로 교환하는 엔드포인트가 필요**하다.
클라이언트가 기대하는 계약은 `data/SlogApi.kt` 의 `loginWithKakao` 참고:

```
POST /member/api/v1/auth/social/kakao   { "accessToken": "..." }
  -> RsData<{ item, apiKey, accessToken }>   // 기존 /login 과 같은 모양
```
