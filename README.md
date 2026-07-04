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

## 실행 방법

1. Android Studio (Koala 이상 권장)로 프로젝트 루트를 연다.
2. Gradle sync 후 실제 기기 또는 에뮬레이터(문자 수신 테스트는 실기기 필요)에서 실행한다.
3. 최초 실행 시 SMS 권한을 요청한다. 허용해야 자동 수집이 동작한다.
4. 우측 상단 새로고침 아이콘으로 기존 문자함을 스캔해 과거 승인 문자를 한 번에 가져올 수 있다.
5. `+` 버튼으로 보유 카드의 이용조건(카드사, 월 실적 금액, 산정 기간, 할부 제외 여부)을 등록한다.

## ⚠️ 알려진 제약 / 검증되지 않은 부분

- **이 개발 환경에는 Android SDK가 설치되어 있지 않아 `./gradlew assembleDebug` 등 실제 빌드/실행 검증을 하지 못했습니다.**
  Android Studio(또는 Android SDK가 설치된 CI)에서 첫 빌드 시 사소한 컴파일 오류가 나올 수 있습니다.
- `gradle/wrapper` 바이너리(`gradle-wrapper.jar`)는 포함하지 않았습니다. Android Studio로 열면 자동 생성되거나,
  직접 `gradle wrapper --gradle-version 8.7`을 실행해 생성하세요.
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
