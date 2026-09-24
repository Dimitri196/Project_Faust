package com.projectfaust.insurance;

import com.projectfaust.insurance.dto.InsuranceRecordRequestDto;
import com.projectfaust.insurance.dto.InsuranceRecordResponseDto;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * MapStruct mapper for the {@link InsuranceRecord} domain within Project Faust.
 *
 * <p>Handles three mapping concerns:</p>
 * <ul>
 *   <li><b>Entity → response</b> — flattens the person FK into
 *       {@code personPublicId} / {@code personFullName} and computes the
 *       {@code annualPremiumEquivalent} convenience field.</li>
 *   <li><b>Request → entity</b> — initialises a new entity from inbound data;
 *       the {@code person} FK is resolved by the service layer after mapping.</li>
 *   <li><b>Update merge</b> — applies non-null fields from a request onto an
 *       existing entity without overwriting fields absent from the request.</li>
 * </ul>
 *
 * @author Dimitri / Project Faust
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InsuranceRecordMapper {

    // -------------------------------------------------------------------------
    // Entity → Response DTO
    // -------------------------------------------------------------------------

    /**
     * Maps a persisted {@link InsuranceRecord} to its response representation.
     *
     * <p>The person FK is dereferenced to expose only the public UUID and
     * computed full name. The {@code annualPremiumEquivalent} is calculated
     * from {@code premiumAmount} and {@code premiumFrequency}.</p>
     *
     * @param entity the insurance record entity.
     * @return the populated response DTO.
     */
    @Mapping(source = "person.externalId",   target = "personPublicId")
    @Mapping(source = "entity",              target = "personFullName",
            qualifiedByName = "resolvePersonFullName")
    @Mapping(source = "entity",              target = "annualPremiumEquivalent",
            qualifiedByName = "computeAnnualPremium")
    InsuranceRecordResponseDto toResponse(InsuranceRecord entity);

    /**
     * Maps a list of entities to response DTOs.
     *
     * @param entities the list of insurance record entities.
     * @return list of response DTOs.
     */
    List<InsuranceRecordResponseDto> toResponseList(List<InsuranceRecord> entities);

    // -------------------------------------------------------------------------
    // Request DTO → Entity
    // -------------------------------------------------------------------------

    /**
     * Initialises a new {@link InsuranceRecord} from an inbound request.
     *
     * <p>The {@code person} FK and the generated fields ({@code externalId},
     * {@code ingestedAt}) are excluded — the service layer handles them.</p>
     *
     * @param dto the inbound creation DTO.
     * @return a new, unpersisted entity.
     */
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "externalId",  ignore = true)
    @Mapping(target = "person",      ignore = true)
    @Mapping(target = "ingestedAt",  ignore = true)
    InsuranceRecord toEntity(InsuranceRecordRequestDto dto);

    // -------------------------------------------------------------------------
    // Update merge
    // -------------------------------------------------------------------------

    /**
     * Merges non-null fields from the request DTO onto an existing entity.
     *
     * <p>Fields not present in the request (null) are left unchanged on the
     * target, preserving values that were not explicitly updated.</p>
     *
     * @param dto    the partial update request.
     * @param entity the existing entity to update (modified in place).
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "externalId",  ignore = true)
    @Mapping(target = "person",      ignore = true)
    @Mapping(target = "ingestedAt",  ignore = true)
    void updateEntity(InsuranceRecordRequestDto dto, @MappingTarget InsuranceRecord entity);

    // -------------------------------------------------------------------------
    // Named helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves the person's formatted display name from the entity graph.
     *
     * @param entity the insurance record whose person FK is to be dereferenced.
     * @return the person's full name, or {@code null} if no person is linked.
     */
    @Named("resolvePersonFullName")
    default String resolvePersonFullName(InsuranceRecord entity) {
        if (entity.getPerson() == null) return null;
        return entity.getPerson().getFullName();
    }

    /**
     * Computes the annual premium equivalent from the raw premium amount and
     * billing frequency.
     *
     * <p>Returns {@code null} when either {@code premiumAmount} or
     * {@code premiumFrequency} is unknown.</p>
     *
     * @param entity the insurance record.
     * @return annual premium equivalent, or {@code null} if not computable.
     */
    @Named("computeAnnualPremium")
    default BigDecimal computeAnnualPremium(InsuranceRecord entity) {
        if (entity.getPremiumAmount() == null || entity.getPremiumFrequency() == null) {
            return null;
        }
        BigDecimal amount = entity.getPremiumAmount();
        return switch (entity.getPremiumFrequency()) {
            case WEEKLY         -> amount.multiply(BigDecimal.valueOf(52)).setScale(2, RoundingMode.HALF_UP);
            case MONTHLY        -> amount.multiply(BigDecimal.valueOf(12)).setScale(2, RoundingMode.HALF_UP);
            case BIMONTHLY      -> amount.multiply(BigDecimal.valueOf(6)).setScale(2, RoundingMode.HALF_UP);
            case QUARTERLY      -> amount.multiply(BigDecimal.valueOf(4)).setScale(2, RoundingMode.HALF_UP);
            case SEMIANNUALLY   -> amount.multiply(BigDecimal.valueOf(2)).setScale(2, RoundingMode.HALF_UP);
            case ANNUALLY       -> amount.setScale(2, RoundingMode.HALF_UP);
            case SINGLE_PREMIUM -> amount.setScale(2, RoundingMode.HALF_UP);
            case UNKNOWN        -> null;
        };
    }
}