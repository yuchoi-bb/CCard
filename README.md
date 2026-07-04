# CCard — 카드 실적 체크 앱

카드사에서 오는 승인 문자(SMS)를 읽어 이번 달(또는 전월) 카드 이용조건(예: 전월실적 30만원 이상)을
충족했는지 자동으로 계산해주는 Android 앱 MVP입니다.

## 구성

| 계층 | 파일 | 역할 |
|---|---|---|
| 데이터 | `data/Transaction.kt`, `data/CardCondition.kt`, `data/AppDatabase.kt` | Room 기반 저장소. 거래 내역과 카드별 이용조건을 저장 |
| 파서 | `parser/CardSmsParser.kt` | SMS 본문에서 카드사, 금액, 가맹점, 일시, 취소/할부 여부 추출 |
| 수집 | `sms/SmsReceiver.kt` | 실시간 SMS 수신 시 자동 파싱·저장 (`BroadcastReceiver`) |
| 수집 | `sms/SmsImporter.kt` | 설치 이전 문자함(Inbox)을 스캔해 과거 내역 백필 |
| 도메인 | `domain/MonthlyPerformanceCalculator.kt` | 조건의 실적 산정 기간(당월/전월)에 맞춰 누적 사용액·충족 여부 계산 |
| UI | `MainActivity.kt`, `ui/*` | 권한 요청, 카드별 실적 현황 화면, 조건 추가 다이얼로그 |
| 업데이트 | `update/UpdateChecker.kt`, `update/UpdateManager.kt` | GitHub Releases에서 최신 버전 확인 → 앱 내에서 다운로드·설치 |

## 실행 방법

1. Android Studio (Koala 이상 권장)로 프로젝트 루트를 연다.
2. Gradle sync 후 실제 기기 또는 에뮬레이터(문자 수신 테스트는 실기기 필요)에서 실행한다.
3. 최초 실행 시 SMS 권한을 요청한다. 허용해야 자동 수집이 동작한다.
4. 우측 상단 새로고침 아이콘으로 기존 문자함을 스캔해 과거 승인 문자를 한 번에 가져올 수 있다.
5. `+` 버튼으로 보유 카드의 이용조건(카드사, 월 실적 금액, 산정 기간, 할부 제외 여부)을 등록한다.

## 빌드 & 배포 (GitHub Actions)

`.github/workflows/release.yml`이 `claude/credit-card-tracker-sr7ybx`/`main` 브랜치에 푸시될 때마다
(또는 Actions 탭에서 수동으로) 자동으로:

1. `assembleRelease`로 APK를 빌드하고 (`versionCode`/`versionName`은 실행 번호 기반으로 자동 증가)
2. 태그 `v<run_number>`로 GitHub Release를 생성해 APK를 첨부한다.

빌드된 APK는 저장소의 **Releases** 페이지에서 바로 다운로드할 수 있는 링크가 생긴다.

### 서명 키에 대해

모든 릴리즈는 저장소 루트에 커밋된 `ccard-release.keystore`로 서명된다. 이렇게 고정해 둔 이유는 Android가
서명이 다른 APK는 "업데이트"가 아니라 별도 앱으로 취급해 기존 앱 삭제 없이는 설치를 거부하기 때문이다 —
이 키가 고정되어야 아래 앱 내 자동 업데이트가 매번 끊김 없이 동작한다. **사이드로드 테스트 전용 개인 키이며
플레이스토어 배포용이 아니다.** 프로덕션으로 전환한다면 이 키를 저장소에서 빼고 GitHub Actions secret으로
옮기는 것을 권장한다.

## 앱 내 자동 업데이트

앱 실행 시 `UpdateChecker`가 GitHub Releases의 `latest`를 조회해 현재 설치된 `versionCode`보다 새 버전이
있으면 다이얼로그를 띄운다. "업데이트"를 누르면:

1. (최초 1회) "출처를 알 수 없는 앱 설치" 권한 화면으로 이동해 허용
2. `DownloadManager`로 APK를 앱 전용 저장소에 다운로드
3. 다운로드 완료 시 시스템 설치 화면을 자동으로 띄움 (`FileProvider` 경유)

즉, 첫 설치 이후로는 GitHub에서 새 릴리즈가 나올 때마다 앱을 다시 사이드로드할 필요 없이 앱 안에서 바로
업데이트할 수 있다.

## ⚠️ 알려진 제약 / 검증되지 않은 부분

- **이 개발 환경에는 Android SDK가 설치되어 있지 않아 로컬에서 `./gradlew assembleRelease` 실제 빌드 검증을
  하지 못했습니다.** GitHub Actions는 Android SDK를 갖추고 있어 정상적으로 빌드될 것으로 기대하지만, 실제 CI
  결과(Actions 탭)로 확인이 필요합니다.
- **SMS 파싱 정규식은 추정치입니다.** 카드사(신한/삼성/KB국민/현대/롯데/우리/NH농협/하나/BC)마다, 그리고 같은
  카드사라도 시기별로 문자 포맷이 다릅니다. 실제 수신 문자 몇 건을 `CardSmsParser`의 정규식과 대조해
  `companyKeywords`, `amountRegex`, `dateTimeRegex`, `extractMerchant` 로직을 보정해야 합니다.
- 카드 뒷 4자리(`cardLast4`) 매칭은 문자에 `(1234)` 형태로 카드번호가 포함된 경우에만 동작합니다. 카드가 1장뿐이면
  `cardLast4`를 비워둬도 카드사만으로 구분됩니다.
- "실적 제외 항목"은 현재 할부 여부만 지원합니다. 세금/공과금/상품권 등 카드사별 세부 제외 항목은 반영되어
  있지 않으니 필요 시 `MonthlyPerformanceCalculator`에 필터를 추가해야 합니다.
- Android 8.1+ 등 일부 버전에서는 문자 앱(기본 SMS 앱)이 아니면 `SMS_RECEIVED` 브로드캐스트를 못 받을 수
  있습니다(제조사/OS 정책에 따라 다름). 수신이 안 되면 문자함 스캔(수동 새로고침) 기능으로 우회할 수 있습니다.

## 다음 단계 제안

1. 실제 기기에서 카드사별 문자 샘플을 수집해 파서 정확도 검증
2. 조건 미충족 시 월말 알림(WorkManager 기반 알림) 추가
3. 카드 조건 삭제/수정 UI 보강
4. 다중 카드·다중 계좌 실적 합산 조건(예: "OO페이 포함 30만원") 지원
