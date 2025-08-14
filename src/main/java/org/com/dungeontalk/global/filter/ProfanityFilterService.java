package org.com.dungeontalk.global.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.global.filter.config.ProfanityFilterProperties;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
// import io.github.vaneproject.badwordfiltering.BadWordFiltering;
import java.text.Normalizer;

/**
 * 욕설 필터링 서비스
 * VaneProject BadWordFiltering을 활용한 한국어 지원 욕설 필터링
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfanityFilterService {

    private final ProfanityFilterProperties properties;
    private Set<String> profanityWords;
    private Set<Pattern> profanityPatterns;
    // private BadWordFiltering badWordFiltering;

    @PostConstruct
    public void init() {
        // initializeBadWordFiltering();
        initializeProfanityWords();
        initializeProfanityPatterns();
        log.info("ProfanityFilterService initialized with {} custom words and {} patterns", 
                profanityWords.size(), profanityPatterns.size());
    }
    
    /*
    private void initializeBadWordFiltering() {
        try {
            badWordFiltering = new BadWordFiltering();
            log.info("VaneProject BadWordFiltering 초기화 완료");
        } catch (Exception e) {
            log.warn("BadWordFiltering 초기화 실패: {}", e.getMessage());
            badWordFiltering = null;
        }
    }
    */
    
    /*
    private void initializeLevenshteinDistance() {
        try {
            levenshteinDistance = new LevenshteinDistance(2); // 2글자까지 차이 허용
            log.info("한국어 지원 Levenshtein Distance 필터링 초기화 완료");
        } catch (Exception e) {
            log.warn("Levenshtein Distance 초기화 실패: {}", e.getMessage());
            levenshteinDistance = null;
        }
    }
    */

    private void initializeProfanityWords() {
        profanityWords = new HashSet<>();
        
        try {
            // 외부 파일에서 욕설 단어 로드
            ClassPathResource resource = new ClassPathResource("profanity-words.txt");
            try (InputStream inputStream = resource.getInputStream()) {
                String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                
                profanityWords = Arrays.stream(content.split("\n"))
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .filter(line -> !line.startsWith("#"))  // 주석 제외
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());
                        
                log.info("욕설 필터링 단어 로드 완료: {} 개 단어", profanityWords.size());
            }
        } catch (IOException e) {
            log.warn("욕설 단어 파일 로드 실패, 기본 단어 사용: {}", e.getMessage());
            // 파일 로드 실패 시 기본 단어 사용
            profanityWords = new HashSet<>(Arrays.asList(
                "바보", "멍청이", "시발", "stupid", "damn", "hell"
            ));
        }
    }

    private void initializeProfanityPatterns() {
        profanityPatterns = new HashSet<>();
        // 패턴 기반 필터링 (자음분리, 띄어쓰기 우회 등)
        profanityPatterns.add(Pattern.compile("ㅅ+ㅂ+", Pattern.CASE_INSENSITIVE));
        profanityPatterns.add(Pattern.compile("시+\\s*발+", Pattern.CASE_INSENSITIVE));
    }

    /**
     * 텍스트에 욕설이 포함되어 있는지 확인
     */
    public boolean containsProfanity(String text) {
        if (!properties.isEnabled() || text == null || text.trim().isEmpty()) {
            log.debug("욕설 필터링 비활성화 또는 빈 텍스트: enabled={}, text={}", properties.isEnabled(), text);
            return false;
        }
        
        try {
            log.debug("욕설 검사 시작: 원본='{}'", text);
            
            // 1. 커스텀 단어 매칭
            String normalizedText = normalizeKoreanText(text);
            for (String word : profanityWords) {
                if (normalizedText.contains(word.toLowerCase())) {
                    log.warn("커스텀 욕설 감지됨: '{}'", word);
                    return true;
                }
            }
            
            // 2. 한국어 유사도 검사 (Levenshtein Distance) - 임시 비활성화
            /*
            if (levenshteinDistance != null) {
                for (String word : profanityWords) {
                    if (word.length() >= 2) { // 2글자 이상만 유사도 검사
                        Integer distance = levenshteinDistance.apply(normalizedText, word);
                        if (distance != null && distance <= 1 && word.length() >= 3) {
                            log.warn("유사 욕설 감지됨: '{}' (distance: {})", word, distance);
                            return true;
                        }
                    }
                }
            }
            */
            
            // 3. 한국어 패턴 기반 검사
            if (properties.isIgnoreSpaces()) {
                for (Pattern pattern : profanityPatterns) {
                    if (pattern.matcher(normalizedText).find()) {
                        log.warn("패턴 욕설 감지됨: '{}'", pattern.pattern());
                        return true;
                    }
                }
            }
            
            log.debug("욕설 없음: '{}'", text);
            return false;
        } catch (Exception e) {
            log.error("욕설 검사 중 오류 발생: {}", e.getMessage(), e);
            return false; // 에러 발생 시 통과시킴
        }
    }

    /**
     * 욕설을 필터링하여 대체 문자로 변경
     */
    public String filterProfanity(String text) {
        return filterProfanity(text, properties.getReplacement());
    }

    /**
     * 욕설을 커스텀 대체 문자로 변경
     */
    public String filterProfanity(String text, String replacement) {
        if (!properties.isEnabled() || text == null || text.trim().isEmpty()) {
            return text;
        }
        
        try {
            String result = text;
            
            // 1. 커스텀 단어 필터링
            for (String word : profanityWords) {
                result = result.replaceAll("(?i)" + Pattern.quote(word), replacement);
            }
            
            // 2. 한국어 패턴 기반 필터링
            if (properties.isIgnoreSpaces()) {
                for (Pattern pattern : profanityPatterns) {
                    result = pattern.matcher(result).replaceAll(replacement);
                }
            }
            
            return result;
        } catch (Exception e) {
            log.error("욕설 필터링 중 오류 발생: {}", e.getMessage(), e);
            return text;
        }
    }

    /**
     * 커스텀 욕설 단어 추가
     */
    public void addBadWords(String... words) {
        try {
            for (String word : words) {
                profanityWords.add(word.toLowerCase().trim());
            }
            log.info("커스텀 욕설 단어 {} 개 추가됨", words.length);
        } catch (Exception e) {
            log.error("커스텀 욕설 단어 추가 중 오류: {}", e.getMessage(), e);
        }
    }

    /**
     * 욕설 단어 제거
     */
    public void removeBadWords(String... words) {
        try {
            for (String word : words) {
                profanityWords.remove(word.toLowerCase().trim());
            }
            log.info("욕설 단어 {} 개 제거됨", words.length);
        } catch (Exception e) {
            log.error("욕설 단어 제거 중 오류: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 욕설 단어 파일 다시 로드
     */
    public void reloadProfanityWords() {
        log.info("욕설 단어 목록 다시 로드 시작");
        initializeProfanityWords();
    }
    
    /**
     * 현재 로드된 욕설 단어 개수 조회
     */
    public int getProfanityWordCount() {
        return profanityWords.size();
    }

    private String normalizeText(String text) {
        return text.toLowerCase().replaceAll("\\s+", "");
    }
    
    /**
     * 한국어 텍스트 정규화
     * - 유니코드 정규화 (NFD -> NFC)
     * - 공백 제거
     * - 소문자 변환
     * - 특수문자 제거
     */
    private String normalizeKoreanText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        String normalized = text;
        
        // 1. 유니코드 정규화 (자모 분리된 것들을 합성)
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFC);
        
        // 2. 소문자 변환
        normalized = normalized.toLowerCase();
        
        // 3. 공백 제거
        normalized = normalized.replaceAll("\\s+", "");
        
        // 4. 특수문자 중 일부만 제거 (한글 자모는 유지)
        normalized = normalized.replaceAll("[^가-힣ㄱ-ㅎㅏ-ㅣa-z0-9]", "");
        
        return normalized;
    }
}