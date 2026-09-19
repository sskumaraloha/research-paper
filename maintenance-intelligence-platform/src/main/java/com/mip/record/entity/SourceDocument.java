package com.mip.record.entity;

import com.mip.common.entity.BaseEntity;
import com.mip.plant.entity.Plant;
import com.mip.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** An uploaded file that maintenance records were imported from. */
@Entity
@Table(name = "source_documents")
@Getter
@Setter
@NoArgsConstructor
public class SourceDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    /** SHA-256 of the file content, used to warn about re-uploads of the same file. */
    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(nullable = false, length = 500)
    private String storagePath;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    public SourceDocument(Plant plant, String filename, String contentType, long sizeBytes,
                          String checksum, String storagePath, User uploadedBy) {
        this.plant = plant;
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.checksum = checksum;
        this.storagePath = storagePath;
        this.uploadedBy = uploadedBy;
    }
}
