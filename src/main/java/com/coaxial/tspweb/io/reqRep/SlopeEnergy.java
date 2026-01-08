package com.coaxial.tspweb.io.reqRep;

/**
 * Biểu thị hoạt động của xe trên dốc. Điều này có nghĩa là một chiếc xe có thể
 * sử dụng nhiều hơn hoặc ít hơn sức mạnh trên dốc dương hoặc dốc âm. Trong
 * trường hợp này, điều này có nghĩa là xe sử dụng nhiều hơn 10% sức mạnh nếu độ
 * dốc lớn hơn 3%.
 */
public class SlopeEnergy {  // tiêu hao năng lượng
	/**
	 * Ngưỡng mà từ đó lượng tiêu thụ bổ sung hoặc ít hơn xảy ra.
	 */
	private double threshold;
	/**
	 * Số lượng tiêu thụ bổ sung hoặc ít hơn.
	 */
	private double value;

	public SlopeEnergy(double threshold, double value) {
		this.threshold = threshold;
		this.value = value;
	}

	public double getThreshold() {
		return threshold;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "SlopeEnergy{" + "threshold=" + threshold + ", value=" + value + '}'; //độ tiêu hao
	}
}
