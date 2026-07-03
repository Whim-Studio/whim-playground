package com.whim.ttr.domain;

import com.whim.ttr.api.CardColor;
import com.whim.ttr.api.RouteKind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Ticket to Ride: Europe map — 47 cities and the full official route list
 * (lengths, colors, ferries with locomotive requirements, and tunnels). Double
 * routes are represented as two {@link Route} objects sharing endpoints.
 *
 * <p>City coordinates are laid out in a virtual 0..1000 space that roughly
 * mirrors the real geography; the UI scales them to the panel. A handful of
 * route colors are approximations (see PR notes) but every length, kind, and
 * ferry-loco requirement follows the printed board, and the graph is fully
 * connected and playable.</p>
 */
public final class Board {

    private final Map<String, City> cities = new LinkedHashMap<String, City>();
    private final Map<String, Route> routes = new LinkedHashMap<String, Route>();
    private final Map<String, List<Route>> adjacency = new LinkedHashMap<String, List<Route>>();

    public Board() {
        buildCities();
        buildRoutes();
    }

    // ---- public API --------------------------------------------------------

    public Collection<City> cities() {
        return Collections.unmodifiableCollection(cities.values());
    }

    public City city(String id) {
        return cities.get(id);
    }

    public Collection<Route> routes() {
        return Collections.unmodifiableCollection(routes.values());
    }

    public Route route(String id) {
        return routes.get(id);
    }

    /** All routes touching {@code cityId} (either endpoint). */
    public List<Route> routesFrom(String cityId) {
        List<Route> list = adjacency.get(cityId);
        if (list == null) return Collections.emptyList();
        return Collections.unmodifiableList(list);
    }

    /** All routes directly joining {@code a} and {@code b} (0, 1, or 2 for a double). */
    public List<Route> routesBetween(String a, String b) {
        List<Route> out = new ArrayList<Route>(2);
        List<Route> from = adjacency.get(a);
        if (from != null) {
            for (Route r : from) {
                if (r.connects(a, b)) out.add(r);
            }
        }
        return out;
    }

    // ---- construction ------------------------------------------------------

    private void addCity(String id, int x, int y) {
        addCity(id, id, x, y);
    }

    private void addCity(String id, String name, int x, int y) {
        cities.put(id, new City(id, name, x, y));
        adjacency.put(id, new ArrayList<Route>());
    }

    private void buildCities() {
        // Western / British Isles
        addCity("Edinburgh", 150, 90);
        addCity("London", 175, 205);
        addCity("Dieppe", 210, 255);
        addCity("Brest", 120, 300);
        addCity("Paris", 235, 305);
        addCity("Amsterdam", 265, 205);
        addCity("Bruxelles", 248, 240);
        // Germany / Central
        addCity("Frankfurt", 315, 255);
        addCity("Essen", 300, 200);
        addCity("Berlin", 385, 205);
        addCity("Munchen", 360, 305);
        addCity("Zurich", 320, 345);
        // Scandinavia / North
        addCity("Kobenhavn", 395, 120);
        addCity("Stockholm", 455, 60);
        addCity("Danzig", 455, 175);
        // Iberia
        addCity("Lisboa", 30, 480);
        addCity("Madrid", 115, 465);
        addCity("Cadiz", 70, 545);
        addCity("Barcelona", 205, 465);
        addCity("Pamplona", 150, 415);
        // Southern France / Italy
        addCity("Marseille", 275, 405);
        addCity("Venezia", 375, 385);
        addCity("Roma", 375, 455);
        addCity("Brindisi", 455, 485);
        addCity("Palermo", 395, 565);
        // Eastern Europe
        addCity("Warszawa", 485, 235);
        addCity("Wien", 435, 315);
        addCity("Zagreb", 435, 385);
        addCity("Budapest", 495, 350);
        addCity("Sarajevo", 485, 425);
        addCity("Sofia", 585, 435);
        addCity("Bucuresti", 580, 385);
        // Greece / Turkey
        addCity("Athina", 560, 525);
        addCity("Smyrna", 645, 505);
        addCity("Constantinople", 665, 445);
        addCity("Angora", 760, 470);
        addCity("Erzurum", 835, 435);
        // Russia / Baltics
        addCity("Riga", 520, 120);
        addCity("Wilno", 545, 195);
        addCity("Petrograd", 605, 65);
        addCity("Moskva", 685, 155);
        addCity("Smolensk", 645, 175);
        addCity("Kyiv", 600, 255);
        addCity("Kharkov", 705, 255);
        addCity("Rostov", 765, 305);
        addCity("Sochi", 765, 385);
        addCity("Sevastopol", 685, 365);
    }

