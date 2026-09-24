// =========================================================================
// LOCATION TYPES — corrected against backend LocationRequest/LocationResponse
// =========================================================================
 
export type LocationType =
    | 'CONTINENT'
    | 'COUNTRY'
    | 'PROVINCE'
    | 'DISTRICT'
    | 'CITY'
    | 'SUBDIVISION_L1'
    | 'SUBDIVISION_L2'
    | 'FACILITY'
    | 'ZONE'
    | 'SUBLOCATION';
 
// NEW: matches com.projectfaust.shared.enums.LocationSourceType exactly.
// Used to render source icons/badges (e.g. "imported from OSM" vs "manual entry").
export type LocationSourceType =
    | 'MANUAL'     // entered by an operator directly
    | 'OVERPASS'   // imported from OpenStreetMap via Overpass API
    | 'KATASTR'    // imported from Czech land registry
    | 'GEONAMES'   // imported from Geonames API
    | 'HUMINT'     // provided by a human source
    | 'SEED';      // loaded from locations.json seed file
 
export interface LocationResponse {
    externalId: string;
    name: string;
    localName: string | null;
    type: LocationType;
    isoCode: string | null;
    parentExternalId: string | null;
    parentName: string | null;
    clearanceLevel: ClearanceLevel;
    // NEW: evidentiary provenance — render trust badges / DECEPTION_MARKER warnings.
    // VerificationStatus is already defined elsewhere in this file.
    verificationStatus: VerificationStatus;
    // NEW: ingestion channel — render source icon (OSM, Katastr, Geonames, etc.).
    sourceType: LocationSourceType;
    active: boolean;
    latitude: number | null;
    longitude: number | null;
    hasChildren: boolean;
    // NEW: data freshness — backend Javadoc notes this is "critical for
    // assessing intelligence currency".
    createdAt: string;
    updatedAt: string;
}
 
export interface LocationRequest {
    name: string;
    type: LocationType;
    isoCode?: string;
    parentExternalId?: string;
    clearanceLevel?: ClearanceLevel;
    // NEW: optional — ADMIN can override on creation (e.g. seeding OFFICIAL_REGISTRY data).
    // Defaults to PENDING_REVIEW server-side if omitted.
    verificationStatus?: VerificationStatus;
    // NEW: optional — defaults to MANUAL server-side if omitted.
    sourceType?: LocationSourceType;
    latitude?: number;
    longitude?: number;
}

export interface LocationFilter {
    query?: string;
    type?: string;
    parentId?: string;
    rootOnly?: boolean;
    // Optional, for map bounding box queries
    north?: number;
    south?: number;
    east?: number;
    west?: number;
    maxClearance?: string;
}

export interface Page<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number; // current page index
    first: boolean;
    last: boolean;
    empty: boolean;
}

export type HierarchicalLevel = 'NATIONAL' | 'REGIONAL' | 'LOCAL' | 'INTERNATIONAL' | 'SUB_LOCAL';
export type InstitutionType = 'EXECUTIVE' | 'LEGISLATIVE' | 'JUDICIAL' | 'MILITARY' | 'INTELLIGENCE' | 'REGULATORY' | 'NGO' | 'PRIVATE' | 'SOVEREIGN' | 'STATE_ENTERPRISE';
export type OccupationCategory = 'GOVERNANCE' | 'EXECUTIVE' | 'SPECIALIST' | 'OPERATIONAL' | 'TECHNICAL';
export type EducationLevel = 'SECONDARY' | 'HIGHER_VOCATIONAL' | 'BACHELOR' | 'MASTER' | 'DOCTORATE';
export type ClearanceLevel = 'LEVEL_1_PUBLIC' | 'LEVEL_2_INTERNAL' | 'LEVEL_3_CONFIDENTIAL' | 'LEVEL_4_SECRET' | 'LEVEL_5_TOP_SECRET';
export type NameType = 'LEGAL' | 'ALIAS' | 'PSEUDONYM' | 'MAIDEN' | 'HISTORICAL' | 'RELIGIOUS';

