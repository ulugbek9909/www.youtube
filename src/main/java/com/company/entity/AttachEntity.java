package com.company.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "attach")
@Getter
@Setter
public class AttachEntity extends BaseEntity {

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private String extension;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column
    private Long duration;
}