package com.cab302.eduplanner.service;

import com.cab302.eduplanner.model.RubricItem;
import java.util.List;

public class RubricService {

    public int totalPoints(List<RubricItem> items) {

        return (items == null)
                ? 0
                : items.stream().mapToInt(RubricItem::getPoints).sum();
    }

    public static class RubricSummary {
        private final int total;
        private final double average;
        private final int count;

        public RubricSummary(int total, double average, int count) {
            this.total = total;
            this.average = average;
            this.count = count;
        }

        public int getTotal() { return total; }
        public double getAverage() { return average; }
        public int getCount() { return count; }
    }

    public RubricSummary summarize(List<RubricItem> items) {
        if (items == null || items.isEmpty()) return new RubricSummary(0, 0.0, 0);
        int total = totalPoints(items);
        double avg = total / (double) items.size();
        return new RubricSummary(total, avg, items.size());
    }
}
