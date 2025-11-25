package com.brainstorming.brainstorming_platform.global;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public class BaseEntity {

    @CreatedDate // 자동으로 createAt 이 채워짐
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate  //자동으로 updatedAt 이 채워짐
    private LocalDateTime updatedAt;

}
