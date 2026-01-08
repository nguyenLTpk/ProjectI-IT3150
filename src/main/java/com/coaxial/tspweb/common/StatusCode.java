package com.coaxial.tspweb.common;

import com.coaxial.tspweb.io.GsonWrapper;

/**
 * Đối với thông báo trạng thái, enum này đại diện cho các mã trạng thái có thể.
 */
public enum StatusCode {
	/**
	 * Đại diện cho trạng thái khi máy chủ sẵn sàng thực hiện các phép tính và kết
	 * nối được thiết lập.
	 */
	READY("READY"),
	/**
	 * Biểu diễn trạng thái trong quá trình tính toán.
	 */
	CALCULATING("CALC"),
	/**
	 * Cho biết lỗi phía máy chủ.
	 */
	ERROR("ERROR"),
	/**
	 * Được gửi ngay sau khi tính toán được thực hiện và khách hàng sẽ mong đợi kết
	 * quả.
	 */
	DONE("DONE");

	private final String code;

	StatusCode(final String code) {
		this.code = code;
	}

	@Override
	public String toString() {
		return code;
	}

	public GsonWrapper toGsonWrapper(GsonWrapper wrapper) {
		return (wrapper == null ? new GsonWrapper() : wrapper).add("status", code);
	}

	public GsonWrapper toGsonWrapper() {
		return toGsonWrapper(null);
	}
}
