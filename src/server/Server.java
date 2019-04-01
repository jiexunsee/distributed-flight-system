package server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONObject;

import shared.Callback;

// need some timer so as to turn off expired callbacks.

public class Server {
    private ArrayList<Flight> flights;
    private HashMap<Integer, ArrayList<Callback>> flightCallbacks;

    public Server(ArrayList<Flight> flights) {
        this.flights = flights;
        this.flightCallbacks = new HashMap<>();
    }

    public ArrayList<Integer> getFlightsBySourceDestination (String source, String destination) {
        ArrayList<Integer> flightIds = new ArrayList<>();
        for (Flight f : this.flights) {
            if (source.equalsIgnoreCase(f.getSource()) && destination.equalsIgnoreCase(f.getDestination())) {
                flightIds.add(f.getFlightId());
            }
        }
        return flightIds;
    }

    // return flight details in string to be unpacked, or in class, or json, or some other way?
    public JSONObject getFlightDetails (int flightId) {
        JSONObject jo = new JSONObject();
        for (Flight f : this.flights) {
            if (flightId == f.getFlightId()) {
                jo.put("departure time", f.getDepartureTime());
                jo.put("airfare", f.getAirfare());
                jo.put("seat availability", f.getAvailability());
                return jo;
            }
        }
        return null;
    }

    public Boolean reserveSeatsForFlight (int flightId, int numReserve) {
        Flight f = this.getFlightById(flightId);
        Boolean ack = f.reserveSeats(numReserve);

        if (ack) {
            // do callback action for clients that are monitoring this flight
            int availability = f.getAvailability();
            this.sendUpdates(flightId, availability);
        }
        return ack;
    }

    private void sendUpdates (int flightId, int availability) {
        ArrayList<Callback> callbacks = this.flightCallbacks.get(flightId); // get the callbacks for this flight
        long currentTime = System.currentTimeMillis();
        Iterator iterator = callbacks.iterator();
        while (iterator.hasNext()) {
            Callback callback = (Callback) iterator.next();
            if (callback.hasExpired(currentTime)) { // expired callbacks are only removed when there is a potential update to be sent.
                iterator.remove();
                System.out.println("Callback removed for client with address " + callback.getInetSocketAddress().toString());
            }
            else {
                callback.update(availability);
            }
        }
    }

    public void registerCallback (int flightId, int duration, InetSocketAddress inetSocketAddress) {
        long expiry = System.currentTimeMillis() + (duration * 1000);
        Callback callback = new Callback(flightId, expiry, inetSocketAddress);
        if (!this.flightCallbacks.containsKey(flightId)) {
            this.flightCallbacks.put(flightId, new ArrayList<>());
        }
        ArrayList<Callback> callbacks = this.flightCallbacks.get(flightId);
        callbacks.add(callback);
    }

//    public void registerMonitorAvailabilityCallback(Callback cbObject) throws Exception {
//        this.callbacks.add(cbObject);
//    }
//
//    public void deregisterMonitorAvailabilityCallback(Callback cbObject) throws Exception {
//
//    }

    private Flight getFlightById (int flightId) {
        for (Flight f : this.flights) {
            if (flightId == f.getFlightId()) {
                return f;
            }
        }
        return null;
    }
}
