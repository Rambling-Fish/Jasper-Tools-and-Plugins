package com.coralcea.jasper.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MultiValueMap<S, T> {
	private HashMap<S, List<T>> map = new HashMap<S, List<T>>();

	public List<T> get(S key) {
		return map.get(key);
	}

	public void put(S key, T value) {
		List<T> values = map.get(key);
		if (values == null) 
			map.put(key, values = new ArrayList<T>());
		if (!values.contains(value))
			values.add(value);
		return;
	}

	public int remove(S key, T value) {
		List<T> values = map.get(key);
		if (values != null) {
			int result = values.indexOf(value);
			if (result == -1)
				return -1;
			values.remove(result);
			if (values.isEmpty())
				map.remove(key);
			return result;
		}
		return -1;
	}

	public Object removeValue(T value) {
		Iterator<List<T>> iter = map.values().iterator();
		List<T> current;
		while (iter.hasNext()) {
			current = iter.next();
			if (current.remove(value)) {
				if (current.isEmpty())
					iter.remove();
				return value;
			}
		}
		return null;
	}

	public int size() {
		return map.size();
	}
}
