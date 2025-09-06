package com.cab302.eduplanner.service;

import com.cab302.eduplanner.model.RubricItem;
import java.util.List;

public class RubricService {

    public int totalPoints(List<RubricItem> items) {

        return (items == null)
                ? 0
                : items.stream().mapToInt(RubricItem::getPoints).sum();
    }
}
