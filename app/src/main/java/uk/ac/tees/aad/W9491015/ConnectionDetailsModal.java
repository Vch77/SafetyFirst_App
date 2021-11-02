package uk.ac.tees.aad.W9491015;

public class ConnectionDetailsModal {

    private String name,email;
    double lat,lng;

    public ConnectionDetailsModal(){

    }

    public ConnectionDetailsModal(String name, String email,double lat, double lng) {
        this.name = name;
        this.email = email;
        this.lat = lat;
        this.lng = lng;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

}
