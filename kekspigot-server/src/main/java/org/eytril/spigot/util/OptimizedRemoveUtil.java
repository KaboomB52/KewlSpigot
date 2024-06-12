package org.eytril.spigot.util;

import java.util.List;
import java.util.Set;

public class OptimizedRemoveUtil {

	public interface Marker {

		boolean isNeedRemoval();

		void markRemoval();

	}

	/**
	 * An optimized remove all
	 * From TacoSpigot https://github.com/TacoSpigot/TacoSpigot
	 *
	 * @param list     the list to remove from
	 * @param toRemove what to remove
	 * @param position the position to remove from
	 * @param <T>      the type being removed
	 * @return the new position
	 */
	public static <T extends Marker> int removeAll(List<T> list, Set<T> toRemove, int position) {
		for (Marker marker : toRemove) {
			marker.markRemoval();
		}

		int size = list.size();
		int insertAt = 0;

		for (int i = 0; i < size; i++) {
			T element = list.get(i);
			if (i == position) position = insertAt;
			if (element != null && !element.isNeedRemoval()) {
				list.set(insertAt++, element);
			}
		}

		list.subList(insertAt, size).clear();

		return position;
	}

}
