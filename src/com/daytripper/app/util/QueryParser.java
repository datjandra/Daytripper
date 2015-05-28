package com.daytripper.app.util;

import java.util.List;
import java.util.Locale;

import com.daytripper.app.vocifery.model.Result;
import com.daytripper.app.vocifery.model.Searchable;

public final class QueryParser {

	private static enum ParseState {
		BEGIN,
		CONTINUE,
		SEEK_INDEX,
		END
	}
	
	private QueryParser() {}
	
	public final static String extractDestinationFromQuery(String query, List<Searchable> searchableList) {
		if (searchableList == null || searchableList.isEmpty()) {
			return null;
		}
		
		Integer itemIndex = null;
		ParseState parseState = ParseState.BEGIN;
		String[] terms = query.split("\\s+");
		for (String term : terms) {
			switch (parseState) {
				case BEGIN:
					if (term.equalsIgnoreCase("pick")) {
						parseState = ParseState.CONTINUE;
					}
					break;
					
				case CONTINUE:
					if (term.equalsIgnoreCase("up")) {
						parseState = ParseState.SEEK_INDEX;
					}
					break;
					
				case SEEK_INDEX:
					try {
						itemIndex = Integer.parseInt(term);
						parseState = ParseState.END;
					} catch (Exception e) {}
					break;
					
				default:
					break;
			}
		}
		
		String destination = null;
		if (parseState == ParseState.END &&
				itemIndex != null && 
				itemIndex < searchableList.size()-1) {
			Searchable item = searchableList.get(itemIndex-1);
			if (item != null) {
				destination = String.format(Locale.getDefault(),
						"%10.6f, %10.6f", item.getLatitude(), item.getLongitude());
			}
		}
		return destination;
	}
	
	public final static Integer extractZoomFromQuery(String query) {
		Integer zoom = null;
		ParseState parseState = ParseState.BEGIN;
		String[] terms = query.split("\\s+");
		for (String term : terms) {
			switch (parseState) {
				case BEGIN:
					if (term.equalsIgnoreCase("zoom")) {
						parseState = ParseState.CONTINUE;
					}
					break;
					
				case CONTINUE:
					if (term.equalsIgnoreCase("to")) {
						parseState = ParseState.SEEK_INDEX;
					}
					break;
					
				case SEEK_INDEX:
					try {
						zoom = Integer.parseInt(term);
						parseState = ParseState.END;
					} catch (Exception e) {}
					break;
					
				default:
					break;
			}
		}
		return zoom;
	}
}
