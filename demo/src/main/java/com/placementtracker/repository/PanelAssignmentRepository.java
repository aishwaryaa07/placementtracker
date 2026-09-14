package com.placementtracker.repository;

import com.placementtracker.model.PanelAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PanelAssignmentRepository extends JpaRepository<PanelAssignment, Long> {

    List<PanelAssignment> findByInterviewerId(Long interviewerId);

    List<PanelAssignment> findByInterviewerIdAndRoundResult_Round_Id(Long interviewerId, Long roundId);

    List<PanelAssignment> findByRoundResult_Round_Id(Long roundId);

    boolean existsByInterviewerIdAndRoundResultId(Long interviewerId, Long roundResultId);
}
