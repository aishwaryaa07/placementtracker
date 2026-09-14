package com.placementtracker.model;

import com.placementtracker.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "student_profiles")
@Getter
@Setter
@NoArgsConstructor
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String branch;

    private Integer graduationYear;

    private BigDecimal cgpa;

    private String phone;

    @Column(length = 500)
    private String resumeUrl;

    @Column(length = 500)
    private String tenthMarksheetUrl;

    @Column(length = 500)
    private String twelfthMarksheetUrl;

    // Distinct from the overall `cgpa` above - the most recently completed semester's CGPA,
    // not a cumulative figure.
    private BigDecimal recentSemesterCgpa;
}
