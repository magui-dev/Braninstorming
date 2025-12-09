package com.brainstorming.brainstorming_platform.domain.brainstorming.service;

import com.brainstorming.brainstorming_platform.domain.brainstorming.dto.*;
import com.brainstorming.brainstorming_platform.domain.idea.dto.IdeaRequestDto;
import com.brainstorming.brainstorming_platform.domain.idea.dto.IdeaResponseDto;
import com.brainstorming.brainstorming_platform.domain.idea.entity.Idea;
import com.brainstorming.brainstorming_platform.domain.idea.service.IdeaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 브레인스토밍 서비스
 * Python FastAPI 호출 및 아이디어 DB 저장
 */
@Service
@RequiredArgsConstructor
public class BrainstormingService {

    private final RestTemplate restTemplate;
    private final IdeaService ideaService;

    @Value("${python.api.url}")
    private String pythonApiUrl;

    /**
     * 브레인스토밍 전체 플로우 실행
     * 
     * @param request 사용자 요청 (목적 + 키워드)
     * @return 생성된 아이디어 목록
     */
    @Transactional
    public BrainstormResponse generate(BrainstormRequest request) {
        try {
            // 1. 세션 생성
            SessionResponse session = createSession();
            String sessionId = session.getSessionId();

            // 2. Q1: 목적 입력
            submitPurpose(sessionId, request.getPurpose());

            // 3. Q2: 워밍업 질문 생성 (선택사항, 일단 호출만)
            WarmupResponse warmup = getWarmupQuestions(sessionId);
            
            // 4. Q2 확인
            confirmWarmup(sessionId);

            // 5. Q3: 자유연상 입력
            submitAssociations(sessionId, request.getAssociations());

            // 6. 아이디어 생성 (핵심!)
            IdeasResponse ideasResponse = generateIdeas(sessionId);

            // 7. DB 저장
            List<IdeaResponseDto> savedIdeas = saveIdeasToDb(request.getUserId(), request.getGuestSessionId(), ideasResponse);

            // 8. 세션 삭제
            deleteSession(sessionId);

            // 9. 응답 생성
            BrainstormResponse response = new BrainstormResponse();
            response.setSessionId(sessionId);
            response.setIdeas(savedIdeas);
            response.setMessage("브레인스토밍 완료! " + savedIdeas.size() + "개의 아이디어가 생성되었습니다.");

            return response;

        } catch (Exception e) {
            throw new RuntimeException("브레인스토밍 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 1. 세션 생성
     */
    private SessionResponse createSession() {
        String url = pythonApiUrl + "/api/v1/brainstorming/session";
        return restTemplate.postForObject(url, null, SessionResponse.class);
    }

    /**
     * 2. Q1 목적 입력
     */
    private PurposeResponse submitPurpose(String sessionId, String purpose) {
        String url = pythonApiUrl + "/api/v1/brainstorming/purpose";
        PurposeRequest request = new PurposeRequest(sessionId, purpose);
        return restTemplate.postForObject(url, request, PurposeResponse.class);
    }

    /**
     * 3. Q2 워밍업 질문 생성
     */
    private WarmupResponse getWarmupQuestions(String sessionId) {
        String url = pythonApiUrl + "/api/v1/brainstorming/warmup/" + sessionId;
        return restTemplate.getForObject(url, WarmupResponse.class);
    }

    /**
     * 4. Q2 확인
     */
    private ConfirmResponse confirmWarmup(String sessionId) {
        String url = pythonApiUrl + "/api/v1/brainstorming/confirm/" + sessionId;
        return restTemplate.postForObject(url, null, ConfirmResponse.class);
    }

    /**
     * 5. Q3 자유연상 입력
     */
    private AssociationsResponse submitAssociations(String sessionId, List<String> associations) {
        String url = pythonApiUrl + "/api/v1/brainstorming/associations/" + sessionId;
        AssociationsRequest request = new AssociationsRequest(sessionId, associations);
        return restTemplate.postForObject(url, request, AssociationsResponse.class);
    }

    /**
     * 6. 아이디어 생성 (핵심!)
     */
    private IdeasResponse generateIdeas(String sessionId) {
        String url = pythonApiUrl + "/api/v1/brainstorming/ideas/" + sessionId;
        return restTemplate.getForObject(url, IdeasResponse.class);
    }

    /**
     * 7. 세션 삭제
     */
    private void deleteSession(String sessionId) {
        String url = pythonApiUrl + "/api/v1/brainstorming/session/" + sessionId;
        restTemplate.delete(url);
    }

    /**
     * Python에서 받은 아이디어를 DB에 저장
     */
    private List<IdeaResponseDto> saveIdeasToDb(Long userId, String guestSessionId, IdeasResponse ideasResponse) {
        List<IdeaResponseDto> savedIdeas = new ArrayList<>();

        for (IdeasResponse.IdeaDto ideaDto : ideasResponse.getIdeas()) {
            // description + analysis를 content에 포함
            String content = ideaDto.getDescription() + "\n\n" + ideaDto.getAnalysis();

            // IdeaRequestDto 생성
            IdeaRequestDto requestDto = new IdeaRequestDto();
            requestDto.setUserId(userId);
            requestDto.setGuestSessionId(guestSessionId);
            requestDto.setTitle(ideaDto.getTitle());
            requestDto.setContent(content);
            requestDto.setPurpose("브레인스토밍으로 생성됨");

            // Entity로 변환 후 저장
            Idea idea = requestDto.toEntity();
            Idea savedIdea = ideaService.save(idea);

            // ResponseDto로 변환
            savedIdeas.add(IdeaResponseDto.from(savedIdea));
        }

        return savedIdeas;
    }
}
