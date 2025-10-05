# RsData & RsDataFactory

참고 github : https://github.com/jhs512/slog2.git   
위 github의 src/main/java/com/ll/slog2/global/rsData를 참고함

* `RsData`는 REST API 응답을 표준화하는 DTO이며, 생성 로직은 `RsDataFactory`로 분리.  
* `Empty`는 페이로드가 없는 경우를 나타내는 DTO.  
* `ResponseStatus`는 상태 판별 유틸.

## 파일 목록
- `RsData.java`: 응답 DTO (필드: `resultCode`, `statusCode`, `msg`, `data`)
- `Empty.java`: 빈 페이로드용 DTO
- `RsDataFactory.java`: `RsData` 생성용 정적 팩토리 메서드
- `ResponseStatus.java`: `statusCode` 기반 성공/실패 판별 유틸

## 주요 설계 포인트
- **SRP 준수**: DTO는 데이터 구조만 담당, 생성/검증은 팩토리에 위임
- **resultCode 포맷**: `"XXX-Y"` 형태 (예: `"200-1"`, 하이픈 앞은 HTTP 상태 코드)
- **기본 상수**: `RsDataFactory.OK`, `RsDataFactory.FAIL` (Empty 페이로드)
- **Null 검사**: `Objects.requireNonNull`로 입력 인자 방어

## 사용 예시
```java
// 단일 데이터 반환 (성공, 기본 메시지)
RsData<UserDto> ok = RsDataFactory.of(userDto);

// 메시지 + 데이터
RsData<UserDto> res = RsDataFactory.of("사용자 조회 성공", userDto);

// 커스텀 resultCode + 메시지 (data는 Empty)
RsData<Empty> notFound = RsDataFactory.of("404-1", "사용자 없음");

// 기존 응답에서 페이로드만 교체
RsData<UserDto> replaced = RsDataFactory.newDataOf(RsDataFactory.OK, userDto);

// 상태 판별
boolean success = ResponseStatus.isSuccess(res.getStatusCode());