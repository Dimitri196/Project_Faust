/**
 * GEOGRAPHIC & ORGANIZATIONAL HIERARCHY
 */ 
export type LocationType = 
    | 'CONTINENT' 
    | 'COUNTRY' 
    | 'PROVINCE' 
    | 'DISTRICT' 
    | 'CITY' 
    | 'SUBDIVISION' 
    | 'FACILITY' 
    | 'ZONE' 
    | 'SUBLOCATION';

export interface LocationResponse {
    externalId: string;
    name: string;
    type: LocationType;
    isoCode: string | null;
    parentExternalId: string | null;
    parentName: string | null;
}

export interface LocationRequest {
    name: string;
    type: LocationType;
    isoCode?: string;
    parentExternalId?: string;
}

/**
 * COMMON TYPES & ENUMS
 */
export type HierarchicalLevel = 'NATIONAL' | 'REGIONAL' | 'LOCAL' | 'INTERNATIONAL' | 'SUB_LOCAL';
export type InstitutionType = 'EXECUTIVE' | 'LEGISLATIVE' | 'JUDICIAL' | 'MILITARY' | 'INTELLIGENCE' | 'REGULATORY' | 'NGO' | 'PRIVATE';
export type OccupationCategory = 'GOVERNANCE' | 'EXECUTIVE' | 'SPECIALIST' | 'OPERATIONAL' | 'TECHNICAL';
export type EducationLevel = 'SECONDARY' | 'HIGHER_VOCATIONAL' | 'BACHELOR' | 'MASTER' | 'DOCTORATE';
export type ClearanceLevel = 'LEVEL_1_PUBLIC' | 'LEVEL_2_INTERNAL' | 'LEVEL_3_CONFIDENTIAL' | 'LEVEL_4_SECRET' | 'LEVEL_5_TOP_SECRET';

/**
 * INSTITUTION DOMAIN
 */
export interface InstitutionResponse {
    publicId: string;
    name: string;
    level: HierarchicalLevel;
    type: InstitutionType;
    parentId: string | null;
    countryCode: string;
    hasChildren: boolean;
    isStateOwned: boolean;
    description: string;
    // Propojení s Location Engine
    locationId: string;
    locationName: string;
    fullLocationPath: LocationResponse[] | null;
    logoUrl: string | null;
    websiteUrl: string | null;
}

export interface InstitutionTreeResponse {
    publicId: string;
    name: string;
    level: HierarchicalLevel;
    type: InstitutionType;
    description: string;
    isStateOwned: boolean;
    children: InstitutionTreeResponse[];
}

export interface InstitutionAscendedResponse {
    publicId: string;
    name: string;
    level: HierarchicalLevel;
    type: InstitutionType;
    description: string;
    isStateOwned: boolean;
    parent: InstitutionAscendedResponse | null;
}

/**
 * PERSON & AGENT DOMAIN
 */
export interface PersonResponse {
    publicId: string;
    firstName: string;
    lastName: string;
    titleBefore: string | null;
    titleAfter: string | null;
    displayName: string;
    educationLevel: EducationLevel;
    fieldOfStudy: string;
    email: string;
    phone: string;
    biography: string;
    photoUrl: string;
    // Propojení s Location Engine (působiště/adresa)
    currentLocationId: string;
    currentLocationName: string;
    politicalAffiliation: string | null;
}

export interface AgentOnboardingRequest {
    codename: string;
    officialEmail: string;
    assignedLevel: ClearanceLevel;
    requiresFieldAccess: boolean;
}

/**
 * OCCUPATION & APPOINTMENT DOMAIN
 */
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
    isVacant: boolean;
    rank: string;
}

export interface OccupationTreeResponse {
    publicId: string;
    title: string;
    code: string;
    category: OccupationCategory;
    isVacant: boolean;
    rank: string;
    currentOccupantName: string | null;
    subordinates: OccupationTreeResponse[];
}

export interface AppointmentResponse {
    publicId: string;
    personDisplayName: string;
    personPublicId: string;
    occupationTitle: string;
    occupationPublicId: string;
    startDate: string;
    endDate: string | null;
    isActing: boolean;
    appointmentNote?: string;
    personPhotoUrl: string | null;
    
    // --- NOVÁ FINANČNÍ POLE ---
    monthlySalary: number;
    monthlyLumpSumAllowance: number;
    currency: string;

    // --- STRUKTUROVANÉ BENEFITY ---
    benefitDetails: BenefitDetails | null;
}

export interface BenefitDetails {
    housingType: 'NONE' | 'STATE_RESIDENCE' | 'ALLOWANCE' | 'SOCIAL_SUPPORT';
    officialCarWithDriver: boolean;
    securityDetail: boolean;
    travelBudget: number;
    diplomaticPassport: boolean;
}

/**
 * IDENTITY & ACCESS MANAGEMENT
 */
export interface UserProfile {
    id: string;
    fullName: string;
    email: string;
    role: string;
    clearance: ClearanceLevel;
    status: string;
    techStack: string[];
    isAdmin: boolean;
}

export type UpdateProfileRequest = Partial<{
    fullName: string;
    role: string;
    clearance: ClearanceLevel;
    status: string;
    techStack: string[];
}>;
