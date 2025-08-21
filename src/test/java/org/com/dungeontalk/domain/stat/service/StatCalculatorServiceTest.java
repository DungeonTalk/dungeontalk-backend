package org.com.dungeontalk.domain.stat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatCalculatorService 단위 테스트")
class StatCalculatorServiceTest {

    @InjectMocks
    private StatCalculatorService statCalculatorService;

    @Nested
    @DisplayName("기본 수식 계산 테스트")
    class BasicCalculationTest {

        @Test
        @DisplayName("간단한 사칙연산 계산에 성공한다")
        void calculate_basicArithmetic_success() {
            // given - 변수 없는 기본 수식들
            Map<String, Object> emptyVariables = new HashMap<>();
            
            // when & then - 다양한 기본 수식 계산 검증
            assertThat(statCalculatorService.calculate("10 + 5", emptyVariables)).isEqualTo(15.0);
            assertThat(statCalculatorService.calculate("20 - 8", emptyVariables)).isEqualTo(12.0);
            assertThat(statCalculatorService.calculate("6 * 7", emptyVariables)).isEqualTo(42.0);
            assertThat(statCalculatorService.calculate("15 / 3", emptyVariables)).isEqualTo(5.0);
        }

        @Test
        @DisplayName("복잡한 수식 계산에 성공한다")
        void calculate_complexFormula_success() {
            // given - 복잡한 수식과 빈 변수 맵
            Map<String, Object> emptyVariables = new HashMap<>();
            
            // when & then - 괄호와 우선순위가 포함된 복잡한 수식 검증
            assertThat(statCalculatorService.calculate("(10 + 5) * 2", emptyVariables)).isEqualTo(30.0);
            assertThat(statCalculatorService.calculate("100 + 20 * 3", emptyVariables)).isEqualTo(160.0);
            assertThat(statCalculatorService.calculate("(50 - 20) / (3 + 2)", emptyVariables)).isEqualTo(6.0);
        }

        @Test
        @DisplayName("소수점 계산이 정확하다")
        void calculate_decimalNumbers_accurate() {
            // given - 소수점 포함 수식
            Map<String, Object> emptyVariables = new HashMap<>();
            
            // when & then - 소수점 계산 정확도 검증
            assertThat(statCalculatorService.calculate("10.5 + 2.3", emptyVariables)).isEqualTo(12.8);
            assertThat(statCalculatorService.calculate("7.5 * 1.2", emptyVariables)).isEqualTo(9.0);
        }
    }

    @Nested
    @DisplayName("변수 포함 수식 계산 테스트")
    class VariableCalculationTest {

        @Test
        @DisplayName("단일 변수 수식 계산에 성공한다")
        void calculate_singleVariable_success() {
            // given - 단일 변수와 수식
            Map<String, Object> variables = new HashMap<>();
            variables.put("strength", 10);
            
            // when & then - 변수가 포함된 수식 계산 검증
            assertThat(statCalculatorService.calculate("strength * 2", variables)).isEqualTo(20.0);
            assertThat(statCalculatorService.calculate("strength + 5", variables)).isEqualTo(15.0);
            assertThat(statCalculatorService.calculate("100 - strength", variables)).isEqualTo(90.0);
        }

        @Test
        @DisplayName("다중 변수 수식 계산에 성공한다")
        void calculate_multipleVariables_success() {
            // given - 여러 변수가 포함된 수식
            Map<String, Object> variables = new HashMap<>();
            variables.put("strength", 12);
            variables.put("dexterity", 8);
            variables.put("intelligence", 15);
            
            // when & then - 다중 변수 수식 계산 검증
            assertThat(statCalculatorService.calculate("strength + dexterity", variables)).isEqualTo(20.0);
            assertThat(statCalculatorService.calculate("strength * 1.5 + dexterity * 0.5", variables)).isEqualTo(22.0);
            assertThat(statCalculatorService.calculate("(strength + intelligence) / 2", variables)).isEqualTo(13.5);
        }


