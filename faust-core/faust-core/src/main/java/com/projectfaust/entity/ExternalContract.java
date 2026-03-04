package com.projectfaust.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "external_contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalContract {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "external_id", unique = true)
    private String externalId; // ID z Hlídače Státu

    @Column(name = "supplier_ico")
    private String supplierIco;

    @Column(name = "buyer_ico")
    private String buyerIco;

    @Column(name = "amount_total")
    private BigDecimal amountTotal;

    @Column(name = "contract_date")
    private LocalDate contractDate;

    @Column(name = "subject_text", columnDefinition = "TEXT")
    private String subjectText;

    @Transient
    private String rawJsonData;
}