package com.brainstorming.brainstorming_platform.domain.idea.repository;

import com.brainstorming.brainstorming_platform.domain.idea.entity.Idea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IdeaRepository extends JpaRepository<Idea, Long>  {

    //특정 사용자의 모든 아이디어 조회
    List<Idea> findByUserId(Long userId);

    //특정 사용자의 아이디어 갯수
    long countByUserId(Long userId);

}