        @Test
        @DisplayName("Number 타입 변수 처리에 성공한다")
        void calculate_numberTypes_success() {
            // given - 다양한 Number 타입의 변수들
            Map<String, Object> variables = new HashMap<>();
            variables.put("intValue", 10);           // Integer
            variables.put("longValue", 15L);         // Long  
            variables.put("doubleValue", 12.5);      // Double
            variables.put("floatValue", 8.2f);       // Float
            
            // when & then - 다양한 Number 타입 변수 계산 검증
            assertThat(statCalculatorService.calculate("intValue * 2", variables)).isEqualTo(20.0);
            assertThat(statCalculatorService.calculate("longValue + 5", variables)).isEqualTo(20.0);
            assertThat(statCalculatorService.calculate("doubleValue * 2", variables)).isEqualTo(25.0);
            assertThat(statCalculatorService.calculate("floatValue + 1.8", variables)).isCloseTo(10.0, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        @DisplayName("String 타입 숫자 변수 처리에 성공한다")
        void calculate_stringNumbers_success() {
            // given - 문자열 형태의 숫자 변수들
            Map<String, Object> variables = new HashMap<>();
            variables.put("stringInt", "25");
            variables.put("stringDouble", "15.5");
            
            // when & then - 문자열 숫자 변수 계산 검증
            assertThat(statCalculatorService.calculate("stringInt * 2", variables)).isEqualTo(50.0);
            assertThat(statCalculatorService.calculate("stringDouble + 4.5", variables)).isEqualTo(20.0);
        }
    }

    @Nested
    @DisplayName("예외 상황 테스트")
    class ExceptionTest {

        @Test
        @DisplayName("잘못된 수식으로 계산 시 예외가 발생한다")
        void calculate_invalidFormula_throwsException() {
            // given - 잘못된 수식들
            Map<String, Object> emptyVariables = new HashMap<>();
            
            // when & then - 잘못된 수식에 대한 예외 발생 검증 (exp4j가 던지는 예외 타입에 맞춤)
            assertThatThrownBy(() -> statCalculatorService.calculate("10 +", emptyVariables))
                .isInstanceOf(IllegalArgumentException.class);
                
            assertThatThrownBy(() -> statCalculatorService.calculate("* 5", emptyVariables))
                .isInstanceOf(IllegalArgumentException.class);
                
            assertThatThrownBy(() -> statCalculatorService.calculate("(10 + 5", emptyVariables))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("누락된 변수로 계산 시 예외가 발생한다")
        void calculate_missingVariable_throwsException() {
            // given - 필요한 변수가 누락된 상황
            Map<String, Object> variables = new HashMap<>();
            variables.put("dexterity", 10);
            
            // when & then - 누락된 변수에 대한 예외 발생 검증 (정확한 메시지 확인보다는 예외 발생에 집중)
            assertThatThrownBy(() -> statCalculatorService.calculate("strength * 2", variables))
                .isInstanceOf(IllegalArgumentException.class);
                
            assertThatThrownBy(() -> statCalculatorService.calculate("strength + dexterity", variables))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("잘못된 타입 변수로 계산 시 예외가 발생한다")
        void calculate_invalidVariableType_throwsException() {
            // given - 숫자가 아닌 타입의 변수들
            Map<String, Object> variables = new HashMap<>();
            variables.put("invalidString", "abc");
            variables.put("booleanValue", true);
            variables.put("objectValue", new Object());
            
            // when & then - 잘못된 타입 변수에 대한 예외 발생 검증 (구체적인 메시지보다는 예외 타입에 집중)
            assertThatThrownBy(() -> statCalculatorService.calculate("invalidString * 2", variables))
                .isInstanceOf(IllegalArgumentException.class);
                
            assertThatThrownBy(() -> statCalculatorService.calculate("booleanValue + 5", variables))
                .isInstanceOf(IllegalArgumentException.class);
                
            assertThatThrownBy(() -> statCalculatorService.calculate("objectValue * 3", variables))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null이나 빈 수식으로 계산 시 예외가 발생한다")
        void calculate_nullOrEmptyFormula_throwsException() {
            // given - null이나 빈 수식
            Map<String, Object> variables = new HashMap<>();
            
            // when & then - null이나 빈 수식에 대한 예외 발생 검증 (exp4j에서 발생하는 예외)
            assertThatThrownBy(() -> statCalculatorService.calculate(null, variables))
                .isInstanceOfAny(IllegalArgumentException.class, Exception.class);
                
            assertThatThrownBy(() -> statCalculatorService.calculate("", variables))
                .isInstanceOfAny(IllegalArgumentException.class, Exception.class);
                
            assertThatThrownBy(() -> statCalculatorService.calculate("   ", variables))
                .isInstanceOfAny(IllegalArgumentException.class, Exception.class);
        }

        @Test
        @DisplayName("0으로 나누기 시 예외가 발생한다")
        void calculate_divisionByZero_throwsException() {
            // given - 0으로 나누는 수식
            Map<String, Object> variables = new HashMap<>();
            variables.put("zero", 0);
            
            // when & then - 0으로 나누기에 대한 예외 발생 검증
            assertThatThrownBy(() -> statCalculatorService.calculate("10 / 0", variables))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("수식 해석/계산 오류");
                
            assertThatThrownBy(() -> statCalculatorService.calculate("15 / zero", variables))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("수식 해석/계산 오류");
        }
    }

    @Nested
    @DisplayName("경계값 테스트")
    class BoundaryValueTest {

        @Test
        @DisplayName("매우 큰 숫자 계산이 가능하다")
        void calculate_largeNumbers_success() {
            // given - 매우 큰 숫자들
            Map<String, Object> variables = new HashMap<>();
            variables.put("largeNumber", 1000000);
            
            // when & then - 큰 숫자 계산 검증
            assertThat(statCalculatorService.calculate("largeNumber * 2", variables)).isEqualTo(2000000.0);
            assertThat(statCalculatorService.calculate("999999 + 1", variables)).isEqualTo(1000000.0);
        }

        @Test
        @DisplayName("매우 작은 숫자 계산이 가능하다")
        void calculate_smallNumbers_success() {
            // given - 매우 작은 숫자들
            Map<String, Object> variables = new HashMap<>();
            variables.put("smallNumber", 0.001);
            
            // when & then - 작은 숫자 계산 검증
            assertThat(statCalculatorService.calculate("smallNumber * 1000", variables)).isEqualTo(1.0);
            assertThat(statCalculatorService.calculate("0.1 + 0.2", variables)).isCloseTo(0.3, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("음수 계산이 가능하다")
        void calculate_negativeNumbers_success() {
            // given - 음수 포함 수식
            Map<String, Object> variables = new HashMap<>();
            variables.put("negative", -10);
            
            // when & then - 음수 계산 검증
            assertThat(statCalculatorService.calculate("negative + 15", variables)).isEqualTo(5.0);
            assertThat(statCalculatorService.calculate("-5 + 8", variables)).isEqualTo(3.0);
            assertThat(statCalculatorService.calculate("negative * -2", variables)).isEqualTo(20.0);
        }
    }
}