package com.whim.tarot.data;

import com.whim.tarot.domain.Card;

import java.util.List;

/** Source of the 78-card tarot deck. */
public interface CardRepository {
    List<Card> getAllCards();   // EXACTLY 78, ordered by id 0..77
    Card getById(int id);
    int size();                  // 78
}
