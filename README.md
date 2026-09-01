# Shorts Blocker

숏폼 콘텐츠(YouTube Shorts, Instagram Reels, TikTok, Facebook Reels)만 선택적으로 차단하는 온디바이스 Android 앱입니다. 같은 앱의 다른 기능 — YouTube 롱폼 영상, Instagram DM·일반 피드, Facebook Messenger·뉴스피드 —은 평소처럼 사용할 수 있습니다.

## 왜 만들었는가

숏폼 피드는 "무한 스크롤"을 전제로 설계되어 있어서, 앱 자체를 통째로 차단하면 정작 필요한 기능(메시지 확인, 정보 검색 등)까지 막혀 오래 쓰기 어렵습니다. 반대로 스크린타임 앱들이 흔히 쓰는 "앱 사용 시간 제한"은 숏폼 탭 하나만 골라 차단할 수 없습니다. 이 프로젝트는 **화면 단위로 콘텐츠를 구분해서 숏폼 화면만 걸러내는** 자기 통제 도구를 직접 만들어보자는 목표에서 시작했습니다.

가장 까다로웠던 기술적 문제는 두 가지였습니다.

1. **"지금 보고 있는 화면이 숏폼인가?"를 어떻게 판정할 것인가.** Android `AccessibilityService`로 화면의 뷰 트리(view tree)를 읽어와 `viewId` 정규식으로 1차 판정하고, 하단 탭바처럼 여러 항목이 같은 `viewId` 패턴을 공유하는 경우엔 `text`/`contentDescription`까지 추가로 매칭해 오탐을 줄였습니다. 이 판정 로직(`RuleEvaluator`)은 실제 `AccessibilityNodeInfo`에 의존하지 않는 `AccessibleNode` 인터페이스 위에서 동작하도록 분리해, 실제 기기 없이도 JUnit으로 순수 로직만 테스트할 수 있게 만들었습니다 (`RuleEvaluatorTest.kt`, 16개 케이스).
2. **화면 판정이 메인 스레드를 막지 않게 하는 것.** 뷰 트리 순회를 노드 수(최대 500개)와 깊이(최대 60단계)로 제한해 이벤트 하나당 처리 비용의 상한을 뒀고, `AccessibilityNodeInfo` 재활용(recycle) 누락으로 인한 메모리 누수도 테스트로 잡아냈습니다.

설계 과정에서 내린 판단들(룰 갱신 방식, PIN 분실 복구 방식, OS 레벨 한계를 사용자에게 숨기지 않고 고지하기로 한 이유 등)은 [`docs/DECISIONS.md`](docs/DECISIONS.md)에 기록되어 있습니다.

## 주요 기능

- **선택적 숏폼 차단** — YouTube Shorts / Instagram Reels / TikTok / Facebook Reels 재생 화면과 탭을 감지해 자동으로 뒤로 가기 처리. TikTok과 Snapchat은 앱 전체 차단(FULL 모드), 나머지는 화이트리스트 기반 부분 차단(PARTIAL 모드)
- **브라우저 URL 차단** — Chrome / 삼성 인터넷 / Firefox / Edge에서 `youtube.com/shorts`, `instagram.com/reels` 등 숏폼 URL이 열리는 것도 감지
- **PIN + 복구 질문** — 설정 화면은 PIN으로 보호되며, 분실 시 이메일 없이 온디바이스 보안 질문으로만 복구 가능. PIN·복구 답변은 PBKDF2(솔트 + 12만 회 반복) 해시로만 저장되고, `EncryptedSharedPreferences`(Android Keystore 기반 AES-256)로 암호화되어 평문은 어디에도 남지 않음
- **마스터 온/오프 스위치 이중 보호** — 켜는 것은 즉시 가능하지만, 끄려면 확인 다이얼로그 → PIN 입력을 모두 통과해야 함 (충동적으로 끄는 것을 방지)
- **앱별 개별 차단 토글** — 설정에서 YouTube/Instagram/TikTok/Facebook/Snapchat 각각 독립적으로 켜고 끌 수 있음
- **온디바이스 이벤트 로그** — 최근 차단/미매칭 기록(패키지명, 매칭된 룰 ID, 시각)만 최대 50건 로컬 저장. 화면 내용이나 개인정보는 절대 저장하지 않음
- **재부팅 후에도 자동 복구** — `BOOT_COMPLETED` 브로드캐스트 수신 시 포그라운드 서비스를 다시 시작해 앱을 직접 열지 않아도 차단이 유지됨

## 동작 원리 (아키텍처)

