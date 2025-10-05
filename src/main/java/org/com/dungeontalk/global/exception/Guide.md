# 예외 처리 및 표준 에러 응답 가이드

## 개요
이 문서는 공통 응답 형식(`RsData`)과 연동된 예외 처리 흐름을 정리하여,   
일관된 에러 응답 형식을 유지하고 예외에서 HTTP 응답으로의 매핑을 단순화하는 것을 목표로 합니다.

## 구성 요소

### 1. ErrorCode
- **유형**: Enum
- **목적**: 에러 식별자, 메시지, HTTP 상태 코드를 저장.
- **예시**:
    - `MEMBER_NOT_FOUND`: 404, "멤버를 찾을 수 없습니다."
    - `DATABASE_ERROR`: 500, "데이터베이스 오류"

### 2. CustomException
- **유형**: 도메인별 예외 클래스 (예: `MemberException`)   
-> 이후, 도메인들이 추가되면 각 도메인 별 CustomException을 구현하겠습니다.
- **목적**: 도메인별 에러를 `ErrorCode`와 함께 캡슐화.

### 3. GlobalExceptionHandler
- **유형**: `@ControllerAdvice` 기반 전역 예외 처리기
- **목적**: 예외 처리를 중앙화하고 표준화된 HTTP 응답으로 매핑.

### 4. RsData, RsDataFactory, Empty
- **목적**: 공통 응답 포맷과 생성 유틸리티 정의.
- **구조**:
    - `resultCode`: 에러 코드 식별자 (예: `MEMBER_ERROR_001`, `GLOBAL_ERROR_001`)
    - `statusCode`: HTTP 상태 코드 (예: 404, 500)
    - `msg`: 사람이 읽을 수 있는 에러 메시지
    - `data`: 빈 객체(`{}`) 또는 선택적으로 에러 상세 정보, 타임스탬프 등 메타데이터 (보안 주의).

## 구현 예시

### 서비스 코드 예시
```java
public MemberDto findMember(Long id) {
    return memberRepository.findById(id)
            .orElseThrow(() -> new MemberException(ErrorCode.MEMBER_NOT_FOUND));
}
```

## 클라이언트 응답 예시
### 404: MEMBER_NOT_FOUND

HTTP 상태: 404 NOT_FOUND
응답 본문:
```
{
"resultCode": "MEMBER_ERROR_001",
"statusCode": 404,
"msg": "멤버를 찾을 수 없습니다.",
"data": {}
}
```

### 500: DATABASE_ERROR

HTTP 상태: 500 INTERNAL_SERVER_ERROR
응답 본문:

```
{
  "resultCode": "GLOBAL_ERROR_001",
  "statusCode": 500,
  "msg": "데이터베이스 오류",
  "data": {}
}
```