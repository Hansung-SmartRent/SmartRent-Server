# API 명세

앱과 서버가 **어떤 주소로, 무엇을 보내고, 무엇을 돌려받는지**에 대한 약속입니다. 두 파일로 나눕니다.

| 파일 | 무엇의 기준인가 |
|---|---|
| [openapi.yaml](openapi.yaml) | API 하나하나의 **주소, 보내는 칸, 돌려받는 칸, 자료형, 오류**. 서버를 켜면 Swagger 화면으로 볼 수 있고, 프론트와 백엔드가 이 파일 하나로 맞춥니다. |
| README.md (이 파일) | 모든 API에 공통인 **규칙**(주소, 날짜, 오류, 권한), 전체 API 목록과 담당, API별 **업무 규칙과 확인 사례**, 명세를 바꾸는 절차 |

- 칸 이름이나 자료형은 openapi.yaml에만 적고 여기서는 다시 쓰지 않습니다. 둘이 다르면 openapi.yaml이 맞고, 이 파일을 고칩니다.
- 대여 상태가 바뀌는 규칙은 [DB 설계 7절](../db/README.md#7-대여-상태와-전이-이-절이-기준)이 기준입니다. 여기서는 어떤 API가 그 전이를 일으키는지만 적습니다.
- **확정**은 기획안·팀 결정에 근거가 있는 규칙, **추천**은 이 문서가 제안하는 값입니다. 추천을 바꾸려면 팀과 정한 뒤 이 파일과 openapi.yaml을 함께 고칩니다.

## 1. 공통 규칙

각 규칙을 **뜻 → 우리 규칙 → 이유 → 예시** 순서로 적습니다. "표준"은 널리 쓰는 방식, "팀 선택"은 우리가 고른 방식입니다.

### 1-1. 주소와 HTTP 방법

- **뜻:** 주소는 무엇을 다루는지(명사), HTTP 방법은 무엇을 하는지(동사)를 나타냅니다.
- **우리 규칙:**
  - 모든 주소는 `/api/v1`로 시작합니다(확정, 옛 README).
  - 명사는 복수형 소문자, 단어 사이는 `-`: `/purchase-requests` (표준)
  - 조회 `GET`, 만들기 `POST`, 통째로 바꾸기 `PUT`, 일부 바꾸기 `PATCH`, 지우기 `DELETE` (표준)
  - 상태를 바꾸는 동작은 `POST /rentals/{id}/cancel`처럼 동사를 주소 끝에 붙입니다(팀 선택, 확정).
  - 관리자 전용은 `/admin`으로 시작합니다(팀 선택, 확정).
- **이유:** 수령·반납·연장처럼 "상태를 바꾸는 동작"이 많아서, `PATCH /rentals/{id}` 하나로 몰면 무엇을 하는 요청인지 알아보기 어렵습니다. `/admin`을 앞에 두면 보안 설정에서 관리자 권한을 한 줄로 걸 수 있습니다.
- **예시:** 예약 취소 `POST /api/v1/rentals/120/cancel`, 관리자 수령 처리 `POST /api/v1/admin/rentals/120/pickup`

### 1-2. 요청과 응답 형식

- **뜻:** 요청 본문과 응답을 어떤 모양으로 주고받는지입니다.
- **우리 규칙:**
  - 본문은 JSON(`Content-Type: application/json`), 사진 올리기만 `multipart/form-data` (표준)
  - 칸 이름은 camelCase: `startAt`, `studentNumber` (팀 선택)
  - **모든 응답은 같은 틀로 감쌉니다**(확정, 옛 README): `{ "success": true, "message": "...", "data": { }, "errorCode": null }`
  - 돌려줄 본문이 없으면 `data: null`
- **이유:** 앱이 응답을 한 가지 방식으로 읽을 수 있습니다.
- **예시:**

```json
{ "success": true, "message": "예약되었습니다.", "data": { "applicationId": 77, "rentals": [ { "id": 120, "status": "RESERVED" } ] }, "errorCode": null }
```

### 1-3. 날짜·시각·식별자

- **우리 규칙(확정):**
  - 시각은 ISO 8601에 **+09:00을 붙여** 씁니다: `2026-10-07T10:00:00+09:00`
  - 날짜만은 `2026-10-09`, 시각만은 `09:00`
  - id는 숫자(`120`). 기기 코드(QR·바코드 값)는 글자(`"278"`)
  - 금액은 다루지 않습니다(연체료 없음, 기획안 7장)
- **이유:** 운영일·주말·다음 주 금요일이 모두 한국 날짜 기준이라, 시간대를 빼먹으면 자정 근처에서 날짜가 하루 어긋납니다. 기기 코드는 앞에 0이 붙거나 글자가 섞일 수 있어 숫자로 두지 않습니다.
- **예시:** `"endAt": "2026-10-07T12:00:00+09:00"`

### 1-4. 목록·검색·정렬·쪽 나누기

- **우리 규칙(확정):**
  - 관리자 목록(대여 현황, 학생, 문의, 관리 기록, 알림함)만 쪽을 나눕니다: `?page=0&size=20`, `size`는 최대 100
  - 응답은 `data: { "items": [...], "page": { "page": 0, "size": 20, "totalElements": 57, "totalPages": 3 } }`
  - 기자재 목록(43종), 내 대여, 휴무일처럼 작은 목록은 쪽을 나누지 않고 배열로 줍니다
  - 검색어는 `q`, 거르기는 칸 이름 그대로(`status=RENTING`)
  - 정렬은 API마다 정해진 순서(최신순, 답변 대기 먼저 등)로 고정합니다
- **이유:** 기자재가 43종뿐이라 쪽을 나누면 오히려 앱이 복잡해집니다. 기록이 계속 쌓이는 목록만 나눕니다.

### 1-5. 인증과 권한

- **우리 규칙:**
  - 로그인하면 JWT 액세스 토큰과 리프레시 토큰을 줍니다(확정: Spring Security + JWT, 옛 README). 이후 요청은 `Authorization: Bearer <액세스 토큰>`
  - 액세스 토큰 30분, 리프레시 토큰 14일(확정)
  - 리프레시 토큰은 DB `refresh_tokens`에 해시로 저장하고, 로그아웃·비밀번호 변경·탈퇴 때 지웁니다. 로그아웃은 그 휴대폰의 푸시 토큰도 지웁니다(`POST /auth/logout`).
  - 권한은 세 가지입니다
    - 없음: 가입·로그인·인증번호·비밀번호 찾기
    - 학생(`STUDENT`): 자기 정보와 대여만
    - 관리자(`ADMIN`): `/admin` 전부, 그리고 학생 API 중 조회
  - **승인 전·반려·정지 학생도 로그인과 조회는 됩니다.** 예약·연장만 막습니다(확정, 기획안 1·7장)
  - 남의 대여를 보려 하면 `403 FORBIDDEN`
- **이유:** 기획안 1장 "승인 전 계정은 조회만 되고 예약과 대여는 되지 않는다"를 지키려면 로그인은 열어 두고 동작 단위로 막아야 합니다.

### 1-6. 오류 형식과 상태 코드

- **우리 규칙:**
  - 오류도 같은 틀입니다: `{ "success": false, "message": "사람이 읽는 문장", "data": null, "errorCode": "SAME_KIND_ACTIVE" }`
  - `errorCode`는 대문자와 밑줄(확정, 옛 README). 앱은 `message`를 그대로 보여 주고, 버튼 처리 등은 `errorCode`로 판단합니다
  - HTTP 상태 코드(확정):

| 코드 | 언제 |
|---|---|
| 400 | 입력이 잘못됨(형식, 없는 칸, 규칙에 맞지 않는 시간) |
| 401 | 로그인 안 됨, 토큰 만료, 비밀번호 틀림 |
| 403 | 권한 없음, 승인 전, 정지 |
| 404 | 대상 없음 |
| 409 | 지금 상태에서 할 수 없음(수량 없음, 같은 종류 진행 중, 동시 수정 충돌) |
| 429 | 로그인 5번 실패로 잠김 |
| 500 | 서버가 예상하지 못한 오류(코드 오류, DB 연결 끊김 등) |
| 503 | 외부 서비스(AI) 실패 |

- **이유:** 기획안 3장 "예약이 실패하면 사유를 에러 코드로 구분해 화면에 표시"를 지키려고 실패마다 코드를 따로 둡니다.
- **예시:** 같은 모델을 이미 예약한 학생이 또 예약하면 `409`와 `SAME_KIND_ACTIVE`

#### 오류 코드 전체

| 오류 코드 | HTTP | 기본 메시지 | 언제 |
|---|---|---|---|
| `VALIDATION_ERROR` | 400 | 입력값을 확인해 주세요. | 필수값 누락, 형식 오류. data에 칸별 오류 목록 |
| `UNAUTHORIZED` | 401 | 로그인이 필요합니다. | 토큰 없음·만료·위조 |
| `FORBIDDEN` | 403 | 권한이 없습니다. | 학생이 관리자 API 호출, 남의 대여에 접근 |
| `NOT_FOUND` | 404 | 대상을 찾을 수 없습니다. | 없는 id·코드 |
| `CONFLICT_RETRY` | 409 | 다른 요청과 겹쳤습니다. 다시 시도해 주세요. | 같은 기록을 동시에 고쳐 @Version 충돌 |
| `INTERNAL_ERROR` | 500 | 일시적인 오류가 발생했습니다. 잠시 뒤 다시 시도해 주세요. | 위 코드로 처리하지 못한 예외(코드 오류, DB 연결 끊김 등). 모든 API에서 날 수 있음. 원인은 서버 로그에만 남기고 응답에는 넣지 않음 |
| `EMAIL_DOMAIN_NOT_ALLOWED` | 400 | 학교 이메일만 사용할 수 있습니다. | @ 뒤가 hansung.ac.kr·hansung.kr·hansung.edu와 정확히 같지 않음(기획안 1장) |
| `EMAIL_ALREADY_USED` | 409 | 이미 가입된 이메일입니다. | 가입·이메일 변경 |
| `OTP_INVALID` | 400 | 인증 코드가 올바르지 않습니다. | 번호 불일치, 이미 사용, 다른 이메일 |
| `OTP_EXPIRED` | 400 | 인증 코드가 만료되었습니다. 코드를 다시 받아 주세요. | 5분 지남(기획안 1장) |
| `INVALID_CREDENTIALS` | 401 | 이메일과 비밀번호를 확인해 주세요. | 로그인 실패 |
| `LOGIN_LOCKED` | 429 | 5분 동안 로그인이 제한됩니다. | 5번 연속 실패(앱 기능) |
| `STUDENT_NUMBER_DUPLICATE` | 409 | 이미 등록된 학번입니다. | 같은 학번 두 계정 금지(기획안 1장) |
| `APPROVAL_NOT_PENDING` | 409 | 승인 대기 중인 계정이 아닙니다. | 이미 승인·반려한 학생을 다시 처리 |
| `WITHDRAW_BLOCKED_HELD` | 409 | 보유 중인 기기를 먼저 반납해 주세요. | 대여 중 탈퇴(기획안 1장) |
| `NOT_APPROVED` | 403 | 승인된 학생 계정에서 예약할 수 있습니다. | 승인 전 예약·대여(기획안 1장) |
| `SUSPENDED` | 403 | 이용 정지 중입니다. | 정지 중 예약·현장 대여·연장(기획안 7장) |
| `LATE_RENTAL_EXISTS` | 409 | 연체 중인 기자재를 먼저 반납해 주세요. | 연체 중 새 예약·현장 대여(기획안 3·5장) |
| `SAME_KIND_ACTIVE` | 409 | 같은 종류의 진행 중인 신청은 하나만 가능합니다. | 같은 모델 또는 같은 이름 모델에 진행 중 신청(기획안 3장) |
| `MODEL_ARCHIVED` | 409 | 운영이 종료된 모델입니다. |  |
| `RENTAL_TOO_LONG` | 400 | 이 기자재는 한 번에 빌릴 수 있는 시간을 넘었습니다. | 품목의 한 번 대여 최대 시간(`maxRentalMinutes`)을 넘는 신청(기획안 3장, 지금은 레이저 커팅기 2시간) |
| `SEGMENT_INVALID` | 400 | 시작과 종료 시간을 확인해 주세요. | 시작 ≥ 종료, 칸 없음 |
| `SEGMENTS_OVERLAP` | 400 | 예약 구간이 서로 겹칩니다. |  |
| `OUT_OF_BOOKING_WINDOW` | 400 | 예약 가능 기간을 확인해 주세요. | 이미 시작한 칸, 다음 주 금요일 이후(장기는 1년 이후, maxRentalDays가 있으면 시작일 + 그 일수 이후)(기획안 3·4장) |
| `CLOSED_DAY` | 400 | 운영하지 않는 날입니다. | 주말·공휴일·임시 휴무에 시작(기획안 8장) |
| `RETURN_ON_CLOSED_DAY` | 400 | 반납일이 휴무일입니다. 다른 날을 선택해 주세요. | 2일 이상 대여(기획안 8장) |
| `SLOT_INVALID` | 400 | 운영 시간 안의 시간 칸을 선택해 주세요. | 칸 경계와 맞지 않음, 운영 시간 밖 |
| `SAME_DAY_ONLY` | 400 | 이 모델은 당일 대여만 가능합니다. | 2일 이상 대여가 아닌 모델 |
| `LONG_TERM_DATE_INVALID` | 400 | 장기 대여는 수령일만 고르고 반납은 6개월 뒤 운영일까지입니다. | 기획안 4장 |
| `NO_CAPACITY` | 409 | 선택한 시간의 남은 수량이 없습니다. | 대여 가능 대수 0(1시간 여유 포함) |
| `NOT_CANCELLABLE` | 409 | 시작 전 예약만 취소할 수 있습니다. | 기획안 3·5장 |
| `INVALID_STATUS` | 409 | 지금 상태에서는 처리할 수 없습니다. | 수령·반납·정정을 맞지 않는 상태에서 요청 |
| `OUTSIDE_OPERATING_HOURS` | 409 | 운영 시간에만 처리할 수 있습니다. | 운영 시간 밖 수령·현장 대여(기획안 5장) |
| `PROFESSOR_MAIL_REQUIRED` | 400 | 지도교수 승인 메일을 현장에서 확인해 주세요. | 장기 대여 수령(기획안 4장) |
| `UNIT_MISMATCH` | 400 | 예약한 모델의 정상 기기인지 확인해 주세요. | 다른 모델, 정상 아님(기획안 5장) |
| `UNIT_IN_USE` | 409 | 이미 대여 중인 기기입니다. |  |
| `NO_EARLY_CAPACITY` | 409 | 조기 수령 가능한 수량이 없습니다. | 시작 전 수령인데 그 사이 남는 기기 없음(기획안 5장) |
| `RESERVATION_ENDED` | 409 | 예약 시간이 종료되었습니다. |  |
| `NO_SHOW_DEADLINE_PASSED` | 409 | 수령 마감이 지나 노쇼로 처리되었습니다. | 일반·2일 이상 대여는 예약 시작 10분, 장기는 수령일 운영 종료에 도달한 수령 요청. 판정 작업이 아직 돌지 않았어도 이 요청에서 노쇼로 처리하고 거절(DB 설계 7절) |
| `WALK_IN_END_INVALID` | 400 | 이 반납 기한으로는 빌려줄 수 없습니다. | 선택지에 없는 반납 기한(기획안 5장) |
| `EXTENSION_NOT_ALLOWED` | 409 | 현재 연장할 수 없습니다. | 이미 연장, 기한 지남, 즉시 반납 요망, 정지(기획안 6장) |
| `LONG_TERM_EXTENSION_BLOCKED` | 409 | 장기 대여는 창구에 지도교수 승인 메일을 가져와 연장해 주세요. | 기획안 4장 |
| `EXTENSION_LIMIT` | 400 | 연장 가능 기한을 초과했습니다. | 운영 종료, 뒤 예약 1시간 전, 다음 주 금요일, 휴무일(기획안 6장) |
| `UNIT_DELETE_BLOCKED` | 409 | 대여 중인 기기는 삭제할 수 없습니다. | 기획안 2장 |
| `UNIT_CODE_DUPLICATE` | 409 | 이미 쓰는 기기 코드입니다. |  |
| `MODEL_ARCHIVE_BLOCKED` | 409 | 대여 중인 기기가 있어 운영을 종료할 수 없습니다. | 대여 중(RENTING) 기록이 있는 모델의 운영 종료(기획안 2장) |
| `MODEL_NAME_DUPLICATE` | 409 | 같은 이름의 모델이 이미 있습니다. | 운영 중인 모델끼리 이름이 같음(대소문자·공백 무시). 사진 인식이 모델 하나를 찾게 하기 위함(DB 설계 3절) |
| `WARNING_NOT_ACTIVE` | 409 | 유효한 경고를 찾을 수 없습니다. | 이미 취소된 경고 |
| `AI_UNAVAILABLE` | 503 | AI 추천을 잠시 사용할 수 없습니다. | Gemini API 실패·시간 초과(docs/외부연결.md) |
| `FILE_INVALID` | 400 | jpg 또는 png 사진을 올려 주세요. | 형식·크기 |
| `MAIL_UNAVAILABLE` | 503 | 메일을 보내지 못했습니다. 잠시 뒤 다시 시도해 주세요. | SMTP 실패(docs/외부연결.md). 보낸 인증번호는 무효 |
| `STORAGE_UNAVAILABLE` | 503 | 사진을 저장하지 못했습니다. 잠시 뒤 다시 시도해 주세요. | S3 실패(docs/외부연결.md) |

### 1-7. 입력 검증

- **우리 규칙:** 칸 형식(빈칸, 길이, 이메일 모양)은 Bean Validation(`@NotBlank`, `@Size`, `@Email`)으로 막고 `400 VALIDATION_ERROR`와 칸별 오류 목록을 줍니다. 업무 규칙(수량, 운영 시간, 같은 종류)은 서비스 코드에서 검사하고 전용 오류 코드를 줍니다(확정: Bean Validation, 옛 README).
- **예시:**

```json
{ "success": false, "message": "입력값을 확인해 주세요.", "data": [ { "field": "purpose", "reason": "200자 이하로 입력해 주세요." } ], "errorCode": "VALIDATION_ERROR" }
```

### 1-8. 동시 요청과 중복 요청

- **우리 규칙:**
  - 예약, 수령, 현장 대여, 연장은 **그 모델 행을 잠근 뒤** 남은 대수를 다시 셉니다(확정, 기획안 3장 "요청을 처리하는 순간에만 잠근다", 방법은 [DB 설계 6절](../db/README.md#동시-요청)).
  - 두 관리자가 같은 대여를 동시에 처리하면 나중 요청은 `409 CONFLICT_RETRY`(확정, JPA `@Version`).
  - 학생이 예약 버튼을 두 번 눌러도 두 번째는 "같은 종류 한 건" 규칙에 걸려 `SAME_KIND_ACTIVE`로 끝납니다. 따로 중복 방지 키를 두지 않습니다(확정).
- **이유:** 시간 칸을 미리 붙잡아 두는 방식은 기획안 12장에서 뺐습니다.

## 2. 전체 API 목록과 담당

담당 나눔은 옛 README의 역할표를 따릅니다. 운영 시간·휴무 API는 백엔드 1이 만들고, 그 변경으로 걸리는 예약 정리는 백엔드 2가 만든 서비스를 호출합니다(연결과 확인 책임은 이슈 [B2-16](../issues/B2-16.md)). 이슈 번호는 [작업 목록](../issues/README.md)입니다.

| 묶음 | 방법 | 주소 | 하는 일 | 권한 | 담당 | 이슈 |
|---|---|---|---|---|---|---|
| 인증 | POST | `/auth/email-codes` | 인증번호 보내기 | 없음 | 백엔드 1 | B1-05 |
| 인증 | POST | `/auth/signup` | 회원가입 | 없음 | 백엔드 1 | B1-05 |
| 인증 | POST | `/auth/login` | 로그인 | 없음 | 백엔드 1 | B1-04 |
| 인증 | POST | `/auth/token/refresh` | 토큰 새로 받기 | 없음 | 백엔드 1 | B1-04 |
| 인증 | POST | `/auth/logout` | 로그아웃 | 로그인한 사용자 | 백엔드 1 | B1-04 |
| 인증 | POST | `/auth/password/reset` | 비밀번호 찾기 | 없음 | 백엔드 1 | B1-05 |
| 내 정보 | GET | `/users/me` | 내 정보 | 로그인한 사용자 | 백엔드 1 | B1-06 |
| 내 정보 | POST | `/users/me/student-id` | 학생증 제출·재제출 | 학생 | 백엔드 1 | B1-07 |
| 내 정보 | PUT | `/users/me/password` | 비밀번호 변경 | 로그인한 사용자 | 백엔드 1 | B1-06 |
| 내 정보 | PUT | `/users/me/email` | 이메일 변경 | 로그인한 사용자 | 백엔드 1 | B1-06 |
| 내 정보 | PUT | `/users/me/profile-image` | 프로필 사진 바꾸기 | 로그인한 사용자 | 백엔드 1 | B1-08 |
| 내 정보 | DELETE | `/users/me/profile-image` | 프로필 사진 지우기 | 로그인한 사용자 | 백엔드 1 | B1-08 |
| 내 정보 | DELETE | `/users/me` | 탈퇴 | 학생 | 백엔드 1 | B1-09 |
| 내 정보 | GET | `/users/me/warnings` | 내 경고 내역 | 학생 | 백엔드 2 | B2-12 |
| 내 정보 | GET | `/users/me/notification-settings` | 알림 설정 보기 | 로그인한 사용자 | 백엔드 2 | B2-14 |
| 내 정보 | PUT | `/users/me/notification-settings` | 알림 설정 바꾸기 | 로그인한 사용자 | 백엔드 2 | B2-14 |
| 내 정보 | PUT | `/users/me/push-token` | 푸시 토큰 등록 | 로그인한 사용자 | 백엔드 2 | B2-13 |
| 기자재 | GET | `/models` | 기자재 목록·검색 | 로그인한 사용자 | 백엔드 1 | B1-10 |
| 기자재 | GET | `/models/{id}` | 기자재 상세 | 로그인한 사용자 | 백엔드 1 | B1-10 |
| 기자재 | GET | `/units/by-code/{code}` | 코드로 기자재 찾기 | 로그인한 사용자 | 백엔드 1 | B1-11 |
| 기자재 | POST | `/models/recognition` | 사진 인식 결과로 찾기 | 로그인한 사용자 | 백엔드 1 | B1-12 |
| 기자재 | GET | `/models/{id}/availability` | 예약 가능한 칸 | 로그인한 사용자 | 백엔드 2 | B2-02 |
| 대여 | POST | `/reservations` | 예약하기 | 학생 | 백엔드 2 | B2-03 |
| 대여 | GET | `/rentals/me` | 내 대여 목록 | 학생 | 백엔드 2 | B2-04 |
| 대여 | GET | `/rentals/{id}` | 대여 상세 | 로그인한 사용자 | 백엔드 2 | B2-04 |
| 대여 | POST | `/rentals/{id}/cancel` | 예약 취소 | 학생 | 백엔드 2 | B2-05 |
| 대여 | GET | `/rentals/{id}/extension-options` | 연장 선택지 | 학생 | 백엔드 2 | B2-09 |
| 대여 | POST | `/rentals/{id}/extend` | 연장하기 | 학생 | 백엔드 2 | B2-09 |
| 대여 | POST | `/rentals/{id}/damage-report` | 기기 이상 신고 | 학생 | 백엔드 1 | B1-13 |
| 관리자 대여 | GET | `/admin/summary` | 관리자 홈 숫자와 우선 처리 | 관리자 | 백엔드 2 | B2-15 |
| 관리자 대여 | GET | `/admin/rentals` | 대여 현황 | 관리자 | 백엔드 2 | B2-15 |
| 관리자 대여 | GET | `/admin/pickups/lookup` | 수령할 예약 찾기 | 관리자 | 백엔드 2 | B2-06 |
| 관리자 대여 | POST | `/admin/rentals/{id}/pickup` | 수령 처리 | 관리자 | 백엔드 2 | B2-06 |
| 관리자 대여 | GET | `/admin/walk-ins/options` | 현장 대여 선택지 | 관리자 | 백엔드 2 | B2-07 |
| 관리자 대여 | POST | `/admin/walk-ins` | 현장 대여 처리 | 관리자 | 백엔드 2 | B2-07 |
| 관리자 대여 | GET | `/admin/returns/lookup` | 반납할 대여 찾기 | 관리자 | 백엔드 2 | B2-08 |
| 관리자 대여 | POST | `/admin/rentals/{id}/return` | 반납 처리 | 관리자 | 백엔드 2 | B2-08 |
| 관리자 대여 | POST | `/admin/rentals/{id}/damage-discovered` | 반납 후 파손 발견 | 관리자 | 백엔드 2 | B2-11 |
| 관리자 대여 | POST | `/admin/rentals/{id}/cancel` | 관리자 예약 취소 | 관리자 | 백엔드 2 | B2-05 |
| 관리자 대여 | POST | `/admin/rentals/{id}/correct-no-show` | 노쇼 정정 | 관리자 | 백엔드 2 | B2-11 |
| 관리자 대여 | POST | `/admin/rentals/{id}/correct-return` | 반납 시각 정정 | 관리자 | 백엔드 2 | B2-11 |
| 관리자 대여 | POST | `/admin/warnings/{id}/revoke` | 경고 취소 | 관리자 | 백엔드 2 | B2-12 |
| 관리자 학생 | GET | `/admin/students` | 학생 목록 | 관리자 | 백엔드 1 | B1-14 |
| 관리자 학생 | GET | `/admin/students/{id}` | 학생 상세 | 관리자 | 백엔드 1 | B1-14 |
| 관리자 학생 | POST | `/admin/students/{id}/approve` | 가입 승인 | 관리자 | 백엔드 1 | B1-15 |
| 관리자 학생 | POST | `/admin/students/{id}/reject` | 가입 반려 | 관리자 | 백엔드 1 | B1-15 |
| 관리자 학생 | PATCH | `/admin/students/{id}` | 학생 이름·학번 수정 | 관리자 | 백엔드 1 | B1-16 |
| 관리자 기자재 | GET | `/admin/models/{id}` | 관리자용 모델 상세 | 관리자 | 백엔드 1 | B1-17 |
| 관리자 기자재 | POST | `/admin/models` | 모델 추가 | 관리자 | 백엔드 1 | B1-17 |
| 관리자 기자재 | PUT | `/admin/models/{id}` | 모델 수정 | 관리자 | 백엔드 1 | B1-17 |
| 관리자 기자재 | POST | `/admin/models/{id}/archive` | 모델 운영 종료 | 관리자 | 백엔드 1 | B1-17 |
| 관리자 기자재 | POST | `/admin/models/{id}/images` | 모델 사진 올리기 | 관리자 | 백엔드 1 | B1-08 |
| 관리자 기자재 | POST | `/admin/models/{id}/units` | 기기 추가 | 관리자 | 백엔드 1 | B1-18 |
| 관리자 기자재 | PATCH | `/admin/units/{id}` | 기기 이름·상태 변경 | 관리자 | 백엔드 1 | B1-18 |
| 관리자 기자재 | DELETE | `/admin/units/{id}` | 기기 삭제 | 관리자 | 백엔드 1 | B1-18 |
| 운영 | GET | `/holidays` | 휴무일 목록 | 로그인한 사용자 | 백엔드 1 | B1-19 |
| 운영 | POST | `/admin/holidays` | 임시 휴무 추가·공휴일 운영 해제 취소 | 관리자 | 백엔드 1 | B1-19 |
| 운영 | POST | `/admin/holidays/{date}/open` | 공휴일에 운영하기 | 관리자 | 백엔드 1 | B1-19 |
| 운영 | DELETE | `/admin/holidays/{date}` | 임시 휴무 지우기 | 관리자 | 백엔드 1 | B1-19 |
| 운영 | GET | `/admin/models/{id}/hours` | 운영 시간 변경 목록 | 관리자 | 백엔드 1 | B1-20 |
| 운영 | PUT | `/admin/models/{id}/hours` | 운영 시간 바꾸기 | 관리자 | 백엔드 1 | B1-20 |
| 운영 | DELETE | `/admin/models/{id}/hours/{date}` | 날짜별 운영 시간 되돌리기 | 관리자 | 백엔드 1 | B1-20 |
| 알림 | GET | `/notifications` | 알림함 | 로그인한 사용자 | 백엔드 2 | B2-13 |
| 알림 | POST | `/notifications/{id}/read` | 알림 읽음 | 로그인한 사용자 | 백엔드 2 | B2-13 |
| 알림 | POST | `/notifications/read-all` | 모두 읽음 | 로그인한 사용자 | 백엔드 2 | B2-13 |
| AI 추천 | POST | `/recommendations` | AI 기자재 추천 | 로그인한 사용자 | 백엔드 1 | B1-21 |
| 공지·문의 | GET | `/notices` | 공지 목록 | 로그인한 사용자 | 백엔드 1 | B1-22 |
| 공지·문의 | POST | `/admin/notices` | 공지 쓰기 | 관리자 | 백엔드 1 | B1-22 |
| 공지·문의 | PUT | `/admin/notices/{id}` | 공지 수정 | 관리자 | 백엔드 1 | B1-22 |
| 공지·문의 | DELETE | `/admin/notices/{id}` | 공지 삭제 | 관리자 | 백엔드 1 | B1-22 |
| 공지·문의 | POST | `/inquiries` | 문의 쓰기 | 학생 | 백엔드 1 | B1-23 |
| 공지·문의 | GET | `/inquiries/me` | 내 문의 | 학생 | 백엔드 1 | B1-23 |
| 공지·문의 | GET | `/admin/inquiries` | 문의 목록 | 관리자 | 백엔드 1 | B1-23 |
| 공지·문의 | POST | `/admin/inquiries/{id}/answer` | 문의 답변 | 관리자 | 백엔드 1 | B1-23 |
| 공지·문의 | POST | `/purchase-requests` | 구매 요청 쓰기 | 학생 | 백엔드 1 | B1-23 |
| 공지·문의 | GET | `/purchase-requests/me` | 내 구매 요청 | 학생 | 백엔드 1 | B1-23 |
| 공지·문의 | GET | `/admin/purchase-requests` | 구매 요청 목록 | 관리자 | 백엔드 1 | B1-23 |
| 공지·문의 | POST | `/admin/purchase-requests/{id}/answer` | 구매 요청 답변 | 관리자 | 백엔드 1 | B1-23 |
| 통계·기록 | GET | `/admin/stats` | 이용 통계 | 관리자 | 백엔드 1 | B1-24 |
| 통계·기록 | GET | `/admin/audit-logs` | 관리 기록 | 관리자 | 백엔드 1 | B1-24 |

### 2-1. 앱 화면별 API

앱(`src/screens/`)의 화면을 읽고 기능마다 쓰는 API와 이슈를 짝지은 표입니다. 서버가 붙으면 앱은 이 API를 부릅니다. 화면이 늘거나 바뀌면 이 표도 고칩니다.

**학생 화면**

| 화면 | 기능 | API | 이슈 |
|---|---|---|---|
| 로그인 | 로그인, 5번 실패 잠금 | `POST /auth/login` | B1-04 |
| (앱 전체) | 토큰 새로 받기 | `POST /auth/token/refresh` | B1-04 |
| (앱 전체) | 푸시 토큰 등록 | `PUT /users/me/push-token` | B2-13 |
| 회원가입 | 인증번호 받기, 가입 | `POST /auth/email-codes`, `POST /auth/signup` | B1-05 |
| 회원가입·학생증 다시 제출 | 학생증 사진 고르기 → 카드 틀에 맞춰 자르기(앱) → 이름·학번·자른 사진 제출 | `POST /users/me/student-id` | B1-07 |
| 비밀번호 찾기 | 인증번호, 새 비밀번호 | `POST /auth/email-codes`, `POST /auth/password/reset` | B1-05 |
| 홈 | 내 상태·반려 사유, 대여 중·예약 달력, 공지, 휴무일 | `GET /users/me`, `GET /rentals/me`, `GET /notices`, `GET /holidays` | B1-06, B2-04, B1-22, B1-19 |
| 기자재 목록 | 카테고리·이름 검색, 지금 대여 가능 | `GET /models` | B1-10 |
| 기자재 목록 | 최근 검색어 5개 | API 없음: 휴대폰에 저장하고 서버에 보내지 않음(팀 정리 문서) | — |
| 기자재 상세 | 상세 정보 | `GET /models/{id}` | B1-10 |
| 대여 신청 | 날짜별 칸과 남은 대수, 휴무일 | `GET /models/{id}/availability`, `GET /holidays` | B2-02, B1-19 |
| 대여 신청 | 예약(일반·2일 이상·장기) | `POST /reservations` | B2-03 |
| 내 대여 | 진행 중·지난 대여 | `GET /rentals/me` | B2-04 |
| 대여 상세 | 상세, 예약 취소 | `GET /rentals/{id}`, `POST /rentals/{id}/cancel` | B2-04, B2-05 |
| 대여 상세 | 연장 선택지, 연장 | `GET /rentals/{id}/extension-options`, `POST /rentals/{id}/extend` | B2-09 |
| 대여 상세 | 기기 이상 신고 | `POST /rentals/{id}/damage-report` | B1-13 |
| 스캔 | QR·바코드로 찾기 | `GET /units/by-code/{code}` | B1-11 |
| 사진 인식 결과 | 인식한 모델명으로 모델 상세 열기 | `POST /models/recognition` → `GET /models/{id}` | B1-12, B1-10 |
| 기자재 찾기 도우미 | AI 추천 | `POST /recommendations` | B1-21 |
| 마이페이지 | 내 정보, 로그아웃, 탈퇴 | `GET /users/me`, `POST /auth/logout`, `DELETE /users/me` | B1-06, B1-04, B1-09 |
| 프로필 | 프로필 사진 바꾸기·지우기 | `PUT /users/me/profile-image`, `DELETE /users/me/profile-image` | B1-08 |
| 비밀번호 변경 | 현재 이메일로 받은 인증번호로 확인 후 변경 | `POST /auth/email-codes`, `PUT /users/me/password` | B1-05, B1-06 |
| 이메일 변경 | 새 학교 메일 인증 후 변경 | `POST /auth/email-codes`, `PUT /users/me/email` | B1-05, B1-06 |
| 경고 내역 | 경고 날짜·사유·기자재, 정지 해제일 | `GET /users/me/warnings` | B2-12 |
| 알림함 | 목록, 읽음, 모두 읽음 | `GET /notifications`, `POST /notifications/{id}/read`, `POST /notifications/read-all` | B2-13 |
| 알림 설정 | 반납 전 알림 끄기 | `GET /users/me/notification-settings`, `PUT /users/me/notification-settings` | B2-14 |
| 문의·구매 요청 | 쓰기, 내 목록과 답변 | `POST /inquiries`, `GET /inquiries/me`, `POST /purchase-requests`, `GET /purchase-requests/me` | B1-23 |

**관리자 화면**

| 화면 | 기능 | API | 이슈 |
|---|---|---|---|
| 관리자 홈 | 숫자와 우선 처리 | `GET /admin/summary` | B2-15 |
| 대여 현황 | 목록·필터, 관리자 예약 취소 | `GET /admin/rentals`, `POST /admin/rentals/{id}/cancel` | B2-15, B2-05 |
| 수령·반납 처리 | 수령할 예약 찾기, 기기 코드 찍고 수령 | `GET /admin/pickups/lookup`, `GET /units/by-code/{code}`, `POST /admin/rentals/{id}/pickup` | B2-06, B1-11 |
| 수령·반납 처리 / 기기 코드로 반납 | 반납할 대여 찾기, 상태 고르고 반납 | `GET /admin/returns/lookup`, `POST /admin/rentals/{id}/return` | B2-08 |
| 현장 대여 | 학번으로 학생 찾기, 기기 찍기, 반납 기한 선택지, 빌려주기 | `GET /admin/students`, `GET /units/by-code/{code}`, `GET /admin/walk-ins/options`, `POST /admin/walk-ins` | B1-14, B1-11, B2-07 |
| 미수령 정정 | 노쇼를 대여 중으로 | `POST /admin/rentals/{id}/correct-no-show` | B2-11 |
| 반납 시간 정정 | 실제 반납 시각 입력 | `POST /admin/rentals/{id}/correct-return` | B2-11 |
| 대여 상세(관리자) | 반납 후 파손 발견 | `POST /admin/rentals/{id}/damage-discovered` | B2-11 |
| 학생 목록 | 상태별 목록, 검색 | `GET /admin/students` | B1-14 |
| 학생 상세 | 학생증 심사(승인·반려), 이름·학번 수정, 경고 취소 | `GET /admin/students/{id}`, `POST /admin/students/{id}/approve`, `POST /admin/students/{id}/reject`, `PATCH /admin/students/{id}`, `POST /admin/warnings/{id}/revoke` | B1-14, B1-15, B1-16, B2-12 |
| 기자재 관리 | 모델 목록·검색 | `GET /models` | B1-10 |
| 모델 추가·수정 | 모델 정보, 사진, 운영 종료 | `GET /admin/models/{id}`, `POST /admin/models`, `PUT /admin/models/{id}`, `POST /admin/models/{id}/images`, `POST /admin/models/{id}/archive` | B1-17, B1-08 |
| 모델 추가·수정 | 기기 추가, 이름·상태 변경, 삭제 | `POST /admin/models/{id}/units`, `PATCH /admin/units/{id}`, `DELETE /admin/units/{id}` | B1-18 |
| 운영 관리 | 휴무일 달력, 임시 휴무 추가·지우기, 공휴일 운영 | `GET /holidays`, `POST /admin/holidays`, `DELETE /admin/holidays/{date}`, `POST /admin/holidays/{date}/open` | B1-19 |
| 모델 운영 시간 | 기본·날짜별 운영 시간 변경, 되돌리기 | `GET /admin/models/{id}/hours`, `PUT /admin/models/{id}/hours`, `DELETE /admin/models/{id}/hours/{date}` | B1-20 |
| 공지 관리 | 목록, 쓰기·수정·삭제 | `GET /notices`, `POST /admin/notices`, `PUT /admin/notices/{id}`, `DELETE /admin/notices/{id}` | B1-22 |
| 문의·구매 요청 | 목록, 답변 | `GET /admin/inquiries`, `POST /admin/inquiries/{id}/answer`, `GET /admin/purchase-requests`, `POST /admin/purchase-requests/{id}/answer` | B1-23 |
| 이용 통계 | 기간별 숫자, 많이 빌린 기자재 | `GET /admin/stats` | B1-24 |
| 관리 기록 | 처리 기록 목록 | `GET /admin/audit-logs` | B1-24 |
| 알림함(관리자) | 파손 신고·문의·운영 문제 알림 | `GET /notifications` 등 | B2-13 |

- 이 표의 API 80개 전부가 위 화면 중 하나 이상에서 쓰입니다. 화면에서 쓰지 않는 API는 없습니다.
- 앱 코드는 화면과 필요한 값을 파악하는 데만 참고했습니다. 요청·응답 칸의 기준은 [openapi.yaml](openapi.yaml)입니다.

## 3. API별 업무 규칙과 확인 사례

규칙 자체는 기획안과 DB 설계에 있고, 여기에는 **API를 만들 때 헷갈리기 쉬운 곳**과 **확인 사례**만 적습니다. 확인 사례의 정확한 입력과 기대 결과는 [fixtures/cases.json](../../fixtures/cases.json)에 있어 자동 시험으로 옮길 수 있습니다.

### 인증·내 정보 (백엔드 1)

- 이메일은 **소문자로 바꾼 뒤 @ 뒤 전체가 정확히 같은지** 비교합니다. `myhansung.ac.kr`, `hansung.ac.kr.example.com`은 거절(기획안 1장). → 사례 `AUTH-01`
- 인증번호는 번호가 맞아도 5분이 지났으면 `OTP_EXPIRED`로 따로 알려 줍니다(앱과 같음). → 사례 `AUTH-02`
- 비밀번호 규칙은 기획안에 없어 팀이 "8자 이상"으로 정했습니다(확정).
- 학생증 사진은 실물 학생증 사진이나 헤이영 전자학생증의 QR 화면 캡처를 받습니다. 관리자는 얼굴·이름·학번만 확인하고 QR은 검사하지 않습니다(기획안 1장).
- 학생은 앱에서 사진을 카드 틀(16:10)에 직접 맞춰 자른 뒤, 자른 사진 한 장만 `POST /users/me/student-id`로 보냅니다. 서버는 사진을 자르거나 내용을 검사하지 않고, 공통 업로드 규칙(jpg·png, 10MB 이하, 어기면 `FILE_INVALID`)만 확인해 저장합니다. 원본은 서버로 오지 않습니다.
- 학생 상세 조회의 `studentIdImageUrl`은 그 사진의 임시 주소이며 관리자만 받습니다.
- 학생증 사진은 승인·반려와 같은 DB 처리에서 삭제 작업에 사진 키를 남긴 뒤 사용자 사진 키를 비우고, 커밋 직후 삭제합니다. 실패해도 승인은 유지하며 B1-08이 저장된 작업으로 재시도합니다(DB 3절 storage_deletion_jobs).
- 탈퇴는 대여 중이면 거부, 아니면 예약 취소 후 삭제, 경고는 6개월 보관. → 사례 `AUTH-03`

### 기자재 (백엔드 1)

- 목록의 `availableNow`는 "지금부터 다음 칸 끝까지 빌릴 수 있는 대수"입니다(확정). 쉬는 날이나 운영 시간 밖이면 0.
- 학생용 상세에는 개별 기기 목록을 넣지 않습니다(앱과 같음). 관리자용 상세에만 넣습니다.
- 사진 인식 API는 휴대폰이 인식한 **모델명**을 받아 모델 **하나**를 돌려줍니다. 대소문자와 공백을 무시하고 이름이 같으면 일치, 운영 종료 모델은 제외. 운영 중인 모델끼리는 이 기준으로 이름이 같을 수 없게 모델 추가·수정에서 막으므로(`MODEL_NAME_DUPLICATE`, [DB 설계 3절 equipment_models](../db/README.md#equipment_models-확정-기획안-234장)) 결과는 하나 이하입니다. 없으면 `404 NOT_FOUND`이고 앱은 "찾지 못했습니다" 안내를 띄웁니다. 앱은 받은 모델 id로 상세 화면을 엽니다. 기기 한 대마다 상세를 두지 않습니다(기획안 10장).

- 기자재 검색은 이름·분류·제조사·활용 목적을 대상으로 하고 목록은 페이지 없이 반환합니다. 목록·상세에 출처를 포함합니다. 대여 상세는 신고 내용·반납 판정·반납 후 파손 시각, 경고 목록은 취소 사유를 포함합니다. 값이 생기기 전에는 null입니다. 정확한 칸 이름은 openapi.yaml 기준입니다.

### 예약 (백엔드 2)

- 칸은 서버가 만듭니다. 운영 시작부터 `interval_minutes`마다, 운영 종료를 넘지 않게(기획안 3장). **이미 시작한 칸은 주지 않고, 그 칸으로 예약하면 `OUT_OF_BOOKING_WINDOW`**(기획안 3장). → 사례 `RSV-01`(30분 간격 3D 프린터)
- 남은 대수는 합계가 아니라 **동시에 겹치는 최대치**로 셉니다. → 사례 `RSV-02`
- 반출 가능 모델은 앞뒤 1시간 여유를 둡니다. → 사례 `RSV-03`
- 떨어진 칸은 한 번에 신청할 수 있고 1건으로 셉니다. 같은 종류 두 번째 신청은 거절. → 사례 `RSV-04`
- 쉬는 날에는 칸이 없고 예약도 거절. → 사례 `RSV-05`
- 장기 대여: 일반 요청의 시간 구간 대신 수령 날짜만 전송합니다. 시작은 해당 날짜 운영 시작으로 저장하고, 오늘 수령은 운영 종료 전까지 신청 가능합니다. 반납 기한은 6개월 뒤 운영 종료, 그날이 쉬는 날이면 그 전 운영일. → 사례 `LT-01`, `LT-02`
- 다음 주 금요일: 월요일 시작 주 기준. 2026-09-26(토)의 다음 주 금요일은 2026-10-02. → 사례 `RSV-06`

### 수령·현장 대여·반납 (백엔드 2)

- 시작 판정이 아직 처리되지 않았으면 수령 요청에서 먼저 반영합니다. 시작 시 내줄 기기가 없어 취소된 예약은 수령할 수 없으며 `409 INVALID_STATUS`로 거절합니다. 노쇼로 처리하지 않습니다.

- 아직 수령 전인 예약의 노쇼 마감 도달 여부를 운영 시간 거절보다 먼저 확인합니다. 장기 수령일 종료 시각과 운영 종료가 같아도 노쇼 처리를 빠뜨리지 않습니다. 예약 시작 시각의 기기 부족 취소를 먼저 처리하고, 그 예약에는 노쇼 경고를 붙이지 않습니다.

- 수령은 운영일 운영 시간 안에만. 주말·공휴일·운영 시간 밖이면 `OUTSIDE_OPERATING_HOURS`. → 사례 `PICK-01`
- 수령 마감(일반·2일 이상 대여는 예약 시작 + 10분, 장기 대여는 수령일 운영 종료 시각)이 지난 수령 요청은 판정 작업을 기다리지 않고 그 자리에서 노쇼로 처리한 뒤 `NO_SHOW_DEADLINE_PASSED`. 학생이 실제로 제시간에 왔었다면 관리자가 노쇼 정정을 씁니다([DB 설계 7절](../db/README.md#판정-작업스케줄러)).
- 장기 대여 수령과 현장 대여는 `professorMailChecked=true`가 아니면 `PROFESSOR_MAIL_REQUIRED`. → 사례 `PICK-02`
- 현장 대여의 반납 기한 선택지는 예약 규칙과 같고, 다음 예약 전까지만(반출 기자재는 그 1시간 전까지). → 사례 `WALK-01`
- 반납 시각은 관리자가 승인한 시각입니다. 기한보다 늦으면 연체 경고 1회(이미 판정 작업이 붙였으면 다시 붙이지 않음). → 사례 `RET-01`
- 학생이 대여 중에 신고한 기기는 반납 때 파손이어도 경고가 없습니다. → 사례 `RET-02`
- 신고하면 그 기기는 바로 `INSPECTION`이 되고 대여는 `RENTING` 그대로입니다. 관리자가 기기 상태를 바꿀 때와 똑같이 대수를 다시 세어, 다른 정상 기기로 줄 수 없는 예약만 **신청 시각이 늦은** 순서로 바로 취소하고 알립니다(경고 없음, 기획안 2장, [DB 설계 4절](../db/README.md#4-삭제와-보존-규칙-확정)). → 사례 `UNIT-01`

- 파손 반납과 반납 후 파손 발견에도 B2-16 예약 정리를 연결합니다. 이미 점검 중인 기기를 파손으로 바꿨다는 이유로 수량을 이중 차감하지 않습니다.
- 학생의 이상 신고 응답에는 자신의 대여만 포함합니다. 다른 예약의 취소 목록은 신고자에게 보내지 않습니다.

### 연장 (백엔드 2)

- 일반 대여만, 한 번만, 기한 전에만. 장기 대여는 `LONG_TERM_EXTENSION_BLOCKED`(창구 안내). → 사례 `EXT-01`
- 2일 이상 대여는 다음 주 금요일 운영 종료까지, 주말·휴무 제외. → 사례 `EXT-02`
- 모델에 `maxRentalMinutes`가 있으면(지금은 레이저 커팅기 120분뿐) 한 신청의 칸 합계가 그 시간 이하여야 하고(`RENTAL_TOO_LONG`), 현장 대여 선택지와 연장도 대여 시작부터 그 시간 안입니다.
- 모델에 `maxRentalDays`가 있으면(지금은 포터블 모니터 7일뿐) 예약·연장 모두 반납일 한도가 다음 주 금요일이 아니라 대여 시작일 + 그 일수입니다(기획안 4장).
- 연장하면 반납 전 알림을 새 기한 기준으로 다시 보냅니다. → 사례 `EXT-03`

### 판정 작업과 경고 (백엔드 2)

- 일반·2일 이상 대여의 노쇼 기준은 **예약 시작 시각 + 10분**(10:00 예약이면 10:10:00부터 노쇼)입니다. 장기는 수령일 운영 종료입니다. 판정 작업은 1분마다 돌므로 노쇼 **처리**는 최대 1분(과 작업 실행 시간)만큼 늦을 수 있지만, 경고와 기록의 시각은 마감 시각으로 남기고, 그 사이 들어온 수령 요청은 위 규칙으로 거절합니다([DB 설계 7절](../db/README.md#판정-작업스케줄러)).
- 노쇼는 신청 단위로 1회, 같은 신청의 남은 칸은 취소. → 사례 `NS-01`
- 예약 시작 시각에 같은 모델의 다른 정상 기기로도 내줄 수 없으면 해당 예약을 즉시 CANCELLED로 처리하고 CANCEL 알림을 보냅니다. 학생에게 경고를 주지 않습니다. 이후 기기가 반납돼도 취소된 예약을 되살리거나 수령 안내를 보내지 않습니다. → 사례 `NS-02`
- 경고 3회면 정지, 수령 전 예약 취소, 보유 기기는 즉시 반납 요망, 6개월은 마지막 반납부터. → 사례 `WARN-01`
- 관리자가 경고를 취소해 3회 미만이 되면 정지 해제. 취소한 경고는 같은 사건으로 다시 생기지 않음. → 사례 `WARN-02`

### 운영 시간·휴무 (백엔드 1, 예약 정리는 백엔드 2 서비스)

- 임시 휴무를 넣으면 그날 걸친 예약만 경고 없이 취소하고 알림, 관리자에게 취소 목록을 돌려줍니다. → 사례 `OPS-01`
- 그날이 반납일인 대여가 일주일 안이면 그 뒤 가장 가까운 운영하는 평일(주말·휴무 제외)의 운영 종료 시각으로 기한을 늘리고 알립니다(기획안 8장). → 사례 `OPS-02`
- 공휴일 받기에 실패하면 기존 달력을 유지하고 관리자에게 알립니다(기획안 8장, [외부 연결 4절](../외부연결.md#4-공휴일--공공데이터포털-특일-정보)).

### AI 추천 (백엔드 1)

- Gemini API 무료 등급을 씁니다. 모델 ID는 B1-21에서 무료 지원 여부를 확인해 선정하며 **실제 호출 전에는 "검증 전"**입니다([외부 연결 3절](../외부연결.md#3-ai-추천--gemini-api)). 응답의 `modelIdVerified`로 확인 여부를 알립니다.
- AI가 우리 목록에 없는 품목을 말하면 버리고, 우리 모델 id가 있는 것만 돌려줍니다(확정).
- 대여 가능 여부는 넣지 않습니다(기획안 10장). 연결 방법과 실패 처리는 [외부 연결](../외부연결.md).

### 관리자 조회와 알림 연결

- 학생 목록의 경고·정지·승인·검색 조건은 전체 결과에 적용한 뒤 페이지를 나눕니다. 유효 경고와 현재 정지 판단은 DB 규칙을 그대로 사용합니다.
- 관리 기록은 한국 날짜 양끝 포함 범위로 조회하고, 여러 action은 OR로 묶습니다. 기간과 종류는 AND입니다. 날짜 한쪽만 전송하거나 시작일이 종료일보다 뒤면 VALIDATION_ERROR입니다.
- 화면의 종류 묶음은 다음 action들을 한 요청의 actions에 보내 조회합니다. 새 action을 도입하면 기록 생성 이슈와 이 매핑을 함께 고칩니다. action 코드 매핑은 프론트를 붙일 때 맞춥니다.

| 화면 묶음 | actions |
|---|---|
| 대여 | PICKUP, WALK_IN, NO_SHOW_CORRECTION |
| 반납 | RETURN, RETURN_CORRECTION, DAMAGE_DISCOVERY |
| 기기 | UNIT_CONDITION, UNIT_EDIT, MODEL_ADD, MODEL_EDIT, MODEL_ARCHIVE, CAPACITY |
| 운영 | OPERATIONS |
| 학생 | WARNING_REVOKE, WITHDRAW |

- 알림은 relatedType과 relatedId로 대상 종류와 번호를 함께 반환합니다. 문의·구매 요청 답변은 RESPONSE이며 INQUIRY/PURCHASE_REQUEST로 구분합니다. 연결 대상이 없으면 둘 다 null입니다. 알림 클릭 후 조회에서도 권한을 검사하고 삭제된 대상은 열지 않습니다.

### 통계 집계 (기능과 아래 집계 세부 기준 모두 확정)

기획안 11장의 기간별 대여·반납·노쇼·연체 수와 많이 빌린 기자재를 응답에 포함합니다. **집계 기준일·건수 단위는 기획안에 구체적으로 정해져 있지 않아 팀이 C-03에서 다음 기준으로 확정했습니다.**

| 항목 | 계산 방법 |
|---|---|
| 조회 기간 | from과 to 필수. 한국 시각 from 00:00 이상, to 다음 날 00:00 미만 |
| 대여 수 | 그 기간 picked_at이 있는 대여 기록 수(실제 수령, 현장 대여 포함) |
| 반납 수 | 그 기간 returned_at이 있는 반납 완료 기록 수(정정 후 시각) |
| 노쇼 수 | 그 기간 created_at인 NO_SHOW 경고 수. 관리자 취소 경고 제외. 신청 단위 1회 |
| 연체 수 | 그 기간 created_at인 LATE 경고 수. 관리자 취소 경고 제외. 대여 단위 1회 |
| 많이 빌린 기자재 | 같은 기간 실제 수령 기록을 모델별로 세어 내림차순, 동률은 modelId 오름차순. 0건 모델 제외 |

경고가 정지 종료로 만료된 것과 관리자가 잘못된 경고를 취소한 것은 구분합니다. 확정 기준에서는 만료 경고는 과거 발생 건수에 포함하고 취소 경고는 제외합니다. 기존 byStatus와 모델·학생 요약은 조회 시점 전체 현황으로 남겨 기간 집계와 구분합니다. 결과가 없으면 건수는 0, 인기 모델 목록은 빈 배열입니다. from > to·날짜 형식 오류는 VALIDATION_ERROR입니다.

## 4. 명세를 바꾸는 절차

1. 바꿀 내용을 팀 채팅에 먼저 알립니다. 칸 이름이나 오류 코드처럼 **앱이 이미 쓰는 약속**이면 프론트 담당의 확인을 받습니다.
2. 같은 PR에서 openapi.yaml과 이 파일을 함께 고칩니다. 규칙이 바뀌면 [기획안](../design/기획안.md)부터 고칩니다.
3. PR 제목 앞에 `[API]`를 붙이고(예: `[API][FEAT] B2-03 예약하기`), 본문에 "무엇이 바뀌었고 앱에서 무엇을 고쳐야 하는지"를 적습니다.
4. 이미 나간 칸을 없애거나 뜻을 바꿀 때는 새 칸을 먼저 추가하고, 앱이 옮긴 뒤 옛 칸을 지웁니다.
