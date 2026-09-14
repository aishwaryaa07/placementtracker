package com.placementtracker.util;

import com.placementtracker.model.RoundResult;
import com.placementtracker.model.RoundResultStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Shared by ApplicationResponse (per-application view) and the funnel dashboard
// (per-round aggregate view) so "is this round actually reachable yet" has exactly one
// implementation. A round is locked once any earlier round (by sequence, same application)
// hasn't been passed yet. The specific reason (still waiting vs. an earlier round already
// failed) is carried through once set - it doesn't change further down the sequence.
public final class RoundResultLockCalculator {

    private RoundResultLockCalculator() {
    }

    public static Map<Long, String> lockReasons(List<RoundResult> roundResultsForOneApplication) {
        List<RoundResult> bySequence = new ArrayList<>(roundResultsForOneApplication);
        bySequence.sort(Comparator.comparing(rr -> rr.getRound().getSequence()));

        Map<Long, String> lockReasonByRoundResultId = new HashMap<>();
        String activeLockReason = null;
        for (RoundResult rr : bySequence) {
            lockReasonByRoundResultId.put(rr.getId(), activeLockReason);
            if (activeLockReason == null && rr.getStatus() != RoundResultStatus.PASSED) {
                activeLockReason = rr.getStatus() == RoundResultStatus.FAILED
                        ? "Locked because an earlier round was failed."
                        : "Complete previous rounds first.";
            }
        }
        return lockReasonByRoundResultId;
    }
}