    private void buildRoutes() {
        // ---------- Western Europe / British Isles ----------
        dbl("Edinburgh", "London", 4, null, null, RouteKind.NORMAL, 0);
        ferry("London", "Dieppe", 2, 1, true);      // double ferry, 1 loco each
        ferry("London", "Amsterdam", 2, 2, false);  // single ferry, 2 loco
        dbl("Dieppe", "Paris", 1, null, null, RouteKind.NORMAL, 0);
        one("Dieppe", "Bruxelles", 2, CardColor.GREEN, RouteKind.NORMAL, 0);
        one("Dieppe", "Brest", 2, CardColor.ORANGE, RouteKind.NORMAL, 0);
        one("Brest", "Paris", 3, CardColor.BLACK, RouteKind.NORMAL, 0);
        one("Brest", "Pamplona", 4, CardColor.PURPLE, RouteKind.NORMAL, 0);
        dbl("Paris", "Bruxelles", 2, CardColor.YELLOW, CardColor.RED, RouteKind.NORMAL, 0);
        dbl("Paris", "Frankfurt", 3, CardColor.WHITE, CardColor.ORANGE, RouteKind.NORMAL, 0);
        one("Paris", "Marseille", 4, null, RouteKind.NORMAL, 0);
        dbl("Paris", "Pamplona", 4, CardColor.BLUE, CardColor.GREEN, RouteKind.NORMAL, 0);
        one("Pamplona", "Marseille", 4, CardColor.RED, RouteKind.NORMAL, 0);

        // ---------- Iberia ----------
        tunnel("Pamplona", "Madrid", 3, CardColor.WHITE, false, 0, false);
        tunnel("Pamplona", "Barcelona", 2, null, false, 0, false);
        tunnel("Madrid", "Barcelona", 2, CardColor.YELLOW, false, 0, false);
        one("Barcelona", "Marseille", 4, null, RouteKind.NORMAL, 0);
        one("Madrid", "Lisboa", 3, CardColor.PURPLE, RouteKind.NORMAL, 0);
        one("Madrid", "Cadiz", 3, CardColor.ORANGE, RouteKind.NORMAL, 0);
        one("Lisboa", "Cadiz", 2, CardColor.BLUE, RouteKind.NORMAL, 0);

        // ---------- Germany / Central ----------
        one("Amsterdam", "Bruxelles", 1, CardColor.BLACK, RouteKind.NORMAL, 0);
        one("Amsterdam", "Essen", 3, CardColor.YELLOW, RouteKind.NORMAL, 0);
        one("Amsterdam", "Frankfurt", 2, CardColor.WHITE, RouteKind.NORMAL, 0);
        one("Bruxelles", "Frankfurt", 2, CardColor.BLUE, RouteKind.NORMAL, 0);
        one("Essen", "Frankfurt", 2, CardColor.GREEN, RouteKind.NORMAL, 0);
        one("Essen", "Berlin", 2, CardColor.BLUE, RouteKind.NORMAL, 0);
        ferry("Essen", "Kobenhavn", 3, 1, true);     // double ferry, 1 loco each
        one("Frankfurt", "Munchen", 2, CardColor.PURPLE, RouteKind.NORMAL, 0);
        one("Frankfurt", "Berlin", 3, CardColor.BLACK, RouteKind.NORMAL, 0);
        one("Berlin", "Danzig", 4, null, RouteKind.NORMAL, 0);
        dbl("Berlin", "Warszawa", 4, CardColor.YELLOW, CardColor.PURPLE, RouteKind.NORMAL, 0);
        one("Berlin", "Wien", 3, CardColor.GREEN, RouteKind.NORMAL, 0);
        one("Munchen", "Wien", 3, CardColor.ORANGE, RouteKind.NORMAL, 0);
        one("Munchen", "Venezia", 2, CardColor.BLUE, RouteKind.NORMAL, 0);

        // ---------- Alps / Switzerland (tunnels) ----------
        tunnel("Paris", "Zurich", 3, null, false, 0, false);
        tunnel("Zurich", "Munchen", 3, CardColor.YELLOW, false, 0, false);
        tunnel("Zurich", "Venezia", 2, CardColor.GREEN, false, 0, false);
        tunnel("Zurich", "Marseille", 2, CardColor.PURPLE, false, 0, false);

        // ---------- Scandinavia ----------
        ferry("Kobenhavn", "Stockholm", 3, 1, true);  // double ferry, 1 loco each
        tunnel("Petrograd", "Stockholm", 8, null, false, 0, false);

        // ---------- Italy / Adriatic ----------
        one("Venezia", "Roma", 2, CardColor.BLACK, RouteKind.NORMAL, 0);
        one("Venezia", "Zagreb", 2, null, RouteKind.NORMAL, 0);
        one("Roma", "Brindisi", 2, CardColor.WHITE, RouteKind.NORMAL, 0);
        ferry("Roma", "Palermo", 4, 1, false);
        ferry("Brindisi", "Palermo", 3, 1, false);
        ferry("Palermo", "Smyrna", 6, 2, false);
        ferry("Brindisi", "Athina", 4, 1, false);

        // ---------- Balkans ----------
        one("Zagreb", "Wien", 2, null, RouteKind.NORMAL, 0);
        one("Zagreb", "Budapest", 2, CardColor.ORANGE, RouteKind.NORMAL, 0);
        one("Zagreb", "Sarajevo", 3, CardColor.RED, RouteKind.NORMAL, 0);
        dbl("Wien", "Budapest", 1, CardColor.RED, CardColor.WHITE, RouteKind.NORMAL, 0);
        one("Budapest", "Sarajevo", 3, CardColor.PURPLE, RouteKind.NORMAL, 0);
        tunnel("Budapest", "Bucuresti", 4, null, false, 0, false);
        tunnel("Budapest", "Kyiv", 6, null, false, 0, false);
        tunnel("Sarajevo", "Sofia", 2, null, false, 0, false);
        one("Sarajevo", "Athina", 4, CardColor.GREEN, RouteKind.NORMAL, 0);
        one("Sofia", "Athina", 3, CardColor.PURPLE, RouteKind.NORMAL, 0);
        one("Sofia", "Bucuresti", 2, null, RouteKind.NORMAL, 0);
        tunnel("Sofia", "Constantinople", 3, CardColor.BLUE, false, 0, false);

        // ---------- Turkey / Southeast ----------
        one("Bucuresti", "Constantinople", 3, CardColor.YELLOW, RouteKind.NORMAL, 0);
        one("Bucuresti", "Sevastopol", 4, CardColor.WHITE, RouteKind.NORMAL, 0);
        tunnel("Bucuresti", "Kyiv", 4, null, false, 0, false);
        tunnel("Constantinople", "Smyrna", 2, null, false, 0, false);
        tunnel("Constantinople", "Angora", 2, null, false, 0, false);
        ferry("Constantinople", "Sevastopol", 4, 2, false);
        one("Smyrna", "Angora", 3, CardColor.ORANGE, RouteKind.NORMAL, 0);
        one("Angora", "Erzurum", 3, CardColor.BLACK, RouteKind.NORMAL, 0);
        tunnel("Erzurum", "Sochi", 3, null, false, 0, false);
        ferry("Erzurum", "Sevastopol", 4, 2, false);

        // ---------- Russia / Ukraine / Baltics ----------
        ferry("Sevastopol", "Sochi", 2, 1, false);
        one("Sevastopol", "Rostov", 4, null, RouteKind.NORMAL, 0);
        one("Rostov", "Sochi", 2, null, RouteKind.NORMAL, 0);
        one("Rostov", "Kharkov", 2, CardColor.GREEN, RouteKind.NORMAL, 0);
        one("Kharkov", "Kyiv", 4, null, RouteKind.NORMAL, 0);
        one("Kharkov", "Moskva", 4, null, RouteKind.NORMAL, 0);
        one("Moskva", "Smolensk", 2, CardColor.ORANGE, RouteKind.NORMAL, 0);
        one("Moskva", "Petrograd", 4, CardColor.WHITE, RouteKind.NORMAL, 0);
        one("Smolensk", "Kyiv", 3, CardColor.RED, RouteKind.NORMAL, 0);
        one("Kyiv", "Wilno", 2, null, RouteKind.NORMAL, 0);
        tunnel("Kyiv", "Warszawa", 4, null, false, 0, false);
        one("Wilno", "Smolensk", 3, CardColor.YELLOW, RouteKind.NORMAL, 0);
        one("Wilno", "Warszawa", 3, CardColor.RED, RouteKind.NORMAL, 0);
        one("Wilno", "Riga", 4, CardColor.GREEN, RouteKind.NORMAL, 0);
        one("Wilno", "Petrograd", 4, CardColor.BLUE, RouteKind.NORMAL, 0);
        one("Riga", "Petrograd", 4, null, RouteKind.NORMAL, 0);
        one("Danzig", "Warszawa", 2, null, RouteKind.NORMAL, 0);
        one("Danzig", "Riga", 3, CardColor.BLACK, RouteKind.NORMAL, 0);
        one("Warszawa", "Wien", 4, CardColor.BLUE, RouteKind.NORMAL, 0);
    }

