package com.coaxial.tspweb.io.reqRep;

/**
 * Đại diện cho các thuộc tính năng lượng của phương tiện đã sử dụng, tức là
 * tiêu thụ thêm trên đường dốc
 */
public class Energy { // năng lượng
	/**
	 * Mức tiêu thụ chính của xe, tính bằng kWh / km
	 */
	private double consumption;

	/**
	 * Có nên nội suy tuyến tính về mức tiêu thụ năng lượng bổ sung và khả năng hồi
	 * phục hay không thay vì sử dụng các ngưỡng đã cho làm giá trị giới hạn.
	 */
	private boolean interpolation;

	/**
	 * Độ dốc dương từ đó xe hao xăng hơn.
	 */
	private SlopeEnergy additional;

	/**
	 * Độ dốc âm từ đó xe ít tiêu hao điện năng hơn.
	 */
	private SlopeEnergy recuperation;

	public Energy(double consumption, boolean interpolation, SlopeEnergy additional, SlopeEnergy recuperation) {
		this.consumption = consumption;
		this.interpolation = interpolation;
		this.additional = additional;
		this.recuperation = recuperation;
	}

	public double getConsumption() {
		return consumption;
	}

	public void setConsumption(double consumption) {
		this.consumption = consumption;
	}

	public boolean isInterpolation() {
		return interpolation;
	}

	public void setInterpolation(boolean interpolation) {
		this.interpolation = interpolation;
	}

	public SlopeEnergy getAdditional() {
		return additional;
	}

	public void setAdditional(SlopeEnergy additional) {
		this.additional = additional;
	}

	public SlopeEnergy getRecuperation() {
		return recuperation;
	}

	public void setRecuperation(SlopeEnergy recuperation) {
		this.recuperation = recuperation;
	}

	@Override
	public String toString() {
		return "Energy{" + "consumption=" + consumption + ", interpolation=" + interpolation + ", additional="
				+ additional + ", recuperation=" + recuperation + '}';
	}
}
