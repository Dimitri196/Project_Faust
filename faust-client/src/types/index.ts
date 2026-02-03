//src/types/index.ts

export type HierarchicalLevel = 'NATIONAL' | 'REGIONAL' | 'LOCAL' | 'INTERNATIONAL' | 'SUB_LOCAL';
export type InstitutionType = 'EXECUTIVE' | 'LEGISLATIVE' | 'JUDICIAL' | 'MILITARY' | 'INTELLIGENCE' |'REGULATORY';
export type OccupationCategory = 'POLITICAL' | 'CIVIL_SERVICE' | 'TECHNICAL' | 'CONTRACTUAL' | 'ADVISORY';
export type EducationLevel = 'SECONDARY' | 'HIGHER_VOCATIONAL' | 'BACHELOR' | 'MASTER' | 'DOCTORATE';
export type ClearanceLevel = 'LEVEL_1_PUBLIC' | 'LEVEL_2_INTERNAL' | 'LEVEL_3_CONFIDENTIAL' | 'LEVEL_4_SECRET' | 'LEVEL_5_TOP_SECRET';


// --- INSTITUTION ---
export interface InstitutionTreeResponse {
    publicId: string;
    name: string;
    level: HierarchicalLevel;
    type: InstitutionType;
    description: string;
    isStateOwned: boolean;
    children: InstitutionTreeResponse[];
}

export interface InstitutionResponse {
    publicId: string;
    name: string;
    countryCode: string;
    level: HierarchicalLevel;
    type: InstitutionType;
    parentId: string | null;
    hasChildren: boolean;
    isStateOwned: boolean;
    description: string;
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

// --- OCCUPATION ---
export interface OccupationResponse {
    publicId: string;
    title: string;
    code: string;
    category: OccupationCategory;
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

// --- PERSON ---
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
}

// --- APPOINTMENT ---
export interface AppointmentResponse {
    publicId: string;
    personDisplayName: string;
    personPublicId: string;
    occupationTitle: string;
    startDate: string; // ISO Date String
    endDate: string | null;
    isActing: boolean;
}

// --- USER ---
export interface UserProfile {
    id: string; 
    fullName: string;
    email: string;
    role: string;
    clearance: ClearanceLevel; // Použití nového typu
    status: string;
    techStack: string[];
    isAdmin: boolean;
}
