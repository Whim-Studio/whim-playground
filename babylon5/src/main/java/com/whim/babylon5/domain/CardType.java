package com.whim.babylon5.domain;

/**
 * Card types used by this prototype. The rulebook ("Anatomy of a Card" /
 * "Card Details") defines Character, Group, Fleet, Location, Enhancement,
 * Event, Agenda, Conflict, Aftermath and Contingency cards. This adaptation
 * collapses them to the subset the engine reasons about:
 *
 *  - AMBASSADOR : the faction leader (a Starting Ambassador character).
 *  - CHARACTER  : ordinary character cards (sponsored into Supporting/Inner Circle).
 *  - CONFLICT   : conflict cards initiated during the Conflict round.
 *  - AFTERMATH  : aftermath cards played in the Resolution round.
 *  - AGENDA     : faction agenda cards.
 *  - LOCATION   : location cards (military opposition to war conflicts).
 *  - SUPPORT    : groups / fleets / enhancements — generic supporting cards.
 */
public enum CardType { AMBASSADOR, CHARACTER, CONFLICT, AFTERMATH, AGENDA, LOCATION, SUPPORT }
