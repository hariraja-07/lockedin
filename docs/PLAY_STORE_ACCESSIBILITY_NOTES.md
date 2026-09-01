# Play 스토어 접근성 권한 심사 주의사항

접근성 API를 화면 판독 목적(스크린 리더 등)이 아닌 용도로 쓰는 앱은 Google Play의 **제한된 사용 요건(Restricted Use)** 정책 대상이며, 게시 전 별도 선언 및 심사를 통과해야 한다.

## 제출 시 준비할 것
1. **Play Console > 앱 콘텐츠 > 접근성 권한** 설문에서 이 앱의 용도를 "콘텐츠 차단/디지털 웰빙"으로 명확히 기술.
2. **데모 영상**: 접근성 서비스를 켜는 과정부터 Shorts/Reels/TikTok이 실제로 차단되는 장면까지 화면 녹화. 심사자가 접근성 권한 없이는 핵심 기능이 동작하지 않음을 확인할 수 있어야 함.
3. **왜 다른 API로는 안 되는지 설명.** UsageStatsManager나 DeviceAdmin으로는 앱 *내부* 화면(Shorts vs 롱폼)을 구분할 수 없고, 접근성 API의 노드 트리 검사만이 이를 가능하게 함을 서술.
4. **개인정보처리방침 URL** 필수 제출 — 이 저장소의 [PRIVACY_POLICY.md](./PRIVACY_POLICY.md) 내용을 웹에 게시하고 링크.
5. 앱 내 접근성 권한 요청 화면에 **왜 필요한지 사용자에게 먼저 설명하는 화면**(런타임 권한 요청 전 사전 고지)이 있어야 함 — 이 앱은 `OnboardingScreen`이 이 역할을 함.

## 거절되기 쉬운 지점
- **너무 광범위한 패키지 대상.** `accessibility_service_config.xml`의 `packageNames`를 필요한 앱(YouTube/Instagram/TikTok/브라우저)으로만 제한할 것 — 전체 앱 대상으로 설정하면 반려 가능성이 높음. 이 프로젝트는 이미 제한되어 있음 ([accessibility_service_config.xml](../app/src/main/res/xml/accessibility_service_config.xml)).
- **화면 콘텐츠 전송/저장 의혹.** 코드에 네트워크 권한(`INTERNET`)이 전혀 없고, 로그(`BlockEventLogger`)에는 패키지명과 룰 ID만 남긴다는 점을 심사 노트에 강조.
- **접근성 권한과 무관한 기능 끼워팔기.** 이 서비스가 실제로 차단 기능에만 쓰이고 광고/분석 SDK 등과 결합되어 있지 않아야 함.

## 사이드로딩 관련 별도 안내
Play 스토어 밖에서 APK를 직접 설치한 경우, Android 13+는 접근성 서비스 활성화 시 "제한된 설정" 해제 절차(설정 > 앱 > 이 앱 > 제한된 설정 허용)를 요구한다. 이는 Play 심사와 무관하게 사용자에게 온보딩에서 안내해야 하는 별개의 OS 동작이다.
