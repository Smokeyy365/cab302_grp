package com.cab302.eduplanner.service;

import com.cab302.eduplanner.model.RubricItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RubricServiceTest {

    private final RubricService rubricService = new RubricService();

    @Test
    void totalPointsSumsAllItems() {
        List<RubricItem> items = List.of(
                new RubricItem("Criteria 1", 5),
                new RubricItem("Criteria 2", 10),
                new RubricItem("Criteria 3", 15)
        );

        int total = rubricService.totalPoints(items);

        assertEquals(30, total);
    }

    @Test
    void totalPointsWithNullListReturnsZero() {
        int total = rubricService.totalPoints(null);

        assertEquals(0, total);
    }
}