```
AccessibilityEvent
      │
      ▼
ShortsBlockAccessibilityService  ── BlockingPreferences 확인 (마스터/앱별 on-off)
      │
      ▼
AccessibilityNodeAdapter  (실제 AccessibilityNodeInfo → AccessibleNode 어댑팅)
      │
      ▼
RuleEvaluator  ── RuleRepository가 로드한 block_rules.json 기준으로 판정
      │            (whitelist 우선, viewId/text/contentDescription 매칭, BFS 순회 상한)
      ▼
BlockDecision (Block / Allow / Unmatched)
      │
      ▼
BlockActionExecutor  ── BACK 수행 → 200ms 후에도 차단 화면이면 HOME, 동일 트리거 1초 디바운스
      │
      ▼
BlockOverlayController (짧은 안내 오버레이)  +  BlockEventLogger (로컬 로그 기록)
```

차단 판정은 전부 기기 안에서 이루어지며, 판정 규칙(`assets/block_rules.json`)도 앱에 내장되어 별도 서버로 전송되거나 원격에서 갱신되지 않습니다.

## 기술 스택

| 영역 | 사용 기술 |
|---|---|
| 언어 | Kotlin |
| UI | Jetpack Compose, Material3, Navigation Compose |
| 화면 감지 | Android `AccessibilityService` API |
| 보안 저장 | `androidx.security.crypto` (`EncryptedSharedPreferences`, Android Keystore), PBKDF2WithHmacSHA256 |
| 백그라운드 유지 | Foreground Service(`specialUse`), `BOOT_COMPLETED` `BroadcastReceiver` |
| 데이터 포맷 | JSON (`org.json`) — 룰 정의, 로컬 이벤트 로그 |
| 테스트 | JUnit4 (순수 Kotlin 룰 엔진 단위 테스트, 프레임워크 의존성 없이 동작) |
| 빌드 | Gradle (Kotlin DSL), AGP 8.5.2 |

## 프로젝트 구조

```
app/src/main/java/com/shortsblocker/app/
├── rules/      # 판정 엔진 — AccessibleNode, BlockRule, RuleEvaluator, RuleRepository 등 (순수 Kotlin, 프레임워크 독립적)
├── service/    # AccessibilityService, 포그라운드 서비스, 부팅 리시버, 차단 실행(BACK/HOME)
├── overlay/    # 차단 시 보여주는 안내 오버레이
├── logging/    # 온디바이스 전용 이벤트 로그 (링 버퍼, 최대 50건)
├── security/   # PIN·복구 질문 저장 및 검증 (PBKDF2 해시 + EncryptedSharedPreferences)
├── prefs/      # 마스터/앱별 on-off 상태
└── ui/         # Compose 화면 (온보딩, PIN 설정/입력/복구, 홈, 설정)

app/src/main/assets/block_rules.json   # 차단 룰 정의 (내장, 원격 갱신 없음)
app/src/test/                          # RuleEvaluator 단위 테스트
docs/                                  # 설계 결정, 기술 명세서, 개인정보 처리방침, Play 스토어 심사 메모
```

## 빌드 및 실행 방법

1. Android Studio에서 프로젝트 루트(`ShortsBlocker/`)를 엽니다.
2. Gradle sync가 끝나면 실제 기기(또는 에뮬레이터, API 26+)를 연결하고 Run 합니다.
3. 최초 실행 시 온보딩 → **접근성 권한** 허용 → **다른 앱 위에 표시** 권한 허용 → PIN·복구 질문 설정 순서로 진행합니다.
4. 단위 테스트만 실행하려면:
   ```bash
   ./gradlew test
   ```

## 알려진 한계

- 기기 설정 > 접근성에서 이 앱의 서비스 자체를 끄는 것은 Android 정책상 어떤 앱도 막을 수 없습니다. 앱 내 PIN은 **이 앱의 설정 화면**만 보호하며, 이 사실은 온보딩 화면에서 사용자에게 그대로 고지합니다. 즉 이 앱은 "하드 락"이 아니라 자기 통제를 돕는 소프트 커밋 도구로 설계되었습니다.
- 각 서비스가 화면 내부 뷰 ID를 바꾸면 해당 룰이 더 이상 매칭되지 않을 수 있습니다 (`Unmatched`로 로컬 로그에 기록되어 사용자가 직접 확인 가능). 이 경우 `block_rules.json`을 수정하고 앱을 재빌드해야 합니다.
- Facebook 차단 규칙은 뷰 ID 구조상 감지 정확도가 상대적으로 낮을 수 있습니다 (설정 화면에 고지됨).

더 자세한 기능 명세와 테스트 케이스는 [`docs/TECHNICAL_SPEC.md`](docs/TECHNICAL_SPEC.md)를 참고하세요.
