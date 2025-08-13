package org.com.dungeontalk.domain.stat.service;

import org.springframework.stereotype.Service;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.Map;
import java.util.Set;

@Service
public class StatCalculatorService {
    public double calculate(String formula, Map<String, Object> variables) {
        try {
            // 1. 공식에 쓰인 변수 이름 파싱 (exp4j 파싱)
            Expression e = new ExpressionBuilder(formula)
                    .variables(variables.keySet())
                    .build();

            // 2. 공식에서 요구하는 변수 목록과 실제 전달된 변수 비교
            Set<String> requiredVars = e.getVariableNames();
            for (String key : requiredVars) {
                if (!variables.containsKey(key)) {
                    throw new IllegalArgumentException("필요한 변수 '" + key + "'가 누락되었습니다.");
                }
            }

            // 3. 변수 값 설정 (숫자형 변환/검증)
            variables.forEach((key, value) -> {
                try {
                    if (value instanceof Number) {
                        e.setVariable(key, ((Number) value).doubleValue());
                    } else if (value instanceof String) {
                        e.setVariable(key, Double.parseDouble((String) value));
                    } else {
                        throw new IllegalArgumentException("변수 '" + key + "'의 값이 숫자 또는 숫자 문자열이 아닙니다.");
                    }
                } catch (Exception ex) {
                    throw new IllegalArgumentException("변수 '" + key + "'의 값 변환 중 오류: " + value, ex);
                }
            });

            // 4. 연산 수행
            return e.evaluate();

        } catch (IllegalArgumentException e) {
            // 로깅 등 추가
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("수식 해석/계산 오류: " + formula, e);
        }
    }
}
