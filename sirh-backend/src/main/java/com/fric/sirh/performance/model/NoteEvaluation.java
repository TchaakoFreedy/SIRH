package com.fric.sirh.performance.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteEvaluation {

    private String critereId;
    private String critereNom;
    private Double note;
    private Integer coefficient;
    private Double scorePondere;
}