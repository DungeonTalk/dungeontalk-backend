//package org.com.dungeontalk.domain.matching.dto.request;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.com.dungeontalk.domain.matching.common.WorldType;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//class MatchingJoinRequestTest {
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    @Test
//    @DisplayName("Builder로 객체 생성 테스트")
//    void builderCreatesObject() {
//        // given
//        String memberId = "user-12345";
//        WorldType worldType = WorldType.FANTASY;
//
//        // when
//        MatchingJoinRequest result = MatchingJoinRequest.builder()
//                .memberId(memberId)
//                .worldType(worldType)
//                .build();
//
//        // then
//        assertThat(result).isNotNull();
//        assertThat(result.getMemberId()).isEqualTo(memberId);
//        assertThat(result.getWorldType()).isEqualTo(worldType);
//    }
//
//    @Test
//    @DisplayName("Java 객체를 JSON으로 변환")
//    void serializeToJson() throws Exception {
//        // given
//        MatchingJoinRequest request = MatchingJoinRequest.builder()
//                .memberId("test-user")
//                .worldType(WorldType.FANTASY)
//                .build();
//
//        // when
//        String json = objectMapper.writeValueAsString(request);
//
//        // then
//        assertThat(json).contains("\"memberId\":\"test-user\"");
//        assertThat(json).contains("\"worldType\":\"FANTASY\"");
//    }
//
//    @Test
//    @DisplayName("JSON을 Java 객체로 변환")
//    void deserializeFromJson() throws Exception {
//        // given
//        String json = """
//            {
//                "memberId": "test-user",
//                "worldType": "FANTASY"
//            }
//            """;
//
//        // when
//        MatchingJoinRequest result = objectMapper.readValue(json, MatchingJoinRequest.class);
//
//        // then
//        assertThat(result).isNotNull();
//        assertThat(result.getMemberId()).isEqualTo("test-user");
//        assertThat(result.getWorldType()).isEqualTo(WorldType.FANTASY);
//    }
//}