// =========================================================================
// USER / AUTH ENUMS
// Must match com.projectfaust.shared.enums.UserRole / UserStatus exactly.
// =========================================================================
export type UserRole = 'VIEWER' | 'ANALYST' | 'ADMIN' | 'SUPER_ADMIN';
export type UserStatus = 'OPERATIONAL' | 'SUSPENDED' | 'INACTIVE' | 'PENDING_ACTIVATION';

// =========================================================================
// TELEMETRY AND TECHNICAL FOOTPRINT ENUMS
// =========================================================================
export type ContactType =
    | 'CELLULAR_GSM'
    | 'VOIP'
    | 'SATELLITE_TERMINAL'
    | 'THREEMA'
    | 'SIGNAL'
    | 'MATRIX_IDENTITY'
    | 'SESSION_ID'
    | 'COMMERCIAL_IM'
    | 'EMAIL'
    | 'IP_ADDRESS'
    | 'DARKNET_ENDPOINT'
    | 'CRYPTO_WALLET'
    | 'TACTICAL_RF'
    | 'COVERT_PHYSICAL_DROP';

export type VerificationStatus =
    | 'OFFICIAL_REGISTRY'
    | 'TECHNICAL_INTERCEPT'
    | 'VETTED_HUMINT'
    | 'VERIFIED_OSINT'
    | 'UNVERIFIED'
    | 'DECEPTION_MARKER'
    | 'EXPIRED_DEPRECATING'
    | 'PENDING_REVIEW'
    | 'CONFLICTING';

export interface InstitutionResponse {
    publicId: string;
    name: string;
    level: HierarchicalLevel;
    type: InstitutionType;
    parentId: string | null;
    countryCode: string;
    hasChildren: boolean;
    /** Backend field: stateOwned (renamed from isStateOwned). */
    stateOwned: boolean;
    description: string;
    locationId: string;
    locationName: string;
    fullLocationPath: LocationResponse[] | null;
    verificationStatus?: VerificationStatus;
    clearanceLevel: ClearanceLevel;
    logoUrl: string | null;
    websiteUrl: string | null;
    active: boolean;
}

export interface IntelligenceReportResponse {
    reportId: string;
    personId: string;
    personFullName: string;
    /** Backend field: analysisResult */
    analysisResult: string;
    generatedAt: string;
    /** Backend field: modelVersion */
    modelVersion: string;
    /** Backend field: riskScore (nullable Integer) */
    riskScore: number | null;
}

export interface InstitutionTreeResponse {
    publicId: string;
    name: string;
    level: HierarchicalLevel;
    type: InstitutionType;
    description: string;
    /** Backend field: stateOwned (renamed from isStateOwned). */
    stateOwned: boolean;
    children: InstitutionTreeResponse[];
    hasChildren: boolean;
    /** Backend field: parentId (UUID), not a nested object — prevents infinite recursion. */
    parentId: string | null;
    active: boolean;
    logoUrl: string | null;
    verificationStatus: VerificationStatus;
}

export interface InstitutionAscendedResponse {
    publicId: string;
    name: string;
    level: HierarchicalLevel;
    type: InstitutionType;
    description: string;
    /** Backend field: stateOwned (renamed from isStateOwned). */
    stateOwned: boolean;
    parent: InstitutionAscendedResponse | null;
    verificationStatus: VerificationStatus;
}

export interface PersonNameDto {
    firstName: string;
    lastName: string;
    type: NameType;
    /** Backend field: primary (renamed from isPrimary). */
    primary: boolean;
    validFrom: string | null;
    validTo: string | null;
    note: string | null;
}

// =========================================================================
// TELEMETRY STRUCTURE FOR GRID AND GRAPH ANALYSIS
// =========================================================================
export interface PersonContactDto {
    publicId: string;
    contactType: ContactType;
    contactValueRaw: string;
    contactValueNormalized: string;
    operatorName: string | null;
    imei: string | null;
    verificationStatus: VerificationStatus;
    confidenceScore: number;
    clearanceLevel: ClearanceLevel;
    /** Backend field: active (renamed from isActive). */
    active: boolean;
    validFrom: string | null;
    validTo: string | null;
    analyticalNote: string | null;
}

