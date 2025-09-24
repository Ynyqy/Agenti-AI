package com.lusheng.ragblock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "agt_ly_affairs_files")
public class AgtAffairsFile {

    @Id
    private Integer id;

    private String title;

    @Column(name = "pdf_url")
    private String pdfUrl;

    // 您可以根据需要映射其他字段，但对于当前需求，这两个就够了
}