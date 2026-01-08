package com.coaxial.tspweb.model;

import com.google.maps.model.DistanceMatrixElement;

import java.util.Comparator;

/**
 * Lớp đặc biệt này tạo ra một mảng chỉ mục, được sắp xếp bên ngoài bằng cách sử
 * dụng bộ so sánh này. Bằng cách này, bạn có thể lấy mảng chỉ mục theo thứ tự
 * kích thước của mảng ban đầu để truy xuất các phần tử của mỗi dòng của ma trận
 * theo đúng thứ tự mà không cần sắp xếp thứ tự.
 */
public class ElementIndexComparator implements Comparator<Integer> {
	private final DistanceMatrixElement[] array;

	public ElementIndexComparator(DistanceMatrixElement[] array) {
		this.array = array;
	}

	public Integer[] createIndexArray() {
		Integer[] indexes = new Integer[array.length];
		for (int i = 0; i < array.length; i++) {
			indexes[i] = i; // Autoboxing
		}
		return indexes;
	}

	@Override
	public int compare(Integer index1, Integer index2) {
		return (int) (array[index1].distance.inMeters - array[index2].distance.inMeters);
	}
}