export interface PersonResponse {
    publicId: string;
    firstName: string;
    lastName: string;
    titleBefore: string | null;
    titleAfter: string | null;
    displayName: string;
    educationLevel: EducationLevel;
    fieldOfStudy: string | null;
    biography: string | null;
    photoUrl: string | null;
    currentLocationId: string | null;
    currentLocationName: string | null;
    politicalAffiliation: string | null;
    birthDate: string;
    deathDate: string | null;
    gender: 'MALE' | 'FEMALE' | 'OTHER';
    nationality: string;
    placeOfBirth: string;
    age: number;
    clearanceLevel: ClearanceLevel;
    verificationStatus: VerificationStatus;
    nameHistory: PersonNameDto[];
    contactHistory: PersonContactDto[];
    primaryEmail: string | null;
    primaryPhone: string | null;
    currentPositions: AppointmentResponse[];
}

export interface OccupationResponse {
    publicId: string;
    title: string;
    code: string;
    category: OccupationCategory;
    description: string;
    institutionName: string;
    institutionPublicId: string;
    supervisorTitle: string | null;
    reportsToPublicId: string | null;
    /** Backend field: vacant (renamed from isVacant). */
    vacant: boolean;
    rank: string;
    currentOccupantName: string | null;
    currentOccupantId: string | null;
    verificationStatus: VerificationStatus;
    requiredClearanceLevel: ClearanceLevel;
}

export interface OccupationTreeResponse {
    publicId: string;
    title: string;
    code: string;
    category: OccupationCategory;
    /** NEW: minimum clearance required for this position. */
    requiredClearanceLevel: ClearanceLevel;
    /** Backend field: vacant (renamed from isVacant). */
    vacant: boolean;
    /** NEW: whether the position is currently in the org structure. */
    active: boolean;
    rank: string;
    currentOccupantName: string | null;
    /** NEW: public UUID of the current holder; null if vacant. */
    currentOccupantId: string | null;
    /** NEW: public UUID of the direct superior position; null for top-level. */
    reportsToPublicId: string | null;
    subordinates: OccupationTreeResponse[];
    personPublicId: string | null;
    /** NEW: evidentiary provenance state of this record. */
    verificationStatus: VerificationStatus;
}

export interface AppointmentRequest {
    personPublicId: string;
    occupationPublicId: string;
    startDate: string;
    endDate?: string | null;
    monthlySalary: number;
    monthlyLumpSumAllowance: number;
    /** Backend field: acting (renamed from isActing). */
    acting: boolean;
    /** Backend field: exOffoAccess (renamed from isExOffoAccess). */
    exOffoAccess: boolean;
    benefitDetails: BenefitDetails;
    appointmentNote?: string;
}

export interface AppointmentResponse {
    publicId: string;
    personDisplayName: string;
    personPublicId: string;
    occupationTitle: string;
    occupationPublicId: string;
    startDate: string;
    endDate: string | null;
    /** Backend field: acting (renamed from isActing). */
    acting: boolean;
    /** Backend field: exOffoAccess (renamed from isExOffoAccess). */
    exOffoAccess: boolean;
    appointmentNote?: string;
    personPhotoUrl: string | null;
    monthlySalary: number;
    monthlyLumpSumAllowance: number;
    currency: string;
    benefitDetails: BenefitDetails | null;
    verificationStatus: VerificationStatus;
}

export interface BenefitDetails {
    housingType: 'NONE' | 'STATE_RESIDENCE' | 'ALLOWANCE' | 'SOCIAL_SUPPORT';
    officialCarWithDriver: boolean;
    securityDetail: boolean;
    travelBudget: number;
    diplomaticPassport: boolean;
}

// =========================================================================
// AUTH / USER PROFILE
// =========================================================================

/**
 * Response from POST /api/v1/auth/login.
 * Matches backend com.projectfaust.auth.dto.AuthResponse exactly.
 */
export interface AuthResponse {
    /** JWT token — send as `Authorization: Bearer <token>` on all subsequent requests. */
    token: string;
    /** Token lifetime in milliseconds (default 86400000 = 24h). */
    expiresIn: number;
    userId: string;
    email: string;
    fullName: string;
    role: UserRole;
    clearance: ClearanceLevel;
}

/**
 * Request body for POST /api/v1/auth/login.
 * Matches backend com.projectfaust.auth.dto.LoginRequest.
 */
