package com.whim.ttr.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * The official Ticket to Ride: Europe destination-ticket list (short tickets
 * plus the six high-value long routes). All endpoints reference {@link City}
 * ids defined by {@link Board}. Point values follow the printed cards; a few
 * are approximate where the physical value was uncertain (noted in the PR).
 */
final class Tickets {

    private Tickets() { }

    static List<DestinationTicket> official() {
        List<DestinationTicket> t = new ArrayList<DestinationTicket>();

        // ---- long routes (high value) ----
        t.add(new DestinationTicket("Cadiz", "Stockholm", 21));
        t.add(new DestinationTicket("Edinburgh", "Athina", 21));
        t.add(new DestinationTicket("Kobenhavn", "Erzurum", 21));
        t.add(new DestinationTicket("Brest", "Petrograd", 20));
        t.add(new DestinationTicket("Lisboa", "Danzig", 20));
        t.add(new DestinationTicket("Palermo", "Moskva", 20));

        // ---- short routes ----
        t.add(new DestinationTicket("Amsterdam", "Pamplona", 7));
        t.add(new DestinationTicket("Amsterdam", "Wilno", 12));
        t.add(new DestinationTicket("Angora", "Kharkov", 10));
        t.add(new DestinationTicket("Athina", "Angora", 5));
        t.add(new DestinationTicket("Athina", "Wilno", 11));
        t.add(new DestinationTicket("Barcelona", "Bruxelles", 8));
        t.add(new DestinationTicket("Barcelona", "Munchen", 8));
        t.add(new DestinationTicket("Berlin", "Bucuresti", 8));
        t.add(new DestinationTicket("Berlin", "Moskva", 12));
        t.add(new DestinationTicket("Berlin", "Roma", 9));
        t.add(new DestinationTicket("Brest", "Marseille", 7));
        t.add(new DestinationTicket("Brest", "Venezia", 8));
        t.add(new DestinationTicket("Bruxelles", "Danzig", 9));
        t.add(new DestinationTicket("Budapest", "Sofia", 5));
        t.add(new DestinationTicket("Edinburgh", "Paris", 12));
        t.add(new DestinationTicket("Essen", "Kyiv", 10));
        t.add(new DestinationTicket("Frankfurt", "Kobenhavn", 5));
        t.add(new DestinationTicket("Frankfurt", "Smolensk", 13));
        t.add(new DestinationTicket("Kyiv", "Petrograd", 6));
        t.add(new DestinationTicket("Kyiv", "Sochi", 8));
        t.add(new DestinationTicket("London", "Berlin", 7));
        t.add(new DestinationTicket("London", "Wien", 10));
        t.add(new DestinationTicket("Madrid", "Dieppe", 8));
        t.add(new DestinationTicket("Madrid", "Zurich", 10));
        t.add(new DestinationTicket("Marseille", "Essen", 8));
        t.add(new DestinationTicket("Palermo", "Constantinople", 8));
        t.add(new DestinationTicket("Paris", "Wien", 8));
        t.add(new DestinationTicket("Paris", "Zagreb", 7));
        t.add(new DestinationTicket("Riga", "Bucuresti", 10));
        t.add(new DestinationTicket("Roma", "Smyrna", 8));
        t.add(new DestinationTicket("Rostov", "Erzurum", 5));
        t.add(new DestinationTicket("Sarajevo", "Sevastopol", 8));
        t.add(new DestinationTicket("Smolensk", "Rostov", 8));
        t.add(new DestinationTicket("Sofia", "Smyrna", 5));
        t.add(new DestinationTicket("Stockholm", "Wien", 11));
        t.add(new DestinationTicket("Venezia", "Constantinople", 10));
        t.add(new DestinationTicket("Warszawa", "Smolensk", 6));
        t.add(new DestinationTicket("Zagreb", "Brindisi", 6));
        t.add(new DestinationTicket("Zurich", "Budapest", 6));
        t.add(new DestinationTicket("Zurich", "Brindisi", 6));

        return t;
    }
}
