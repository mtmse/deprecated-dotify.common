package org.daisy.dotify.common.split;

import java.util.List;

public class SplitResult<T extends SplitPointUnit> {
	private final List<T> head;
	private final SplitPointDataSource<T> tail;
	
	public SplitResult(List<T> head, SplitPointDataSource<T> tail) {
		this.head = head;
		this.tail = tail;
	}
	
	public List<T> head() {
		return head;
	}

	public SplitPointDataSource<T> tail() {
		return tail;
	}

}
