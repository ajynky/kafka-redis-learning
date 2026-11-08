package com.learning.repository;

import com.learning.entity.OutboxEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByIdAsc();

    @Modifying
    @Query("delete from OutboxEvent e where e.publishedAt < :cutOff")
    int deletePublishedBefore(@Param("cutOff") Instant cutOff);
}
