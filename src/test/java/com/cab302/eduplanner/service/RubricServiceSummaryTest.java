package com.cab302.eduplanner.service;

import com.cab302.eduplanner.model.RubricItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RubricServiceSummaryTest {

    private final RubricService svc = new RubricService();

    @Test
    void summarizeReturnsTotalAverageAndCount() {
        List<RubricItem> items = List.of(
                new RubricItem("A", 3),
                new RubricItem("B", 6),
                new RubricItem("C", 9)
        );

        RubricService.RubricSummary s = svc.summarize(items);

        assertEquals(18, s.getTotal());
        assertEquals(6.0, s.getAverage());
        assertEquals(3, s.getCount());
    }
}
