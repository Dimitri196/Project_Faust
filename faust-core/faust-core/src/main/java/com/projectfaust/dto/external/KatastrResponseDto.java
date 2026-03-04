package com.projectfaust.dto.external;


/*
Katastr API response for relevant data (persons) related to a specific property. The response contains a list of persons, each with their name, surname, and role (e.g., owner, tenant). This information is crucial for understanding the ownership and occupancy of the property in question.
Data cell based on person Id, resp. RČ or something similar to that pattern of government data collection (non-official, secured db collections).

 */
public record KatastrResponseDto() {
}
