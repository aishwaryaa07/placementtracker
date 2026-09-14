package com.placementtracker.service;

import com.placementtracker.dto.RoundScoreImportConfirmRequest;
import com.placementtracker.dto.RoundScoreImportPreviewResponse;
import com.placementtracker.dto.RoundScoreImportResultResponse;
import com.placementtracker.dto.RoundScoreImportRowPreview;
import com.placementtracker.dto.RoundScoreImportRowRequest;
import com.placementtracker.dto.RoundScoreImportSkippedRow;
import com.placementtracker.exception.ApplicationNotAllowedException;
import com.placementtracker.exception.ResourceNotFoundException;
import com.placementtracker.model.Round;
import com.placementtracker.model.RoundResult;
import com.placementtracker.model.RoundResultStatus;
import com.placementtracker.model.SelectionMode;
import com.placementtracker.repository.RoundRepository;
import com.placementtracker.repository.RoundResultRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class RoundScoreImportService {

    private static final List<String> HEADERS =
            List.of("roundResultId", "studentName", "studentEmail", "branch", "existingScore", "existingStatus", "score");

    private final RoundRepository roundRepository;
    private final RoundResultRepository roundResultRepository;
    private final RoundResultService roundResultService;

    // Template lists every currently-reachable (not locked) round result for this round, so
    // an admin only ever fills in a score column - roundResultId (the only column read back
    // on import) is an existing PK, never a name/email match.
    @Transactional(readOnly = true)
    public String buildTemplate(Long roundId) {
        assertRoundExists(roundId);
        List<RoundResult> results = roundResultRepository.findByRoundId(roundId).stream()
                .filter(roundResultService::priorRoundsPassed)
                .toList();

        StringWriter writer = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(HEADERS.toArray(new String[0])).build())) {
            for (RoundResult rr : results) {
                printer.printRecord(
                        rr.getId(),
                        rr.getApplication().getStudent().getUser().getName(),
                        rr.getApplication().getStudent().getUser().getEmail(),
                        rr.getApplication().getStudent().getBranch(),
                        rr.getScore(),
                        rr.getStatus(),
                        "");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return writer.toString();
    }

    // Pure computation - no DB writes. Re-validated from scratch at confirm() time, so a
    // preview going stale (someone else grades a row in the meantime) is caught there, not
    // trusted here.
    @Transactional(readOnly = true)
    public RoundScoreImportPreviewResponse preview(Long roundId, MultipartFile file) {
        Round round = getRoundOrThrow(roundId);
        List<RoundScoreImportRowPreview> rows = new ArrayList<>();

        for (CSVRecord record : parseCsv(file)) {
            BigDecimal score = parseScore(record);
            if (score == null) {
                continue; // blank score - nothing to preview, not an error
            }
            Long roundResultId = parseRoundResultId(record);
            RowOutcome outcome = evaluateRow(round, roundResultId, score);
            rows.add(toPreviewRow(outcome, roundResultId, score));
        }

        return new RoundScoreImportPreviewResponse(roundId, rows);
    }

    // Driven by the JSON rows the frontend already holds from preview() - not a re-upload or
    // a server-held "pending import" session (this app has no session store; JWT auth is
    // stateless throughout, and this keeps that true). Every row is re-validated here from
    // scratch, then committed through scoreRoundResult - the same method student self-submit
    // and interviewer entry use, so there is exactly one place grading rules live.
    public RoundScoreImportResultResponse confirm(Long roundId, RoundScoreImportConfirmRequest request) {
        Round round = getRoundOrThrow(roundId);
        int committed = 0;
        List<RoundScoreImportSkippedRow> skipped = new ArrayList<>();

        for (RoundScoreImportRowRequest row : request.getRows()) {
            RowOutcome outcome = evaluateRow(round, row.getRoundResultId(), row.getScore());
            if (outcome.error() != null) {
                skipped.add(new RoundScoreImportSkippedRow(row.getRoundResultId(), outcome.error()));
                continue;
            }
            roundResultService.scoreRoundResult(outcome.roundResult(), row.getScore(), outcome.roundResult().getRemarks());
            committed++;
        }

        return new RoundScoreImportResultResponse(committed, skipped);
    }

    private record RowOutcome(RoundResult roundResult, String predictedStatus, String error) {
        static RowOutcome ofError(String error) {
            return new RowOutcome(null, null, error);
        }
    }

    private RowOutcome evaluateRow(Round round, Long roundResultId, BigDecimal score) {
        if (roundResultId == null) {
            return RowOutcome.ofError("Missing roundResultId");
        }
        Optional<RoundResult> found = roundResultRepository.findById(roundResultId);
        if (found.isEmpty()) {
            return RowOutcome.ofError("No round result found with this id");
        }
        RoundResult roundResult = found.get();
        if (!roundResult.getRound().getId().equals(round.getId())) {
            return RowOutcome.ofError("This round result belongs to a different round");
        }
        if (!roundResultService.priorRoundsPassed(roundResult)) {
            return RowOutcome.ofError("This round is locked - the student hasn't passed all earlier rounds yet");
        }
        if (roundResult.getStatus() != RoundResultStatus.PENDING && roundResult.getStatus() != RoundResultStatus.SCORED) {
            return RowOutcome.ofError("This round has already been graded");
        }
        if (score == null || score.signum() < 0) {
            return RowOutcome.ofError("Score must be a non-negative number");
        }

        SelectionMode mode = round.getEffectiveSelectionMode();
        if (mode == SelectionMode.THRESHOLD) {
            if (round.getMinScore() == null) {
                return RowOutcome.ofError("This round does not accept a score-based import");
            }
            String predicted = score.compareTo(round.getMinScore()) >= 0 ? "PASSED" : "FAILED";
            return new RowOutcome(roundResult, predicted, null);
        }
        return new RowOutcome(roundResult, "SCORED", null);
    }

    private RoundScoreImportRowPreview toPreviewRow(RowOutcome outcome, Long roundResultId, BigDecimal newScore) {
        if (outcome.error() != null) {
            return new RoundScoreImportRowPreview(roundResultId, null, null, null, null, newScore, null, outcome.error());
        }
        RoundResult rr = outcome.roundResult();
        return new RoundScoreImportRowPreview(
                rr.getId(),
                rr.getApplication().getStudent().getUser().getName(),
                rr.getApplication().getStudent().getUser().getEmail(),
                rr.getScore(),
                rr.getStatus().name(),
                newScore,
                outcome.predictedStatus(),
                null);
    }

    private Iterable<CSVRecord> parseCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApplicationNotAllowedException("No file was uploaded");
        }
        try (CSVParser parser = CSVParser.parse(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {
            return new ArrayList<>(parser.getRecords());
        } catch (IOException e) {
            throw new ApplicationNotAllowedException("Could not read the uploaded file as CSV");
        }
    }

    private Long parseRoundResultId(CSVRecord record) {
        try {
            String raw = record.get("roundResultId");
            return raw == null || raw.isBlank() ? null : Long.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // A malformed (non-numeric) score cell is treated the same as blank - skipped, not an
    // error row - since there's no score value to report back in the preview/skip response.
    private BigDecimal parseScore(CSVRecord record) {
        try {
            String raw = record.get("score");
            return raw == null || raw.isBlank() ? null : new BigDecimal(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Round getRoundOrThrow(Long roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("No round found with id " + roundId));
    }

    private void assertRoundExists(Long roundId) {
        if (!roundRepository.existsById(roundId)) {
            throw new ResourceNotFoundException("No round found with id " + roundId);
        }
    }
}