export interface LoginRequest {
    email: string;
    password: string;
}

/**
 * Operator profile — matches backend com.projectfaust.user.dto.ProfileResponse.
 *
 * Field renames vs older version:
 *  - isAdmin -> admin   (Lombok double-prefix fix on the entity)
 *  - role: string -> UserRole
 *  - status: string -> UserStatus
 *  - added createdAt / updatedAt
 */
export interface UserProfile {
    id: string;
    fullName: string;
    email: string;
    role: UserRole;
    clearance: ClearanceLevel;
    status: UserStatus;
    techStack: string[];
    /** Backend field: admin (renamed from isAdmin). */
    admin: boolean;
    createdAt: string;
    updatedAt: string;
}

/**
 * Request body for onboarding a new operator — matches backend
 * com.projectfaust.user.dto.AgentOnboardingRequest.
 *
 * Completely redesigned vs the old version:
 *  - requiresFieldAccess: boolean removed (FIELD_OPERATIVE role no longer exists)
 *  - initialRole: UserRole added — explicit role selection
 *  - temporaryPassword added — service hashes this with BCrypt before storage
 */
export interface AgentOnboardingRequest {
    codename: string;
    officialEmail: string;
    assignedLevel: ClearanceLevel;
    initialRole: UserRole;
    temporaryPassword: string;
}

export type SearchCategory = 'PERSON' | 'INSTITUTION' | 'OCCUPATION' | 'LOCATION';
 
export interface GlobalSearchResponse {
    id: string;
    displayName: string;
    /** Backend returns a raw String; SearchCategory covers known values with a fallback. */
    category: SearchCategory | string;
    subLabel: string;
    rankScore: number;
}

export type UpdateProfileRequest = Partial<{
    fullName: string;
    role: UserRole;
    clearance: ClearanceLevel;
    status: UserStatus;
    techStack: string[];
}>

export type IdentifierType =
    | 'NATIONAL_REGISTRATION'  // IČO (CZ), SIREN (FR), REGON (PL), CRN (UK) etc.
    | 'VAT'                    // DIČ (CZ), Umsatzsteuer-ID (DE) etc.
    | 'LEI'                    // ISO 17442 — global, no country
    | 'DUNS'                   // Dun & Bradstreet — global, no country
    | 'EUID'                   // EU Unique Identifier — cross-border
    | 'GLEIF_RELATIONSHIP'     // GLEIF parent/child relationship map
    | 'CUSTOM';                // Source-specific; document in note field

export interface InstitutionIdentifierResponse {
    id: string;
    type: IdentifierType;
    value: string;
    /** ISO 3166-1 alpha-2 country code; null for global schemes (LEI, DUNS, EUID). */
    countryCode: string | null;
    note: string | null;
    active: boolean;
    createdAt: string;
}

export interface InstitutionIdentifierRequest {
    type: IdentifierType;
    value: string;
    /** ISO 3166-1 alpha-2 country code. Omit for global schemes (LEI, DUNS, EUID). */
    countryCode?: string;
    note?: string;
}

// =========================================================================
// EXTERNAL CONTRACTS
// Matches com.projectfaust.ingest.dto.ExternalContractResponse.
// Source: Hlidač Státu (CZ) and future procurement sources.
// =========================================================================

/** Whether the queried institution was the buyer, supplier, or both. */
export type ContractRole = 'BUYER' | 'SUPPLIER' | 'BOTH';

export interface ExternalContractResponse {
    id: string;
    /** Which external system this record was ingested from (e.g. HLIDAC_STATU_CZ). */
    sourceSystem: string;
    /** The record's ID within the source system. */
    externalId: string;
    buyerIco: string | null;
    buyerName: string | null;
    supplierIco: string | null;
    supplierName: string | null;
    /** Total contract value in the source currency (CZK for Hlidač Státu). */
    amountTotal: number | null;
    /** ISO date string (yyyy-MM-dd) — when the contract was confirmed/published. */
    contractDate: string | null;
    subjectText: string | null;
    verificationStatus: VerificationStatus;
    /** When this record was pulled into Faust — useful for assessing data freshness. */
    ingestedAt: string | null;
    /** Role of the queried institution in this contract — computed server-side. */
    role: ContractRole;
}