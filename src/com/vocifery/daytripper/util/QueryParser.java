package com.vocifery.daytripper.util;

import java.util.List;
import java.util.Locale;

import com.vocifery.daytripper.model.Searchable;

public final class QueryParser {

	private final static String ZOOM_FIRST_CUE = "zoom";
	private final static String ZOOM_SECOND_CUE = "level";
	private final static String NAME_FIRST_CUE = "is";
	
	private static enum ParseState {
		BEGIN,
		CONTINUE,
		SEEK_FOCUS,
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
						parseState = ParseState.SEEK_FOCUS;
					}
					break;
					
				case SEEK_FOCUS:
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
					if (term.equalsIgnoreCase(ZOOM_FIRST_CUE)) {
						parseState = ParseState.CONTINUE;
					}
					break;
					
				case CONTINUE:
					if (term.equalsIgnoreCase(ZOOM_SECOND_CUE)) {
						parseState = ParseState.SEEK_FOCUS;
					}
					break;
					
				case SEEK_FOCUS:
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
	
	public final static String extractNameFromQuery(String query) {
		StringBuilder nameBuilder = new StringBuilder();
		ParseState parseState = ParseState.BEGIN;
		String[] terms = query.split("\\s+");
		for (String term : terms) {
			switch (parseState) {
				case BEGIN:
					if (term.equalsIgnoreCase(NAME_FIRST_CUE)) {
						parseState = ParseState.SEEK_FOCUS;
					}
					break;
					
				case SEEK_FOCUS:
					nameBuilder.append(term);
					nameBuilder.append(" ");
					break;
					
				default:
					break;
			}
		}
		return nameBuilder.toString().trim();
	}
}
