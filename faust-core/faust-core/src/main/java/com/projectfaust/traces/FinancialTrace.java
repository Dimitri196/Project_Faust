package com.projectfaust.traces;

import com.projectfaust.financial.BankAccount;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;

/**
 * Financial footprint trace — a single card transaction, cash operation,
 * or wire transfer linked to an identified person.
 *
 * <p>Covers: POS terminal payments, ATM withdrawals/deposits, contactless
 * payments, SEPA wire transfers, and cash-heavy operations flagged by
 * financial intelligence feeds.</p>
 *
 * <p>The optional {@link #bankAccount} FK links this trace to a known
 * {@link BankAccount} in the FININT module when the card/account has already
 * been registered. When the account is not yet in the system, the raw card
 * number (masked) and IBAN are stored as strings only.</p>
 *
 * <p>Intelligence use-cases:
 * <ul>
 *   <li>Map a subject's physical movement via merchant GPS coordinates.</li>
 *   <li>Detect cash-intensive behaviour (structuring / smurfing patterns).</li>
 *   <li>Cross-correlate card usage timestamps with digital login events.</li>
 *   <li>Identify co-presence at a merchant with another subject.</li>
 * </ul>
 * </p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(
        name = "financial_traces",
        indexes = {
                @Index(name = "idx_ftrace_external_id",  columnList = "external_id"),
                @Index(name = "idx_ftrace_person",       columnList = "person_id"),
                @Index(name = "idx_ftrace_observed_at",  columnList = "observed_at"),
                @Index(name = "idx_ftrace_bank_account", columnList = "bank_account_id"),
                @Index(name = "idx_ftrace_merchant",     columnList = "merchant_name"),
                @Index(name = "idx_ftrace_country",      columnList = "merchant_country")
        }
)
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FinancialTrace extends BaseTrace {

    // ── Account linkage ───────────────────────────────────────────────────────

    /**
     * Optional FK to a known {@link BankAccount} in the FININT module.
     * Null when the card/account has not yet been registered in the system.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    /**
     * Masked card number (last 4 digits, e.g. "•••• •••• •••• 4291").
     * Never store full PAN. Retained for correlation when bankAccount is null.
     */
    @Column(name = "card_number_masked", length = 25)
    private String cardNumberMasked;

    /** Raw IBAN if the trace represents a wire transfer. */
    @Column(name = "iban_raw", length = 34)
    private String ibanRaw;

    // ── Transaction details ───────────────────────────────────────────────────

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    /** ISO 4217 currency code of the transaction. */
    @Column(name = "currency", length = 3)
    private String currency;

    /**
     * Transaction type: CARD_PAYMENT, ATM_WITHDRAWAL, ATM_DEPOSIT,
     * WIRE_TRANSFER, CONTACTLESS, ONLINE_PAYMENT, CASH_DEPOSIT, etc.
     */
    @Column(name = "transaction_type", length = 60)
    private String transactionType;

    /** Authorisation or reference code from the payment processor. */
    @Column(name = "authorization_code", length = 50)
    private String authorizationCode;

    // ── Merchant / terminal ───────────────────────────────────────────────────

    @Column(name = "merchant_name", length = 300)
    private String merchantName;

    /**
     * Merchant Category Code (ISO 18245) — classifies the type of business.
     * E.g. "5411" = grocery store, "7011" = hotel, "4111" = commuter transport.
     */
    @Column(name = "merchant_category_code", length = 4)
    private String merchantCategoryCode;

    @Column(name = "terminal_id", length = 50)
    private String terminalId;

    /** Street address of the POS terminal or ATM. */
    @Column(name = "merchant_address", length = 500)
    private String merchantAddress;

    @Column(name = "merchant_city", length = 100)
    private String merchantCity;

    /** ISO 3166-1 alpha-2 country code of the merchant/terminal. */
    @Column(name = "merchant_country", length = 2)
    private String merchantCountry;

    /** GPS latitude of the terminal (from merchant registry, not real-time). */
    @Column(name = "latitude")
    private Double latitude;

    /** GPS longitude of the terminal. */
    @Column(name = "longitude")
    private Double longitude;

    // ── AML signals ───────────────────────────────────────────────────────────

    /**
     * True if this transaction was flagged by the source system's AML rules
     * (e.g. structuring, unusual amount, sanctioned merchant).
     */
    @Builder.Default
    @Column(name = "aml_flagged")
    private boolean amlFlagged = false;

    /** Reason code from the AML system if {@code amlFlagged} is true. */
    @Column(name = "aml_reason", length = 500)
    private String amlReason;
}
