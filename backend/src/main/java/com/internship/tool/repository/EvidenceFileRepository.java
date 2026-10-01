package com.internship.tool.repository;
import com.internship.tool.entity.EvidenceFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface EvidenceFileRepository extends JpaRepository<EvidenceFile, UUID> {
    List<EvidenceFile> findByFindingIdOrderByCreatedAtDesc(Long findingId);
    Optional<EvidenceFile> findByIdAndFindingId(UUID id, Long findingId);
    long countByFindingId(Long findingId);
}
