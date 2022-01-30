package ru.netology.data;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CardTransaction {
    private String id;
    private String source;
    private String target;
    private int amount_in_kopecks;
    private String created;
}