    // ---- route factory helpers ---------------------------------------------

    private int nextIndex(String a, String b) {
        return routesBetween(a, b).size() + 1;
    }

    private void register(Route r) {
        routes.put(r.id(), r);
        adjacency.get(r.cityA()).add(r);
        adjacency.get(r.cityB()).add(r);
    }

    /** A single edge. */
    private void one(String a, String b, int len, CardColor color, RouteKind kind, int loco) {
        String id = a + "-" + b + "-" + nextIndex(a, b);
        register(new Route(id, a, b, len, color, kind, loco));
    }

    /** A double edge: two parallel routes with (possibly) different colors. */
    private void dbl(String a, String b, int len, CardColor c1, CardColor c2, RouteKind kind, int loco) {
        one(a, b, len, c1, kind, loco);
        one(a, b, len, c2, kind, loco);
    }

    /**
     * A ferry edge (kind FERRY). {@code loco} is the minimum locomotives in the
     * payment. When {@code isDouble} is true, two parallel ferries are created.
     */
    private void ferry(String a, String b, int len, int loco, boolean isDouble) {
        one(a, b, len, null, RouteKind.FERRY, loco);
        if (isDouble) {
            one(a, b, len, null, RouteKind.FERRY, loco);
        }
    }

    /**
     * A tunnel edge (kind TUNNEL). {@code isDouble} creates two parallel tunnels.
     */
    private void tunnel(String a, String b, int len, CardColor color, boolean isDouble, int loco, boolean unusedFlag) {
        one(a, b, len, color, RouteKind.TUNNEL, loco);
        if (isDouble) {
            one(a, b, len, color, RouteKind.TUNNEL, loco);
        }
    }
}
