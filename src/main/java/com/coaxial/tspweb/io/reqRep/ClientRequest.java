package com.coaxial.tspweb.io.reqRep;

import com.google.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Đại diện cho tất cả dữ liệu có nguồn gốc từ máy khách.
 */
public class ClientRequest {
	/**
	 * Danh sách các vị trí mà TSP sẽ được giải quyết.
	 */
	private ArrayList<LatLng> locations;

	/**
	 * Các đặc tính năng lượng của xe.
	 */
	private Energy energy;

	public ClientRequest(ArrayList<LatLng> locations, Energy energy) {
		this.locations = locations;
		this.energy = energy;
	}

	public ArrayList<LatLng> getLocations() {
		return locations;
	}

	public void setLocations(ArrayList<LatLng> locations) {
		this.locations = locations;
	}

	public Energy getEnergy() {
		return energy;
	}

	public void setEnergy(Energy energy) {
		this.energy = energy;
	}

	@Override
	public String toString() {
		return "ClientRequest{" + "locations=" + locations + ", energy=" + energy + '}';
	}
}
