
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

export interface LocationResponse {
    externalId: string;
    name: string;
    type: LocationType;
    isoCode: string | null;
    parentExternalId: string | null;
    parentName: string | null;
    clearanceLevel: ClearanceLevel;
    active: boolean;
    latitude: number | null;
    longitude: number | null;
    hasChildren: boolean;
}

export interface LocationRequest {
    name: string;
    type: LocationType;
    isoCode?: string;
    parentExternalId?: string;
    clearanceLevel?: ClearanceLevel;
    latitude?: number;
    longitude?: number;
}

export interface Page<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number; // aktuální stránka
    first: boolean;
    last: boolean;
    empty: boolean;
}

export type HierarchicalLevel = 'NATIONAL' | 'REGIONAL' | 'LOCAL' | 'INTERNATIONAL' | 'SUB_LOCAL';
export type InstitutionType = 'EXECUTIVE' | 'LEGISLATIVE' | 'JUDICIAL' | 'MILITARY' | 'INTELLIGENCE' | 'REGULATORY' | 'NGO' | 'PRIVATE';
export type OccupationCategory = 'GOVERNANCE' | 'EXECUTIVE' | 'SPECIALIST' | 'OPERATIONAL' | 'TECHNICAL';
export type EducationLevel = 'SECONDARY' | 'HIGHER_VOCATIONAL' | 'BACHELOR' | 'MASTER' | 'DOCTORATE';
export type ClearanceLevel = 'LEVEL_1_PUBLIC' | 'LEVEL_2_INTERNAL' | 'LEVEL_3_CONFIDENTIAL' | 'LEVEL_4_SECRET' | 'LEVEL_5_TOP_SECRET';

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
    locationId: string;
    locationName: string;
    fullLocationPath: LocationResponse[] | null;
    logoUrl: string | null;
    websiteUrl: string | null;
}

export interface IntelligenceReportResponse {
    reportId: string;
    personId: string;
    personFullName: string;
    analysis: string;
    generatedAt: string;
    aiModel: string;
    riskLevel: number;
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
    currentLocationId: string;
    currentLocationName: string;
    politicalAffiliation: string | null;
    birthDate: string; 
    deathDate: string | null;
    gender: 'MALE' | 'FEMALE' | 'OTHER';
    nationality: string;
    placeOfBirth: string;
    age: number;
    clearanceLevel: ClearanceLevel;
}

export interface AgentOnboardingRequest {
    codename: string;
    officialEmail: string;
    assignedLevel: ClearanceLevel;
    requiresFieldAccess: boolean;
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

export interface AppointmentRequest {
    personPublicId: string;
    occupationPublicId: string;
    startDate: string;
    endDate?: string | null;
    monthlySalary: number;
    monthlyLumpSumAllowance: number;
    isActing: boolean;
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
    isActing: boolean;
    appointmentNote?: string;
    personPhotoUrl: string | null;
    monthlySalary: number;
    monthlyLumpSumAllowance: number;
    currency: string;
    benefitDetails: BenefitDetails | null;
}

export interface BenefitDetails {
    housingType: 'NONE' | 'STATE_RESIDENCE' | 'ALLOWANCE' | 'SOCIAL_SUPPORT';
    officialCarWithDriver: boolean;
    securityDetail: boolean;
    travelBudget: number;
    diplomaticPassport: boolean;
}

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
